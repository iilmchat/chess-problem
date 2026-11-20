package com.iilmchat.chessproblem.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.iilmchat.chessproblem.core.chess.ChesslibRules
import com.iilmchat.chessproblem.core.chess.Square
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
import com.iilmchat.chessproblem.core.session.PuzzleEvent
import com.iilmchat.chessproblem.core.session.PuzzleSession
import com.iilmchat.chessproblem.core.session.PuzzleSessionSnapshot
import com.iilmchat.chessproblem.core.session.toSnapshot
import com.iilmchat.chessproblem.ui.testPuzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val SESSION_KEY = "active_puzzle_session"

@RunWith(RobolectricTestRunner::class)
class PuzzleViewModelRestoreTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val puzzle = testPuzzle()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `после process death восстанавливаются ходы остаток времени и overlay паузы`() = runTest {
        val snapshot = activeSnapshot()
        val viewModel = createViewModel(SavedStateHandle(mapOf(SESSION_KEY to snapshot)))

        runCurrent()

        val session = viewModel.state.value.session
        assertNotNull(session)
        assertEquals(snapshot.moves, session.moves)
        assertEquals(snapshot.solutionIndex, session.solutionIndex)
        assertEquals(snapshot.remainingMs, session.remainingMs)
        assertEquals(snapshot.hintsUsed, session.hintsUsed)
        assertEquals(snapshot.attemptsLeft, session.attemptsLeft)
        assertTrue(session.paused)
        assertTrue(viewModel.state.value.showPause)
    }

    @Test
    fun `восстановленный таймер не тикает до снятия с паузы`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf(SESSION_KEY to activeSnapshot())))
        runCurrent()
        val before = viewModel.state.value.session?.elapsedMs

        advanceTimeBy(1_000L)
        assertEquals(before, viewModel.state.value.session?.elapsedMs)

        viewModel.resume()
        advanceTimeBy(300L)
        runCurrent()

        assertEquals(before?.plus(250L), viewModel.state.value.session?.elapsedMs)
        assertFalse(viewModel.state.value.showPause)
    }

    @Test
    fun `завершённая сессия не восстанавливается после process death`() = runTest {
        val session = PuzzleSession(puzzle, ChesslibRules())
        var state = session.initialState()
        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("e2"),
                to = Square.fromAlgebraic("e4"),
            ),
        )
        state = session.onEvent(state, PuzzleEvent.OpponentReply)
        assertTrue(state.finished)
        val viewModel = createViewModel(SavedStateHandle(mapOf(SESSION_KEY to state.toSnapshot())))

        runCurrent()

        assertEquals(0, viewModel.state.value.session?.solutionIndex)
        assertFalse(viewModel.state.value.showPause)
    }

    private fun activeSnapshot(): PuzzleSessionSnapshot {
        val session = PuzzleSession(puzzle, ChesslibRules())
        var state = session.initialState()
        // Неверный ход оставляет сессию активной: правильный e2e4 вместе с
        // ответом соперника сразу решает демо-задачу и делает снимок finished.
        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("e2"),
                to = Square.fromAlgebraic("e3"),
            ),
        )
        return session.onEvent(state, PuzzleEvent.Tick(20_000L)).toSnapshot()
    }

    private fun createViewModel(handle: SavedStateHandle) = PuzzleViewModel(
        puzzleId = puzzle.id,
        savedStateHandle = handle,
        puzzleRepository = FakePuzzleRepository(puzzle),
        progressRepository = FakeProgressRepository(),
        hintRepository = FakeHintRepository(),
        settingsRepository = FakeSettingsRepository(),
        rules = ChesslibRules(),
    )

    private class FakePuzzleRepository(
        private val puzzle: Puzzle,
    ) : PuzzleRepository {
        override suspend fun all(): List<Puzzle> = listOf(puzzle)

        override suspend fun byId(id: String): Puzzle? = puzzle.takeIf { it.id == id }

        override suspend fun byLevel(level: Level): List<Puzzle> =
            listOf(puzzle).filter { it.level == level }
    }

    private class FakeProgressRepository : ProgressRepository {
        private val saved = MutableStateFlow<List<PuzzleProgress>>(emptyList())

        override fun observeAll(): Flow<List<PuzzleProgress>> = saved.asStateFlow()

        override fun observeChapters(): Flow<List<ChapterProgress>> =
            MutableStateFlow(emptyList<ChapterProgress>()).asStateFlow()

        override suspend fun forPuzzle(puzzleId: String): PuzzleProgress? =
            saved.value.firstOrNull { it.puzzleId == puzzleId }

        override suspend fun save(progress: PuzzleProgress) {
            saved.update { items -> items.filterNot { it.puzzleId == progress.puzzleId } + progress }
        }

        override suspend fun clear() {
            saved.value = emptyList()
        }
    }

    private class FakeHintRepository : HintRepository {
        private val current = MutableStateFlow(3)

        override val balance: Flow<Int> = current.asStateFlow()

        override suspend fun currentBalance(): Int = current.value

        override suspend fun spend(amount: Int): Boolean {
            current.update { it - amount }
            return true
        }

        override suspend fun refillIfNeeded() = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings: Flow<UserSettings> =
            MutableStateFlow(UserSettings()).asStateFlow()

        override fun startupTheme(): ThemeMode = ThemeMode.SYSTEM

        override fun startupLanguage(): AppLanguage = AppLanguage.SYSTEM

        override suspend fun update(transform: (UserSettings) -> UserSettings) = Unit
    }
}
