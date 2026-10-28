package com.iilmchat.chessproblem.core.session

import com.iilmchat.chessproblem.core.chess.ChessRules
import com.iilmchat.chessproblem.core.chess.Move
import com.iilmchat.chessproblem.core.chess.PieceType
import com.iilmchat.chessproblem.core.chess.Square
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.scoring.StarPolicy
import com.iilmchat.chessproblem.core.scoring.XpPolicy
import kotlin.math.max
import kotlin.math.min

class PuzzleSession(
    private val puzzle: Puzzle,
    private val rules: ChessRules,
) {
    fun initialState(): PuzzleState {
        var board = rules.load(puzzle.fen)
        var moves = emptyList<PlayedMove>()

        puzzle.setupMove?.let { setupUci ->
            val setup = parseUci(setupUci)
            val side = board.sideToMove
            val result = rules.move(board, setup)
            if (result != null) {
                board = result.board
                moves = moves + PlayedMove(
                    uci = setup.uci,
                    san = result.san,
                    byUser = false,
                    isCorrect = true,
                    timestampMs = 0L,
                    side = side,
                )
            }
        }

        return PuzzleState(
            puzzle = puzzle,
            board = board,
            status = PuzzleState.Status.SOLVING,
            moves = moves,
            attemptsLeft = 3,
            timeLimitMs = puzzle.timeLimitMs,
        )
    }

    fun onEvent(state: PuzzleState, event: PuzzleEvent): PuzzleState = when (event) {
        is PuzzleEvent.TapSquare -> tapSquare(state, event.square)
        is PuzzleEvent.MakeMove -> makeMove(state, event)
        is PuzzleEvent.Tick -> tick(state, event.elapsedMs)
        PuzzleEvent.OpponentReply -> opponentReply(state)
        PuzzleEvent.Pause -> state.copy(paused = true)
        PuzzleEvent.Resume -> state.copy(paused = false)
        is PuzzleEvent.UseHint -> useHint(state, event.tier)
        PuzzleEvent.ContinueWithTime -> continueWithTime(state)
        PuzzleEvent.Restart -> restart(state)
        PuzzleEvent.Reveal -> reveal(state)
        PuzzleEvent.FlipBoard -> state.copy(flipped = !state.flipped)
        PuzzleEvent.ClearFeedback -> state.copy(feedback = null)
        PuzzleEvent.ClearHint -> state.copy(activeHint = null, hintPiece = null, hintMove = null)
        PuzzleEvent.ClearPromotion -> state.copy(promotionChoices = emptyList())
    }

    private fun tapSquare(state: PuzzleState, square: Square): PuzzleState {
        if (state.status != PuzzleState.Status.SOLVING || state.paused) return state
        if (state.promotionChoices.isNotEmpty()) return state

        val selected = state.selectedSquare
        if (selected != null && square in state.legalTargets) {
            val candidates = rules.legalMoves(state.board, selected)
                .filter { it.to == square }
            if (candidates.size > 1 && candidates.any { it.promotion != null }) {
                return state.copy(promotionChoices = candidates)
            }
            return candidates.firstOrNull()?.let { applyUserMove(state, it) } ?: state
        }

        val piece = state.board.pieceAt(square)
        if (piece != null && piece.side == state.board.sideToMove) {
            if (selected == square) {
                return state.copy(selectedSquare = null, legalTargets = emptySet())
            }
            val targets = rules.legalMoves(state.board, square).map { it.to }.toSet()
            return state.copy(
                selectedSquare = square,
                legalTargets = targets,
                feedback = null,
            )
        }

        return state.copy(selectedSquare = null, legalTargets = emptySet())
    }

    private fun makeMove(state: PuzzleState, event: PuzzleEvent.MakeMove): PuzzleState {
        if (state.status != PuzzleState.Status.SOLVING || state.paused) return state
        val move = rules.legalMoves(state.board, event.from)
            .firstOrNull { it.to == event.to && it.promotion == event.promotion }
            ?: return state.copy(feedback = Feedback.Illegal)
        return applyUserMove(state, move)
    }

    private fun applyUserMove(state: PuzzleState, move: Move): PuzzleState {
        val result = rules.move(state.board, move)
            ?: return state.copy(feedback = Feedback.Illegal)

        val expected = puzzle.solution.getOrNull(state.solutionIndex)
        val isMate = result.isCheckmate
        val correct = isMate || expected == move.uci

        if (!correct) {
            val mistakes = state.mistakes + 1
            val attemptsLeft = (state.attemptsLeft - 1).coerceAtLeast(0)
            val failed = attemptsLeft == 0
            return state.copy(
                status = if (failed) PuzzleState.Status.FAILED else PuzzleState.Status.SOLVING,
                attemptsLeft = attemptsLeft,
                mistakes = mistakes,
                stars = if (failed) 0 else null,
                feedback = Feedback.Wrong,
                selectedSquare = null,
                legalTargets = emptySet(),
            )
        }

        val played = PlayedMove(
            uci = move.uci,
            san = result.san,
            byUser = true,
            isCorrect = true,
            timestampMs = state.elapsedMs,
            side = state.board.sideToMove,
        )
        val nextIndex = state.solutionIndex + 1
        val solved = isMate || nextIndex >= puzzle.solution.size

        val next = state.copy(
            board = result.board,
            moves = state.moves + played,
            status = if (solved) PuzzleState.Status.SOLVED else PuzzleState.Status.OPPONENT,
            solutionIndex = nextIndex,
            feedback = Feedback.Correct,
            selectedSquare = null,
            legalTargets = emptySet(),
            promotionChoices = emptyList(),
            activeHint = null,
            hintPiece = null,
            hintMove = null,
        )

        return if (solved) finishSolved(next) else next
    }

    private fun opponentReply(state: PuzzleState): PuzzleState {
        if (state.status != PuzzleState.Status.OPPONENT) return state
        val expected = puzzle.solution.getOrNull(state.solutionIndex)
            ?: return finishSolved(state)

        val move = parseUci(expected)
        val result = rules.move(state.board, move)
            ?: return state.copy(
                status = PuzzleState.Status.FAILED,
                stars = 0,
                feedback = Feedback.Illegal,
                selectedSquare = null,
                legalTargets = emptySet(),
            )

        val played = PlayedMove(
            uci = move.uci,
            san = result.san,
            byUser = false,
            isCorrect = true,
            timestampMs = state.elapsedMs,
            side = state.board.sideToMove,
        )
        val nextIndex = state.solutionIndex + 1
        val solved = result.isCheckmate || nextIndex >= puzzle.solution.size

        val next = state.copy(
            board = result.board,
            moves = state.moves + played,
            status = if (solved) PuzzleState.Status.SOLVED else PuzzleState.Status.SOLVING,
            solutionIndex = nextIndex,
            feedback = null,
        )

        return if (solved) finishSolved(next) else next
    }

    private fun tick(state: PuzzleState, elapsedMs: Long): PuzzleState {
        if (state.paused || state.finished) return state
        if (state.status != PuzzleState.Status.SOLVING &&
            state.status != PuzzleState.Status.OPPONENT
        ) {
            return state
        }

        val elapsed = max(state.elapsedMs, elapsedMs)
        if (elapsed >= state.timeLimitMs) {
            return state.copy(
                elapsedMs = state.timeLimitMs,
                status = PuzzleState.Status.FAILED,
                stars = 0,
                feedback = Feedback.Timeout,
                selectedSquare = null,
                legalTargets = emptySet(),
            )
        }

        return state.copy(elapsedMs = elapsed)
    }

    private fun useHint(state: PuzzleState, tier: HintTier): PuzzleState {
        if (state.finished || state.status != PuzzleState.Status.SOLVING) return state
        if (tier == HintTier.IDEA && !state.ideaAvailable) return state

        val expected = puzzle.solution.getOrNull(state.solutionIndex)?.let(::parseUci)
        if (tier != HintTier.IDEA && expected == null) return state

        return state.copy(
            hintsUsed = state.hintsUsed + tier,
            activeHint = tier,
            hintPiece = expected?.from.takeIf { tier != HintTier.IDEA },
            hintMove = expected.takeIf { tier == HintTier.MOVE },
            feedback = Feedback.Hint(tier),
            paused = false,
            selectedSquare = null,
            legalTargets = emptySet(),
        )
    }

    private fun continueWithTime(state: PuzzleState): PuzzleState {
        if (state.status != PuzzleState.Status.FAILED || state.continued) return state
        return state.copy(
            status = PuzzleState.Status.SOLVING,
            attemptsLeft = 1,
            timeLimitMs = state.timeLimitMs + 60_000L,
            continued = true,
            starsCap = min(state.starsCap, 1),
            stars = null,
            xp = null,
            feedback = null,
            paused = false,
            selectedSquare = null,
            legalTargets = emptySet(),
        )
    }

    private fun restart(state: PuzzleState): PuzzleState {
        if (!state.canRestart) return state
        val fresh = initialState()
        return fresh.copy(
            starsCap = min(state.starsCap, 1),
            continued = true,
            flipped = state.flipped,
        )
    }

    private fun reveal(state: PuzzleState): PuzzleState {
        if (state.status != PuzzleState.Status.FAILED) return state

        var board = state.board
        var moves = state.moves
        var index = state.solutionIndex

        while (index < puzzle.solution.size) {
            val move = parseUci(puzzle.solution[index])
            val result = rules.move(board, move) ?: break
            moves = moves + PlayedMove(
                uci = move.uci,
                san = result.san,
                byUser = index % 2 == 0,
                isCorrect = true,
                timestampMs = state.elapsedMs,
                side = board.sideToMove,
            )
            board = result.board
            index += 1
        }

        return state.copy(
            board = board,
            moves = moves,
            solutionIndex = index,
            status = PuzzleState.Status.REVIEW,
            stars = 0,
            xp = 10,
            revealUsed = true,
            feedback = null,
            selectedSquare = null,
            legalTargets = emptySet(),
            promotionChoices = emptyList(),
            activeHint = null,
            hintPiece = null,
            hintMove = null,
        )
    }

    private fun finishSolved(state: PuzzleState): PuzzleState {
        val stars = StarPolicy.calculate(
            starsCap = state.starsCap,
            hintsUsed = state.hintsUsed.size,
            mistakes = state.mistakes,
            elapsedMs = state.elapsedMs,
            timeLimitMs = state.timeLimitMs,
            continued = state.continued,
        )
        return state.copy(
            status = PuzzleState.Status.SOLVED,
            stars = stars,
            xp = XpPolicy.calculate(
                stars = stars,
                elapsedMs = state.elapsedMs,
                timeLimitMs = state.timeLimitMs,
                hintsUsed = state.hintsUsed.size,
            ),
            selectedSquare = null,
            legalTargets = emptySet(),
            promotionChoices = emptyList(),
        )
    }

    private fun parseUci(uci: String): Move {
        require(uci.length >= 4) { "Invalid UCI move: $uci" }
        val promotion = when (uci.getOrNull(4)?.lowercaseChar()) {
            'q' -> PieceType.QUEEN
            'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP
            'n' -> PieceType.KNIGHT
            else -> null
        }
        return Move(
            from = Square.fromAlgebraic(uci.substring(0, 2)),
            to = Square.fromAlgebraic(uci.substring(2, 4)),
            promotion = promotion,
        )
    }
}
