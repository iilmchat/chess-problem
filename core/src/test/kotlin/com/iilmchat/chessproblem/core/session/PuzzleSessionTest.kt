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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PuzzleSessionTest {
    private val rules = ChesslibRules()

    @Test
    fun `solves back rank mate and awards three stars`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = session.initialState()

        state = session.onEvent(state, PuzzleEvent.TapSquare(Square.fromAlgebraic("e1")))
        assertTrue(Square.fromAlgebraic("e8") in state.legalTargets)

        state = session.onEvent(state, PuzzleEvent.TapSquare(Square.fromAlgebraic("e8")))

        assertEquals(PuzzleState.Status.SOLVED, state.status)
        assertEquals(1, state.userMoves)
        assertEquals("Re8#", state.moves.single().san)
        assertEquals(3, state.stars)
        assertEquals(200, state.xp)
        assertTrue(state.board.checkmate)
    }

    @Test
    fun `draw by stalemate after the forced capture`() {
        val session = PuzzleSession(stalemateDraw(), rules)
        var state = session.initialState()

        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("c2"),
                to = Square.fromAlgebraic("b3"),
            ),
        )

        assertEquals(PuzzleState.Status.OPPONENT, state.status)
        assertEquals("Bb3+", state.moves.last().san)

        state = session.onEvent(state, PuzzleEvent.OpponentReply)

        assertEquals(PuzzleState.Status.SOLVED, state.status)
        assertEquals("Qxb3", state.moves.last().san)
        assertTrue(state.board.stalemate)
        assertEquals(1, state.userMoves)
    }

    @Test
    fun `wrong move costs an attempt and keeps the position`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = session.initialState()

        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("e1"),
                to = Square.fromAlgebraic("e2"),
            ),
        )

        assertEquals(PuzzleState.Status.SOLVING, state.status)
        assertEquals(2, state.attemptsLeft)
        assertEquals(1, state.mistakes)
        assertEquals(Feedback.Wrong, state.feedback)
        assertNull(state.board.pieceAt(Square.fromAlgebraic("e2")))
        assertEquals(
            com.iilmchat.chessproblem.core.chess.Piece(
                com.iilmchat.chessproblem.core.chess.PieceType.ROOK,
                com.iilmchat.chessproblem.core.chess.Side.WHITE,
            ),
            state.board.pieceAt(Square.fromAlgebraic("e1")),
        )
    }

    @Test
    fun `three wrong moves fail the puzzle`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = session.initialState()

        listOf("e2", "e3", "e4").forEach { target ->
            state = session.onEvent(
                state,
                PuzzleEvent.MakeMove(
                    from = Square.fromAlgebraic("e1"),
                    to = Square.fromAlgebraic(target),
                ),
            )
        }

        assertEquals(PuzzleState.Status.FAILED, state.status)
        assertEquals(0, state.attemptsLeft)
        assertEquals(3, state.mistakes)
        assertEquals(0, state.stars)
    }

    @Test
    fun `continue adds time and one attempt once`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = failedState(session)

        state = session.onEvent(state, PuzzleEvent.ContinueWithTime)

        assertEquals(PuzzleState.Status.SOLVING, state.status)
        assertEquals(1, state.attemptsLeft)
        assertEquals(175_000L, state.timeLimitMs)
        assertTrue(state.continued)
        assertEquals(1, state.starsCap)

        val afterSecondContinue = session.onEvent(state, PuzzleEvent.ContinueWithTime)
        assertEquals(state, afterSecondContinue)
    }

    @Test
    fun `timeout fails the puzzle`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = session.initialState()

        state = session.onEvent(state, PuzzleEvent.Tick(115_000L))

        assertEquals(PuzzleState.Status.FAILED, state.status)
        assertEquals(115_000L, state.elapsedMs)
        assertEquals(Feedback.Timeout, state.feedback)
        assertEquals(0, state.stars)
    }

    @Test
    fun `idea hint is free but only available once`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = session.initialState()

        state = session.onEvent(state, PuzzleEvent.Pause)
        state = session.onEvent(state, PuzzleEvent.UseHint(HintTier.IDEA))

        assertTrue(HintTier.IDEA in state.hintsUsed)
        assertEquals(HintTier.IDEA, state.activeHint)
        assertFalse(state.paused)
        assertFalse(state.ideaAvailable)

        val second = session.onEvent(state, PuzzleEvent.UseHint(HintTier.IDEA))
        assertEquals(1, second.hintsUsed.size)
    }

    @Test
    fun `opponent reply is applied and the line can finish`() {
        val session = PuzzleSession(knightFork(), rules)
        var state = session.initialState()

        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("f5"),
                to = Square.fromAlgebraic("e7"),
            ),
        )
        assertEquals(PuzzleState.Status.OPPONENT, state.status)
        assertEquals(1, state.solutionIndex)

        state = session.onEvent(state, PuzzleEvent.OpponentReply)
        assertEquals(PuzzleState.Status.SOLVING, state.status)
        assertEquals(2, state.solutionIndex)
        assertEquals("Kh8", state.moves.last().san)

        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("e7"),
                to = Square.fromAlgebraic("c8"),
            ),
        )
        assertEquals(PuzzleState.Status.SOLVED, state.status)
        assertEquals("Nxc8", state.moves.last().san)
        assertEquals(3, state.stars)
    }

    @Test
    fun `restart resets the position and caps stars at one`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = failedState(session)

        state = session.onEvent(state, PuzzleEvent.Restart)

        assertEquals(PuzzleState.Status.SOLVING, state.status)
        assertEquals(3, state.attemptsLeft)
        assertEquals(0L, state.elapsedMs)
        assertEquals(1, state.starsCap)
        assertTrue(state.continued)
        assertEquals(
            com.iilmchat.chessproblem.core.chess.Piece(
                com.iilmchat.chessproblem.core.chess.PieceType.ROOK,
                com.iilmchat.chessproblem.core.chess.Side.WHITE,
            ),
            state.board.pieceAt(Square.fromAlgebraic("e1")),
        )
    }

    @Test
    fun `reveal plays the line and marks the puzzle reviewed`() {
        val session = PuzzleSession(backRankMate(), rules)
        var state = failedState(session)

        state = session.onEvent(state, PuzzleEvent.Reveal)

        assertEquals(PuzzleState.Status.REVIEW, state.status)
        assertEquals(0, state.stars)
        assertEquals(10, state.xp)
        assertTrue(state.revealUsed)
        assertTrue(state.board.checkmate)
        assertEquals(1, state.solutionIndex)
    }

    @Test
    fun `promotion creates choices instead of moving immediately`() {
        val session = PuzzleSession(promotionPuzzle(), rules)
        var state = session.initialState()

        state = session.onEvent(state, PuzzleEvent.TapSquare(Square.fromAlgebraic("a7")))
        state = session.onEvent(state, PuzzleEvent.TapSquare(Square.fromAlgebraic("a8")))

        assertEquals(4, state.promotionChoices.size)
        assertEquals(PuzzleState.Status.SOLVING, state.status)

        state = session.onEvent(
            state,
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("a7"),
                to = Square.fromAlgebraic("a8"),
                promotion = com.iilmchat.chessproblem.core.chess.PieceType.QUEEN,
            ),
        )
        assertEquals(PuzzleState.Status.SOLVED, state.status)
    }

    @Test
    fun `drag move to promotion square offers choices`() {
        val session = PuzzleSession(promotionPuzzle(), rules)
        val state = session.onEvent(
            session.initialState(),
            PuzzleEvent.MakeMove(
                from = Square.fromAlgebraic("a7"),
                to = Square.fromAlgebraic("a8"),
            ),
        )

        assertEquals(PuzzleState.Status.SOLVING, state.status)
        assertEquals(4, state.promotionChoices.size)
        assertTrue(state.promotionChoices.all { it.promotion != null })
    }

    private fun failedState(session: PuzzleSession): PuzzleState {
        var state = session.initialState()
        listOf("e2", "e3", "e4").forEach { target ->
            state = session.onEvent(
                state,
                PuzzleEvent.MakeMove(
                    from = Square.fromAlgebraic("e1"),
                    to = Square.fromAlgebraic(target),
                ),
            )
        }
        return state
    }

    private fun backRankMate() = Puzzle(
        id = "nov-001",
        level = Level.NOVICE,
        chapter = "mate-in-1",
        fen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
        setupMove = null,
        solution = listOf("e1e8"),
        task = PuzzleTask(TaskType.MATE, 1),
        rating = 800,
        themes = listOf("mateIn1", "backRankMate"),
        timeLimitSec = 115,
        title = LocalizedText("Мат в один ход", "Mate in one"),
        prompt = LocalizedText("Ход белых. Найдите мат в один ход.", "White to move. Find mate in one."),
        hints = emptyList(),
        explanation = LocalizedText("Мат ладьёй.", "Rook mate."),
    )

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

    private fun stalemateDraw() = Puzzle(
        id = "grm-901",
        level = Level.GRANDMASTER,
        chapter = "stalemate",
        fen = "5bkr/6pp/8/8/8/6q1/2B5/K7 w - - 0 1",
        setupMove = null,
        solution = listOf("c2b3", "g3b3"),
        task = PuzzleTask(TaskType.DRAW, 1),
        rating = 2400,
        themes = listOf("stalemate"),
        timeLimitSec = 260,
        title = LocalizedText("Пат в углу", "Stalemate in the corner"),
        prompt = LocalizedText("Ход белых. Добейтесь ничьей.", "White to move. Draw the game."),
        hints = emptyList(),
        explanation = LocalizedText("Bb3+ Qxb3 — пат.", "Bb3+ Qxb3 — stalemate."),
    )

    private fun promotionPuzzle() = Puzzle(
        id = "nov-002",
        level = Level.NOVICE,
        chapter = "promotion",
        fen = "6k1/P7/8/8/8/8/8/6K1 w - - 0 1",
        setupMove = null,
        solution = listOf("a7a8q"),
        task = PuzzleTask(TaskType.WIN, 1),
        rating = 850,
        themes = listOf("promotion"),
        timeLimitSec = 120,
        title = LocalizedText("Превращение", "Promotion"),
        prompt = LocalizedText("Проведите пешку.", "Promote the pawn."),
        hints = emptyList(),
        explanation = LocalizedText("Ферзь.", "Queen."),
    )
}
