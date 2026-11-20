package com.iilmchat.chessproblem.ui.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.iilmchat.chessproblem.core.chess.Square
import com.iilmchat.chessproblem.core.model.BoardTheme
import com.iilmchat.chessproblem.ui.testBoard
import com.iilmchat.chessproblem.ui.theme.ChessProblemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru")
class ChessBoardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `выбор фигуры и ход тап-тап меняют состояние доски`() {
        val board = testBoard()
        val e2 = Square.fromAlgebraic("e2")
        val e4 = Square.fromAlgebraic("e4")
        val tapped = mutableListOf<Square>()
        val moves = mutableListOf<Pair<Square, Square>>()
        var selected: Square? by mutableStateOf(null)
        var legalTargets: Set<Square> by mutableStateOf(emptySet())

        composeTestRule.setContent {
            ChessProblemTheme(darkTheme = true) {
                ChessBoard(
                    board = board,
                    selected = selected,
                    legalTargets = legalTargets,
                    lastMove = null,
                    flipped = false,
                    hintPiece = null,
                    hintMove = null,
                    coordinates = true,
                    boardTheme = BoardTheme.SAGE,
                    enabled = true,
                    onSquareClick = { square ->
                        tapped += square
                        when {
                            square == e2 -> {
                                selected = square
                                legalTargets = setOf(e4)
                            }

                            square in legalTargets -> {
                                selected?.let { moves += it to square }
                                selected = null
                                legalTargets = emptySet()
                            }

                            else -> {
                                selected = null
                                legalTargets = emptySet()
                            }
                        }
                    },
                    onMove = { from, to -> moves += from to to },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("e2, белая пешка").performClick()
        composeTestRule.onNodeWithContentDescription("e4, пусто").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(e2, e4), tapped)
        assertEquals(listOf(e2 to e4), moves)
    }
}
