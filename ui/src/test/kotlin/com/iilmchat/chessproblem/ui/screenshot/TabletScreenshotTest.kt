package com.iilmchat.chessproblem.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-w800dp-h1280dp-mdpi")
class TabletScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `скриншот главной экрана планшета`() =
        captureHome(composeTestRule, "tablet-01-home.png")

    @Test
    fun `скриншот решателя экрана планшета`() =
        captureSolver(composeTestRule, "tablet-02-solver.png")

    @Test
    fun `скриншот подсказок экрана планшета`() =
        captureHints(composeTestRule, "tablet-03-hints.png")

    @Test
    fun `скриншот результата экрана планшета`() =
        captureResult(composeTestRule, "tablet-04-result.png")

    @Test
    fun `скриншот прогресса экрана планшета`() =
        captureProgress(composeTestRule, "tablet-05-progress.png")
}
