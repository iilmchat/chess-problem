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
@Config(sdk = [33], qualifiers = "ru-w360dp-h800dp-mdpi")
class PhoneScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `скриншот главной экрана телефона`() =
        captureHome(composeTestRule, "phone-01-home.png")

    @Test
    fun `скриншот решателя экрана телефона`() =
        captureSolver(composeTestRule, "phone-02-solver.png")

    @Test
    fun `скриншот подсказок экрана телефона`() =
        captureHints(composeTestRule, "phone-03-hints.png")

    @Test
    fun `скриншот результата экрана телефона`() =
        captureResult(composeTestRule, "phone-04-result.png")

    @Test
    fun `скриншот прогресса экрана телефона`() =
        captureProgress(composeTestRule, "phone-05-progress.png")
}
