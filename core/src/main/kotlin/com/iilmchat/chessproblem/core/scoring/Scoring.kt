package com.iilmchat.chessproblem.core.scoring

import kotlin.math.min
import kotlin.math.roundToInt

object StarPolicy {
    fun calculate(
        starsCap: Int,
        hintsUsed: Int,
        mistakes: Int,
        elapsedMs: Long,
        timeLimitMs: Long,
        continued: Boolean,
    ): Int {
        val base = when {
            continued -> 1
            hintsUsed == 0 &&
                mistakes <= 1 &&
                elapsedMs <= timeLimitMs * 6 / 10 -> 3

            hintsUsed <= 1 &&
                mistakes <= 2 &&
                elapsedMs <= timeLimitMs -> 2

            else -> 1
        }
        return base.coerceIn(0, starsCap.coerceIn(0, 3))
    }
}

object XpPolicy {
    fun calculate(
        stars: Int,
        elapsedMs: Long,
        timeLimitMs: Long,
        hintsUsed: Int,
    ): Int {
        val starBonus = when (stars) {
            3 -> 50
            2 -> 25
            else -> 0
        }
        val timeRatio = if (timeLimitMs <= 0L) {
            0.0
        } else {
            (1.0 - elapsedMs.toDouble() / timeLimitMs).coerceIn(0.0, 1.0)
        }
        val timeBonus = (50.0 * timeRatio).roundToInt()
        return (100 + starBonus + timeBonus - 20 * hintsUsed).coerceAtLeast(10)
    }

    fun reviewXp(): Int = 10
}

object TimerPolicy {
    fun timeLimitSec(rating: Int, userMoves: Int): Int {
        val raw = 30.0 + 0.09 * rating + 12.0 * userMoves
        val rounded = (raw / 5.0).roundToInt() * 5
        return rounded.coerceIn(60, 420)
    }
}

fun Int.coerceStars(): Int = min(this, 3).coerceAtLeast(0)
