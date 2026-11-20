package com.iilmchat.chessproblem.core.session

import com.iilmchat.chessproblem.core.chess.ChesslibRules
import com.iilmchat.chessproblem.core.chess.Square
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.LocalizedText
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleTask
import com.iilmchat.chessproblem.core.model.TaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SessionRestoreTest {
    private val rules = ChesslibRules()
    private val puzzle = knightFork()
    private val session = PuzzleSession(puzzle, rules)

    @Test
    fun `восстановление после process death возвращает доску ходы и индекс решения`() {
        val played = activeState()
        val snapshot = played.toSnapshot()

        val restored = PuzzleSession(puzzle, rules).restore(snapshot)

        assertEquals(puzzle.id, restored.puzzle.id)
        assertEquals(played.board, restored.board)
        assertEquals(played.moves, restored.moves)
        assertEquals(played.solutionIndex, restored.solutionIndex)
        assertEquals(PuzzleState.Status.SOLVING, restored.status)
    }

    @Test
    fun `восстановленная сессия ставится на паузу и сохраняет остаток таймера`() {
        val played = activeState()
        val snapshot = played.toSnapshot()

        val restored = PuzzleSession(puzzle, rules).restore(snapshot)

        assertTrue(restored.paused)
        assertEquals(played.remainingMs, restored.remainingMs)
        assertEquals(played.timeLimitMs, restored.timeLimitMs)
        assertEquals(played.elapsedMs, restored.elapsedMs)
        assertEquals(played.attemptsLeft, restored.attemptsLeft)
        assertEquals(played.mistakes, restored.mistakes)
        assertEquals(played.hintsUsed, restored.hintsUsed)
        assertEquals(played.flipped, restored.flipped)
        assertEquals(played.continued, restored.continued)
    }

    @Test
    fun `снимок другой задачи нельзя восстановить в текущей сессии`() {
        val snapshot = activeState().toSnapshot().copy(puzzleId = "nov-999")

        assertFailsWith<IllegalArgumentException> {
            session.restore(snapshot)
        }
    }

    private fun activeState(): PuzzleState {
        var state = session.initialState()
        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("f5"),
                to = Square.fromAlgebraic("e7"),
            ),
        )
        state = session.onEvent(state, PuzzleEvent.OpponentReply)
        state = session.onEvent(state, PuzzleEvent.Tick(30_000L))
        state = session.onEvent(state, PuzzleEvent.UseHint(HintTier.IDEA))
        return session.onEvent(state, PuzzleEvent.FlipBoard)
    }

    private fun knightFork() = Puzzle(
        id = "pro-001",
        level = Level.PROFESSIONAL,
        chapter = "mixed-review",
        fen = "2q3k1/5ppp/8/5N2/8/8/5PPP/6K1 w - - 0 1",
        setupMove = null,
        solution = listOf("f5e7", "g8h8", "e7c8"),
        task = PuzzleTask(TaskType.WIN, 2),
        rating = 1450,
        themes = listOf("fork"),
        timeLimitSec = 185,
        title = LocalizedText("Вилка конём", "Knight fork"),
        prompt = LocalizedText("Выиграйте ферзя.", "Win the queen."),
        hints = emptyList(),
        explanation = LocalizedText("Вилка.", "Fork."),
    )
}
