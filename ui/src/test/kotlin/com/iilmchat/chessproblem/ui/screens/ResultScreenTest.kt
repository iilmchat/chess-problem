package com.iilmchat.chessproblem.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.iilmchat.chessproblem.core.chess.Side
import com.iilmchat.chessproblem.core.model.BoardTheme
import com.iilmchat.chessproblem.core.session.PlayedMove
import com.iilmchat.chessproblem.core.session.PuzzleState
import com.iilmchat.chessproblem.ui.testBoard
import com.iilmchat.chessproblem.ui.testPuzzle
import com.iilmchat.chessproblem.ui.theme.ChessProblemTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru")
class ResultScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `экран результата показывает звёзды и разбор`() {
        val puzzle = testPuzzle()
        val session = PuzzleState(
            puzzle = puzzle,
            board = testBoard(),
            status = PuzzleState.Status.SOLVED,
            stars = 3,
            elapsedMs = 98_000L,
            xp = 150,
            moves = listOf(
                PlayedMove("e2e4", "e4", true, true, 0L, Side.WHITE),
                PlayedMove("e7e5", "e5", false, true, 0L, Side.BLACK),
            ),
        )

        composeTestRule.setContent {
            ChessProblemTheme(darkTheme = true) {
                ResultScreen(
                    puzzle = puzzle,
                    state = session,
                    nextPuzzleId = "test-002",
                    reviewBoard = true,
                    boardTheme = BoardTheme.SAGE,
                    coordinates = true,
                    onNext = {},
                    onHome = {},
                    onBack = {},
                    onContinue = {},
                    onRestart = {},
                    onRetry = {},
                    onReveal = {},
                    onToggleReviewBoard = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Задача решена").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("3 из 3 звёзд").assertIsDisplayed()
        composeTestRule.onNodeWithText("Почему это работает").assertExists()
        composeTestRule.onNodeWithText(
            "Ладья удерживает линию, ферзь завершает атаку.",
        ).assertExists()
        composeTestRule.onNodeWithText("Следующая задача").assertExists()
    }
}
