package com.iilmchat.chessproblem.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.BoardTheme
import com.iilmchat.chessproblem.core.model.ThemeMode
import com.iilmchat.chessproblem.core.model.UserSettings
import com.iilmchat.chessproblem.core.repository.LocaleStore
import com.iilmchat.chessproblem.core.repository.SettingsRepository
import com.iilmchat.chessproblem.core.repository.ThemeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(
    private val context: Context,
    private val localeStore: LocaleStore,
    private val themeStore: ThemeStore,
) : SettingsRepository {
    private val themeSeeded = AtomicBoolean(false)

    override val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            // Обновление с v0.4.0: тема была только в DataStore — перенесём её в кэш один раз.
            if (themeSeeded.compareAndSet(false, true)) {
                prefs[KEY_THEME]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?.let(themeStore::setTheme)
            }
            prefs.toSettings()
        }

    override fun startupTheme(): ThemeMode = themeStore.theme()

    override fun startupLanguage(): AppLanguage = localeStore.language()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        var languageChanged: AppLanguage? = null
        var themeChanged: ThemeMode? = null
        context.settingsDataStore.edit { prefs ->
            val current = prefs.toSettings()
            val next = transform(current)
            prefs.write(next)
            if (next.language != current.language) {
                languageChanged = next.language
            }
            if (next.theme != current.theme) {
                themeChanged = next.theme
            }
        }
        // KI-015: кэш первого кадра обновляется синхронно следом за DataStore.
        languageChanged?.let(localeStore::setLanguage)
        themeChanged?.let(themeStore::setTheme)
    }

    private fun Preferences.toSettings() = UserSettings(
        language = localeStore.language(),
        theme = themeStore.theme(),
        boardTheme = readEnum(KEY_BOARD_THEME, BoardTheme.SAGE),
        coordinates = this[KEY_COORDINATES] ?: true,
        autoFlip = this[KEY_AUTO_FLIP] ?: false,
        reduceMotion = this[KEY_REDUCE_MOTION] ?: false,
        sound = this[KEY_SOUND] ?: true,
        haptics = this[KEY_HAPTICS] ?: true,
    )

    private inline fun <reified T : Enum<T>> Preferences.readEnum(
        key: Preferences.Key<String>,
        fallback: T,
    ): T = this[key]?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private fun androidx.datastore.preferences.core.MutablePreferences.write(settings: UserSettings) {
        this[KEY_LANGUAGE] = settings.language.name
        this[KEY_THEME] = settings.theme.name
        this[KEY_BOARD_THEME] = settings.boardTheme.name
        this[KEY_COORDINATES] = settings.coordinates
        this[KEY_AUTO_FLIP] = settings.autoFlip
        this[KEY_REDUCE_MOTION] = settings.reduceMotion
        this[KEY_SOUND] = settings.sound
        this[KEY_HAPTICS] = settings.haptics
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_BOARD_THEME = stringPreferencesKey("board_theme")
        val KEY_COORDINATES = booleanPreferencesKey("coordinates")
        val KEY_AUTO_FLIP = booleanPreferencesKey("auto_flip")
        val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val KEY_SOUND = booleanPreferencesKey("sound")
        val KEY_HAPTICS = booleanPreferencesKey("haptics")
    }
}
