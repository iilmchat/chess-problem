package com.iilmchat.chessproblem.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.session.Feedback
import com.iilmchat.chessproblem.core.session.PuzzleState
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.components.BoardCard
import com.iilmchat.chessproblem.ui.components.ChessTopBar
import com.iilmchat.chessproblem.ui.components.InfoPill
import com.iilmchat.chessproblem.ui.components.MoveLog
import com.iilmchat.chessproblem.ui.components.PrimaryButton
import com.iilmchat.chessproblem.ui.components.SecondaryButton
import com.iilmchat.chessproblem.ui.components.StarRow
import com.iilmchat.chessproblem.ui.components.StatCard
import com.iilmchat.chessproblem.ui.util.formatTime
import com.iilmchat.chessproblem.ui.util.levelTitle
import com.iilmchat.chessproblem.ui.util.localized
import com.iilmchat.chessproblem.ui.util.taskLabel
import com.iilmchat.chessproblem.core.model.BoardTheme

@Composable
fun ResultScreen(
    puzzle: Puzzle,
    state: PuzzleState,
    nextPuzzleId: String?,
    reviewBoard: Boolean,
    boardTheme: BoardTheme,
    coordinates: Boolean,
    onNext: (String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onRetry: () -> Unit,
    onReveal: () -> Unit,
    onToggleReviewBoard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val solved = state.status == PuzzleState.Status.SOLVED
    val reviewed = state.status == PuzzleState.Status.REVIEW
    val title = when {
        solved -> stringResource(R.string.result_solved_title)
        reviewed -> stringResource(R.string.result_review_title)
        state.feedback == Feedback.Timeout -> stringResource(R.string.result_timeout_title)
        else -> stringResource(R.string.result_failed_title)
    }
    val stars = state.stars ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        ChessTopBar(
            title = stringResource(R.string.result_title),
            subtitle = localized(puzzle.title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (solved || reviewed) {
                StarRow(stars = stars, size = 36.dp)
            } else {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.error,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = localized(puzzle.title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(text = taskLabel(puzzle))
                InfoPill(text = stringResource(R.string.puzzle_rating, puzzle.rating))
                InfoPill(text = levelTitle(puzzle.level))
            }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    label = stringResource(R.string.result_time),
                    value = formatTime(state.elapsedMs),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.result_moves),
                    value = "${state.userMoves}/${puzzle.task.plies}",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.result_xp),
                    value = when {
                        solved -> (state.xp ?: 0).toString()
                        reviewed -> "+10"
                        else -> "—"
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(childText(R.string.result_mistakes, state.mistakes))
                InfoPill(childText(R.string.result_hints_used, state.hintsUsed.size))
                if (state.continued) {
                    InfoPill(
                        text = stringResource(R.string.result_continued),
                        contentColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (reviewBoard) {
                Spacer(Modifier.height(16.dp))
                BoardCard(
                    board = state.board,
                    selected = null,
                    legalTargets = emptySet(),
                    lastMove = state.board.lastMove,
                    flipped = state.flipped,
                    hintPiece = null,
                    hintMove = null,
                    coordinates = coordinates,
                    boardTheme = boardTheme,
                    enabled = false,
                    onSquareClick = {},
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.result_explanation),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = localized(puzzle.explanation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (state.moves.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MoveLog(
                    moves = state.moves,
                    userMoves = state.userMoves,
                    totalUserMoves = puzzle.task.plies,
                )
            }

            Spacer(Modifier.height(20.dp))
            if (solved) {
                if (nextPuzzleId != null) {
                    PrimaryButton(
                        text = stringResource(R.string.result_next),
                        onClick = { onNext(nextPuzzleId) },
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.result_home),
                        onClick = onHome,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(
                        if (reviewBoard) R.string.result_hide_board else R.string.result_show_board,
                    ),
                    onClick = onToggleReviewBoard,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = stringResource(R.string.common_retry),
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.result_home),
                        onClick = onHome,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else if (reviewed) {
                if (nextPuzzleId != null) {
                    PrimaryButton(
                        text = stringResource(R.string.result_next),
                        onClick = { onNext(nextPuzzleId) },
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.result_home),
                        onClick = onHome,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.result_restart),
                    onClick = onRetry,
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.result_home),
                    onClick = onHome,
                )
            } else {
                if (state.canContinue) {
                    PrimaryButton(
                        text = stringResource(R.string.result_continue),
                        onClick = onContinue,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.result_continue_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = stringResource(R.string.result_restart),
                        onClick = onRestart,
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.result_restart),
                        onClick = onRestart,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.result_reveal),
                    onClick = onReveal,
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.result_home),
                    onClick = onHome,
                )
            }
        }
    }
}

@Composable
private fun childText(resId: Int, value: Int): String = stringResource(resId, value)
