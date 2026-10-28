package com.iilmchat.chessproblem.core.scoring

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringTest {
    @Test
    fun `three stars require clean fast solve`() {
        assertEquals(
            3,
            StarPolicy.calculate(
                starsCap = 3,
                hintsUsed = 0,
                mistakes = 1,
                elapsedMs = 60_000,
                timeLimitMs = 120_000,
                continued = false,
            ),
        )
    }

    @Test
    fun `two stars allow one hint or two mistakes`() {
        assertEquals(
            2,
            StarPolicy.calculate(
                starsCap = 3,
                hintsUsed = 1,
                mistakes = 2,
                elapsedMs = 110_000,
                timeLimitMs = 120_000,
                continued = false,
            ),
        )
    }

    @Test
    fun `continue caps stars at one`() {
        assertEquals(
            1,
            StarPolicy.calculate(
                starsCap = 1,
                hintsUsed = 0,
                mistakes = 0,
                elapsedMs = 10_000,
                timeLimitMs = 120_000,
                continued = true,
            ),
        )
    }

    @Test
    fun `xp uses stars time and hints`() {
        assertEquals(200, XpPolicy.calculate(stars = 3, elapsedMs = 0, timeLimitMs = 120_000, hintsUsed = 0))
        assertEquals(130, XpPolicy.calculate(stars = 2, elapsedMs = 60_000, timeLimitMs = 120_000, hintsUsed = 1))
        assertEquals(10, XpPolicy.calculate(stars = 1, elapsedMs = 120_000, timeLimitMs = 120_000, hintsUsed = 5))
    }

    @Test
    fun `timer formula is rounded and clamped`() {
        assertEquals(185, TimerPolicy.timeLimitSec(rating = 1450, userMoves = 2))
        assertEquals(95, TimerPolicy.timeLimitSec(rating = 600, userMoves = 1))
        assertEquals(60, TimerPolicy.timeLimitSec(rating = 0, userMoves = 0))
        assertEquals(420, TimerPolicy.timeLimitSec(rating = 2400, userMoves = 15))
    }
}
