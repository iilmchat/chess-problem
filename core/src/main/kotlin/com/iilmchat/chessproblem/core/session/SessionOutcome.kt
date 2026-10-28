package com.iilmchat.chessproblem.core.session

data class SessionOutcome(
    val puzzleId: String,
    val solved: Boolean,
    val reviewed: Boolean,
    val stars: Int,
    val xp: Int,
    val elapsedMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val continued: Boolean,
)

fun PuzzleState.toOutcome(): SessionOutcome = SessionOutcome(
    puzzleId = puzzle.id,
    solved = status == PuzzleState.Status.SOLVED,
    reviewed = status == PuzzleState.Status.REVIEW,
    stars = stars ?: 0,
    xp = xp ?: 0,
    elapsedMs = elapsedMs,
    mistakes = mistakes,
    hintsUsed = hintsUsed.size,
    continued = continued,
)
