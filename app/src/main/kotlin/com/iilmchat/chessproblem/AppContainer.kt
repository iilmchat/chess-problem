package com.iilmchat.chessproblem

import android.content.Context
import com.iilmchat.chessproblem.core.chess.ChessRules
import com.iilmchat.chessproblem.core.chess.ChesslibRules
import com.iilmchat.chessproblem.core.repository.HintRepository
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.core.repository.SettingsRepository
import com.iilmchat.chessproblem.data.local.HintWallet
import com.iilmchat.chessproblem.data.local.LocalePreferences
import com.iilmchat.chessproblem.data.local.SettingsDataStore
import com.iilmchat.chessproblem.data.repository.createProgressRepository
import com.iilmchat.chessproblem.data.repository.createPuzzleRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val localePreferences = LocalePreferences(appContext)

    val puzzleRepository: PuzzleRepository = createPuzzleRepository(appContext)

    val progressRepository: ProgressRepository = createProgressRepository(appContext)

    val settingsRepository: SettingsRepository =
        SettingsDataStore(appContext, localePreferences)

    val hintRepository: HintRepository = HintWallet(appContext)

    val rules: ChessRules = ChesslibRules()
}
