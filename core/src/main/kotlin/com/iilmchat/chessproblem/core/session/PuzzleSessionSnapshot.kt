package com.iilmchat.chessproblem.core.session

import com.iilmchat.chessproblem.core.model.HintTier
import java.io.Serializable

/**
 * Снимок активной сессии для SavedStateHandle. java.io.Serializable — единственный
 * Bundle-совместимый тип, доступный в чистом JVM-модуле: android.os.Parcelable
 * сюда импортировать нельзя (правила слоёв).
 */
data class PuzzleSessionSnapshot(
    val puzzleId: String,
    val solutionIndex: Int,
    val moves: List<PlayedMove>,
    val attemptsLeft: Int,
    val mistakes: Int,
    val hintsUsed: Set<HintTier>,
    val remainingMs: Long,
    val timeLimitMs: Long,
    val continued: Boolean,
    val flipped: Boolean,
    val status: PuzzleState.Status,
    val stars: Int? = null,
    val starsCap: Int = 3,
    val revealUsed: Boolean = false,
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

val PuzzleSessionSnapshot.finished: Boolean
    get() = status == PuzzleState.Status.SOLVED ||
        status == PuzzleState.Status.FAILED ||
        status == PuzzleState.Status.REVIEW

fun PuzzleState.toSnapshot(): PuzzleSessionSnapshot = PuzzleSessionSnapshot(
    puzzleId = puzzle.id,
    solutionIndex = solutionIndex,
    moves = moves,
    attemptsLeft = attemptsLeft,
    mistakes = mistakes,
    hintsUsed = hintsUsed,
    remainingMs = remainingMs,
    timeLimitMs = timeLimitMs,
    continued = continued,
    flipped = flipped,
    status = status,
    stars = stars,
    starsCap = starsCap,
    revealUsed = revealUsed,
)
