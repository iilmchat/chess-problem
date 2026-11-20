package com.iilmchat.chessproblem.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val TabletBreakpoint = 600.dp
private val ContentMaxWidth = 560.dp
private val TabletHorizontalMargin = 32.dp

/**
 * Ограничивает контент колонкой 560 dp и центрирует её на планшетах 7–10".
 * На телефоне сохраняется прежняя ширина: поля задаёт сам экран.
 */
@Composable
fun ResponsiveScaffoldContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val tablet = maxWidth >= TabletBreakpoint
        val horizontalMargin = if (tablet) TabletHorizontalMargin else 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalMargin),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.widthIn(max = ContentMaxWidth)) {
                content()
            }
        }
    }
}
