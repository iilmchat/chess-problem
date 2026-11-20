package com.iilmchat.chessproblem.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.iilmchat.chessproblem.ui.testPuzzle
import com.iilmchat.chessproblem.ui.theme.ChessProblemTheme
import com.iilmchat.chessproblem.ui.viewmodel.CatalogUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru")
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `главный экран показывает уровни и задачу дня`() {
        composeTestRule.setContent {
            ChessProblemTheme(darkTheme = true) {
                HomeScreen(
                    state = CatalogUiState(loading = false, puzzles = listOf(testPuzzle())),
                    onPuzzleClick = {},
                    onLevelClick = {},
                    onProgressClick = {},
                    onSettingsClick = {},
                    onRetry = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Уровни").assertIsDisplayed()
        composeTestRule.onNodeWithText("Задача дня").assertIsDisplayed()
        composeTestRule.onNodeWithText("Новичок").assertExists()
    }
}
