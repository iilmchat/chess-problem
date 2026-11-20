package com.iilmchat.chessproblem

import android.content.Context
import com.iilmchat.chessproblem.core.chess.ChessRules
import com.iilmchat.chessproblem.core.chess.ChesslibRules
import com.iilmchat.chessproblem.core.repository.ContentVersionProvider
import com.iilmchat.chessproblem.core.repository.HintRepository
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.core.repository.SettingsRepository
import com.iilmchat.chessproblem.data.local.HintWallet
import com.iilmchat.chessproblem.data.local.LocalePreferences
import com.iilmchat.chessproblem.data.local.SettingsDataStore
import com.iilmchat.chessproblem.data.local.ThemePreferences
import com.iilmchat.chessproblem.data.repository.createContentVersionProvider
import com.iilmchat.chessproblem.data.repository.createProgressRepository
import com.iilmchat.chessproblem.data.repository.createPuzzleRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val localePreferences = LocalePreferences(appContext)

    // KI-015: синхронный кэш настроек первого кадра, по образцу локали.
    val themePreferences = ThemePreferences(appContext)

    val puzzleRepository: PuzzleRepository = createPuzzleRepository(appContext)

    val contentVersionProvider: ContentVersionProvider = createContentVersionProvider(appContext)

    val progressRepository: ProgressRepository =
        createProgressRepository(appContext, contentVersionProvider)

    val settingsRepository: SettingsRepository =
        SettingsDataStore(appContext, localePreferences, themePreferences)

    val hintRepository: HintRepository = HintWallet(appContext)

    val rules: ChessRules = ChesslibRules()
}
