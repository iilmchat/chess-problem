package com.iilmchat.chessproblem.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class HintsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `шторка подсказок показывает все три уровня`() {
        val session = PuzzleState(
            puzzle = testPuzzle(),
            board = testBoard(),
            status = PuzzleState.Status.SOLVING,
        )

        composeTestRule.setContent {
            ChessProblemTheme(darkTheme = true) {
                HintsSheet(
                    session = session,
                    balance = 3,
                    onUseHint = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Подсказки").assertIsDisplayed()
        composeTestRule.onNodeWithText("Доступно: 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Идея").assertExists()
        composeTestRule.onNodeWithText("Фигура").assertExists()
        composeTestRule.onNodeWithText("Ход").assertExists()
        composeTestRule.onNodeWithText("Бесплатно").assertExists()
    }
}
