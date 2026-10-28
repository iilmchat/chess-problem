package com.iilmchat.chessproblem.data.local

import android.content.Context
import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.repository.LocaleStore

class LocalePreferences(
    context: Context,
) : LocaleStore {
    private val prefs = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun language(): AppLanguage =
        prefs.getString(KEY_LANGUAGE, null)
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.SYSTEM

    override fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "locale"
        private const val KEY_LANGUAGE = "language"
    }
}
