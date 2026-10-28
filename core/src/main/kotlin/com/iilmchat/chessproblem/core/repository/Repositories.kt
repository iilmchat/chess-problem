package com.iilmchat.chessproblem.core.repository

import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface PuzzleRepository {
    suspend fun all(): List<Puzzle>

    suspend fun byId(id: String): Puzzle?

    suspend fun byLevel(level: com.iilmchat.chessproblem.core.model.Level): List<Puzzle>
}

interface ProgressRepository {
    fun observeAll(): Flow<List<PuzzleProgress>>

    suspend fun forPuzzle(puzzleId: String): PuzzleProgress?

    suspend fun save(progress: PuzzleProgress)

    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun update(transform: (UserSettings) -> UserSettings)
}

interface HintRepository {
    val balance: Flow<Int>

    suspend fun currentBalance(): Int

    suspend fun spend(amount: Int): Boolean

    suspend fun refillIfNeeded()
}

interface LocaleStore {
    fun language(): AppLanguage

    fun setLanguage(language: AppLanguage)
}
