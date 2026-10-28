package com.iilmchat.chessproblem.core.session

import com.iilmchat.chessproblem.core.chess.BoardSnapshot
import com.iilmchat.chessproblem.core.chess.Move
import com.iilmchat.chessproblem.core.chess.PieceType
import com.iilmchat.chessproblem.core.chess.Side
import com.iilmchat.chessproblem.core.chess.Square
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.model.Puzzle

data class PlayedMove(
    val uci: String,
    val san: String,
    val byUser: Boolean,
    val isCorrect: Boolean,
    val timestampMs: Long,
    val side: Side,
)

sealed interface Feedback {
    data object Wrong : Feedback
    data object Illegal : Feedback
    data object Correct : Feedback
    data object Timeout : Feedback
    data class Hint(val tier: HintTier) : Feedback
}

data class PuzzleState(
    val puzzle: Puzzle,
    val board: BoardSnapshot,
    val status: Status,
    val moves: List<PlayedMove> = emptyList(),
    val attemptsLeft: Int = 3,
    val mistakes: Int = 0,
    val hintsUsed: Set<HintTier> = emptySet(),
    val elapsedMs: Long = 0L,
    val timeLimitMs: Long = puzzle.timeLimitMs,
    val stars: Int? = null,
    val starsCap: Int = 3,
    val solutionIndex: Int = 0,
    val feedback: Feedback? = null,
    val selectedSquare: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val promotionChoices: List<Move> = emptyList(),
    val flipped: Boolean = false,
    val paused: Boolean = false,
    val continued: Boolean = false,
    val revealUsed: Boolean = false,
    val activeHint: HintTier? = null,
    val hintPiece: Square? = null,
    val hintMove: Move? = null,
    val xp: Int? = null,
) {
    enum class Status {
        SOLVING,
        OPPONENT,
        SOLVED,
        FAILED,
        REVIEW,
    }

    val remainingMs: Long
        get() = (timeLimitMs - elapsedMs).coerceAtLeast(0L)

    val userMoves: Int
        get() = moves.count { it.byUser }

    val totalUserMoves: Int
        get() = puzzle.task.plies

    val canContinue: Boolean
        get() = status == Status.FAILED && !continued

    val canRestart: Boolean
        get() = status == Status.FAILED || status == Status.REVIEW

    val ideaAvailable: Boolean
        get() = HintTier.IDEA !in hintsUsed

    val finished: Boolean
        get() = status == Status.SOLVED || status == Status.FAILED || status == Status.REVIEW
}

sealed interface PuzzleEvent {
    data class TapSquare(val square: Square) : PuzzleEvent
    data class MakeMove(
        val from: Square,
        val to: Square,
        val promotion: PieceType? = null,
    ) : PuzzleEvent

    data class Tick(val elapsedMs: Long) : PuzzleEvent
    data object OpponentReply : PuzzleEvent
    data object Pause : PuzzleEvent
    data object Resume : PuzzleEvent
    data class UseHint(val tier: HintTier) : PuzzleEvent
    data object ContinueWithTime : PuzzleEvent
    data object Restart : PuzzleEvent
    data object Reveal : PuzzleEvent
    data object FlipBoard : PuzzleEvent
    data object ClearFeedback : PuzzleEvent
    data object ClearHint : PuzzleEvent
    data object ClearPromotion : PuzzleEvent
}
