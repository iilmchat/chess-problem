package com.iilmchat.chessproblem.core.repository

import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.ChapterProgress
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.model.ThemeMode
import com.iilmchat.chessproblem.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface PuzzleRepository {
    suspend fun all(): List<Puzzle>

    suspend fun byId(id: String): Puzzle?

    suspend fun byLevel(level: com.iilmchat.chessproblem.core.model.Level): List<Puzzle>
}

interface ProgressRepository {
    fun observeAll(): Flow<List<PuzzleProgress>>

    fun observeChapters(): Flow<List<ChapterProgress>>

    suspend fun forPuzzle(puzzleId: String): PuzzleProgress?

    suspend fun save(progress: PuzzleProgress)

    suspend fun clear()
}

/**
 * Актуальная версия бандла задач. Хранится в заголовке JSON-паков, поэтому
 * прогресс с другой версией считается несовместимым (KI-018).
 */
interface ContentVersionProvider {
    fun current(): String
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    /** Значения, доступные синхронно до первой композиции (KI-015). */
    fun startupTheme(): ThemeMode

    fun startupLanguage(): AppLanguage

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

interface ThemeStore {
    fun theme(): ThemeMode

    fun setTheme(theme: ThemeMode)
}
