package com.iilmchat.chessproblem.data.local

import android.content.Context
import com.iilmchat.chessproblem.core.model.ThemeMode
import com.iilmchat.chessproblem.core.repository.ThemeStore

/**
 * KI-015: синхронный кэш темы для первого кадра. Значение дублируется
 * SettingsDataStore при каждой записи, поэтому чтение здесь не блокирует UI.
 */
class ThemePreferences(
    context: Context,
) : ThemeStore {
    private val prefs = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun theme(): ThemeMode =
        prefs.getString(KEY_THEME, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    override fun setTheme(theme: ThemeMode) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "theme"
        private const val KEY_THEME = "theme"
    }
}
