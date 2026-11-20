package com.iilmchat.chessproblem.ui.screenshot

import androidx.lifecycle.SavedStateHandle
import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.ChapterProgress
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.model.ThemeMode
import com.iilmchat.chessproblem.core.model.UserSettings
import com.iilmchat.chessproblem.core.repository.HintRepository
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.core.repository.SettingsRepository
import com.iilmchat.chessproblem.ui.testPuzzle
import com.iilmchat.chessproblem.ui.viewmodel.PuzzleViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private class ScreenshotPuzzleRepository(
    private val puzzle: Puzzle,
) : PuzzleRepository {
    override suspend fun all(): List<Puzzle> = listOf(puzzle)

    override suspend fun byId(id: String): Puzzle? = puzzle.takeIf { it.id == id }

    override suspend fun byLevel(level: Level): List<Puzzle> =
        listOf(puzzle).filter { it.level == level }
}

private class ScreenshotProgressRepository : ProgressRepository {
    override fun observeAll(): Flow<List<PuzzleProgress>> = MutableStateFlow(emptyList())

    override fun observeChapters(): Flow<List<ChapterProgress>> = MutableStateFlow(emptyList())

    override suspend fun forPuzzle(puzzleId: String): PuzzleProgress? = null

    override suspend fun save(progress: PuzzleProgress) = Unit

    override suspend fun clear() = Unit
}

private class ScreenshotHintRepository : HintRepository {
    override val balance: Flow<Int> = MutableStateFlow(3)

    override suspend fun currentBalance(): Int = 3

    override suspend fun spend(amount: Int): Boolean = true

    override suspend fun refillIfNeeded() = Unit
}

private class ScreenshotSettingsRepository : SettingsRepository {
    override val settings: Flow<UserSettings> = MutableStateFlow(UserSettings())

    override fun startupTheme(): ThemeMode = ThemeMode.DARK

    override fun startupLanguage(): AppLanguage = AppLanguage.RU

    override suspend fun update(transform: (UserSettings) -> UserSettings) = Unit
}

internal fun screenshotPuzzleViewModel(): PuzzleViewModel {
    val puzzle = testPuzzle()
    return PuzzleViewModel(
        puzzleId = puzzle.id,
        savedStateHandle = SavedStateHandle(),
        puzzleRepository = ScreenshotPuzzleRepository(puzzle),
        progressRepository = ScreenshotProgressRepository(),
        hintRepository = ScreenshotHintRepository(),
        settingsRepository = ScreenshotSettingsRepository(),
    )
}
