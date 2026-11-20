package com.iilmchat.chessproblem.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.session.Feedback
import com.iilmchat.chessproblem.core.session.PlayedMove
import com.iilmchat.chessproblem.core.session.PuzzleState
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.components.AttemptsRow
import com.iilmchat.chessproblem.ui.components.BoardCard
import com.iilmchat.chessproblem.ui.components.ErrorState
import com.iilmchat.chessproblem.ui.components.InfoPill
import com.iilmchat.chessproblem.ui.components.LoadingState
import com.iilmchat.chessproblem.ui.components.MoveLog
import com.iilmchat.chessproblem.ui.components.ResponsiveScaffoldContainer
import com.iilmchat.chessproblem.ui.components.TimerPill
import com.iilmchat.chessproblem.ui.util.chapterTitle
import com.iilmchat.chessproblem.ui.util.levelTitle
import com.iilmchat.chessproblem.ui.util.localized
import com.iilmchat.chessproblem.ui.util.taskLabel
import com.iilmchat.chessproblem.ui.viewmodel.PuzzleUiState
import com.iilmchat.chessproblem.ui.viewmodel.PuzzleViewModel

@Composable
fun PuzzleScreen(
    viewModel: PuzzleViewModel,
    nextPuzzleId: String?,
    onBack: () -> Unit,
    onNext: (String) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onLifecycleStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        if (ui.resultVisible) onBack() else viewModel.requestExit()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(ui.messageRes) {
        ui.messageRes?.let { res ->
            snackbarHostState.showSnackbar(context.getString(res))
            viewModel.clearMessage()
        }
    }

    ResponsiveScaffoldContainer(modifier = modifier) {
        when {
            ui.loading -> LoadingState()
            ui.puzzle == null || ui.session == null -> ErrorState(onRetry = onBack)
            ui.resultVisible -> {
                ResultScreen(
                    puzzle = ui.puzzle!!,
                    state = ui.session!!,
                    nextPuzzleId = nextPuzzleId,
                    reviewBoard = ui.reviewBoard,
                    boardTheme = ui.settings.boardTheme,
                    coordinates = ui.settings.coordinates,
                    onNext = onNext,
                    onHome = onHome,
                    onBack = onBack,
                    onContinue = viewModel::continueWithTime,
                    onRestart = viewModel::restartAfterFail,
                    onRetry = viewModel::retry,
                    onReveal = viewModel::reveal,
                    onToggleReviewBoard = viewModel::toggleReviewBoard,
                    modifier = Modifier,
                )
            }

            else -> {
                val puzzle = ui.puzzle!!
                val session = ui.session!!
                val boardEnabled = session.status == PuzzleState.Status.SOLVING &&
                    !session.paused && !session.finished
                val hintText = hintText(ui, session)
                val feedbackText = feedbackText(session)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        PuzzleTopBar(
                            puzzleTitle = localized(puzzle.title),
                            chapter = chapterTitle(puzzle.chapter, puzzle.level),
                            level = levelTitle(puzzle.level),
                            session = session,
                            onBack = viewModel::requestExit,
                            onPause = viewModel::pause,
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TaskCard(
                                ui = ui,
                                session = session,
                            )
                            BoardCard(
                                board = session.board,
                                selected = session.selectedSquare,
                                legalTargets = session.legalTargets,
                                lastMove = session.board.lastMove,
                                flipped = session.flipped,
                                hintPiece = session.hintPiece,
                                hintMove = session.hintMove,
                                coordinates = ui.settings.coordinates,
                                boardTheme = ui.settings.boardTheme,
                                enabled = boardEnabled,
                                onSquareClick = viewModel::onTapSquare,
                                onMove = { from, to -> viewModel.onMakeMove(from, to, null) },
                                haptics = ui.settings.haptics,
                            )
                            StatusLine(
                                hintText = hintText,
                                feedbackText = feedbackText,
                                session = session,
                            )
                            MoveLog(
                                moves = session.moves,
                                userMoves = session.userMoves,
                                totalUserMoves = puzzle.task.plies,
                            )
                            Spacer(Modifier.height(4.dp))
                        }

                        BottomActions(
                            hintBalance = ui.hintBalance,
                            onHint = viewModel::openHints,
                            onFlip = viewModel::flipBoard,
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 76.dp),
                    )
                }

                if (ui.showHints) {
                    HintsSheet(
                        session = session,
                        balance = ui.hintBalance,
                        onUseHint = viewModel::useHint,
                        onDismiss = viewModel::dismissHints,
                    )
                }
                if (ui.showPause) {
                    PauseDialog(
                        onResume = viewModel::resume,
                        onExit = viewModel::requestExit,
                    )
                }
                if (ui.showExit) {
                    ExitDialog(
                        onConfirm = onBack,
                        onDismiss = viewModel::dismissExit,
                    )
                }
                if (session.promotionChoices.isNotEmpty()) {
                    PromotionDialog(
                        session = session,
                        onChoose = { promotion ->
                            val move = session.promotionChoices.firstOrNull { it.promotion == promotion }
                            if (move != null) {
                                viewModel.onMakeMove(move.from, move.to, move.promotion)
                            }
                        },
                        onDismiss = viewModel::clearPromotion,
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleTopBar(
    puzzleTitle: String,
    chapter: String,
    level: String,
    session: PuzzleState,
    onBack: () -> Unit,
    onPause: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.a11y_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = chapter,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$level · $puzzleTitle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        TimerPill(
            remainingMs = session.remainingMs,
            totalMs = session.timeLimitMs,
            paused = session.paused,
            onClick = onPause,
        )
    }
}

@Composable
private fun TaskCard(
    ui: PuzzleUiState,
    session: PuzzleState,
) {
    val puzzle = ui.puzzle ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = localized(puzzle.title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = localized(puzzle.prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                InfoPill(
                    text = taskLabel(puzzle),
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    background = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoPill(text = stringResource(R.string.puzzle_rating, puzzle.rating))
                Spacer(Modifier.width(8.dp))
                AttemptsRow(attemptsLeft = session.attemptsLeft)
                Spacer(Modifier.weight(1f))
                if (session.continued) {
                    InfoPill(
                        text = "★",
                        contentColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    hintText: String?,
    feedbackText: String?,
    session: PuzzleState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            hintText != null -> HintStatus(text = hintText)
            feedbackText != null -> Text(
                text = feedbackText,
                style = MaterialTheme.typography.bodyMedium,
                color = when (session.feedback) {
                    Feedback.Wrong, Feedback.Illegal, Feedback.Timeout -> MaterialTheme.colorScheme.error
                    Feedback.Correct -> MaterialTheme.colorScheme.primary
                    is Feedback.Hint -> MaterialTheme.colorScheme.secondary
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            session.status == PuzzleState.Status.OPPONENT -> Text(
                text = stringResource(R.string.puzzle_opponent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            session.board.checkmate -> InfoPill(
                text = stringResource(R.string.common_checkmate),
                contentColor = MaterialTheme.colorScheme.error,
                background = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            )

            session.board.inCheck -> InfoPill(
                text = stringResource(R.string.common_check),
                contentColor = MaterialTheme.colorScheme.error,
                background = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun HintStatus(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BottomActions(
    hintBalance: Int,
    onHint: () -> Unit,
    onFlip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            label = stringResource(R.string.puzzle_hint),
            icon = Icons.Filled.Lightbulb,
            badge = hintBalance.toString(),
            onClick = onHint,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            label = stringResource(R.string.puzzle_flip),
            icon = Icons.Filled.Flip,
            onClick = onFlip,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (badge != null) {
                Spacer(Modifier.width(7.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun hintText(ui: PuzzleUiState, session: PuzzleState): String? {
    val tier = session.activeHint ?: return null
    val puzzle = ui.puzzle ?: return null
    return puzzle.hint(tier)?.let { hint -> localized(hint.text) } ?: stringResource(
        when (tier) {
            HintTier.IDEA -> R.string.puzzle_hint_idea_text
            HintTier.PIECE -> R.string.puzzle_hint_piece_text
            HintTier.MOVE -> R.string.puzzle_hint_move_text
        },
    )
}

@Composable
private fun feedbackText(session: PuzzleState): String? = when (session.feedback) {
    Feedback.Wrong -> stringResource(R.string.puzzle_wrong)
    Feedback.Illegal -> stringResource(R.string.puzzle_illegal)
    Feedback.Correct -> stringResource(R.string.puzzle_correct)
    Feedback.Timeout -> stringResource(R.string.puzzle_timeout)
    is Feedback.Hint -> null
    null -> null
}

@Suppress("unused")
private fun moveCount(moves: List<PlayedMove>): Int = moves.size
