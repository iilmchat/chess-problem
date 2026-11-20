package com.iilmchat.chessproblem.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.iilmchat.chessproblem.core.chess.Side
import com.iilmchat.chessproblem.core.model.BoardTheme
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.session.PlayedMove
import com.iilmchat.chessproblem.core.session.PuzzleState
import com.iilmchat.chessproblem.ui.screens.HintsSheet
import com.iilmchat.chessproblem.ui.screens.HomeScreen
import com.iilmchat.chessproblem.ui.screens.ProgressScreen
import com.iilmchat.chessproblem.ui.screens.PuzzleScreen
import com.iilmchat.chessproblem.ui.screens.ResultScreen
import com.iilmchat.chessproblem.ui.testBoard
import com.iilmchat.chessproblem.ui.testPuzzle
import com.iilmchat.chessproblem.ui.theme.ChessProblemTheme
import com.iilmchat.chessproblem.ui.viewmodel.CatalogUiState

@Composable
private fun ThemedContent(content: @Composable () -> Unit) {
    ChessProblemTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            content()
        }
    }
}

internal fun captureHome(rule: ComposeContentTestRule, file: String) {
    rule.setContent {
        ThemedContent {
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
    rule.onRoot().captureRoboImage(file)
}

internal fun captureSolver(rule: ComposeContentTestRule, file: String) {
    val viewModel = screenshotPuzzleViewModel()
    rule.setContent {
        ThemedContent {
            PuzzleScreen(
                viewModel = viewModel,
                nextPuzzleId = null,
                onBack = {},
                onNext = {},
                onHome = {},
            )
        }
    }
    rule.waitUntil(timeoutMillis = 10_000) {
        rule.onAllNodesWithText("Тихий ход").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onRoot().captureRoboImage(file)
}

@OptIn(ExperimentalRoborazziApi::class)
internal fun captureHints(rule: ComposeContentTestRule, file: String) {
    val session = PuzzleState(
        puzzle = testPuzzle(),
        board = testBoard(),
        status = PuzzleState.Status.SOLVING,
    )
    rule.setContent {
        ThemedContent {
            HintsSheet(
                session = session,
                balance = 3,
                onUseHint = {},
                onDismiss = {},
            )
        }
    }
    captureScreenRoboImage(file)
}

internal fun captureResult(rule: ComposeContentTestRule, file: String) {
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
    rule.setContent {
        ThemedContent {
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
    rule.onRoot().captureRoboImage(file)
}

internal fun captureProgress(rule: ComposeContentTestRule, file: String) {
    val puzzle = testPuzzle()
    val progress = mapOf(
        puzzle.id to PuzzleProgress(
            puzzleId = puzzle.id,
            solved = true,
            stars = 3,
            bestTimeMs = 98_000L,
            lastPlayedAt = 1L,
        ),
    )
    rule.setContent {
        ThemedContent {
            ProgressScreen(
                state = CatalogUiState(
                    loading = false,
                    puzzles = listOf(puzzle),
                    progress = progress,
                ),
                onPuzzleClick = {},
                onPlay = {},
                onRetry = {},
            )
        }
    }
    rule.onRoot().captureRoboImage(file)
}
