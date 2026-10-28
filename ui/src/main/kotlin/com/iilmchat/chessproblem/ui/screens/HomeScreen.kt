package com.iilmchat.chessproblem.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iilmchat.chessproblem.core.chess.BoardSnapshot
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.board.ChessBoard
import com.iilmchat.chessproblem.ui.board.boardPalette
import com.iilmchat.chessproblem.ui.components.ErrorState
import com.iilmchat.chessproblem.ui.components.InfoPill
import com.iilmchat.chessproblem.ui.components.LoadingState
import com.iilmchat.chessproblem.ui.components.SectionHeader
import com.iilmchat.chessproblem.ui.components.StarRow
import com.iilmchat.chessproblem.ui.theme.Brass
import com.iilmchat.chessproblem.ui.theme.Emerald
import com.iilmchat.chessproblem.ui.theme.Violet
import com.iilmchat.chessproblem.ui.util.levelDescription
import com.iilmchat.chessproblem.ui.util.levelTitle
import com.iilmchat.chessproblem.ui.util.localized
import com.iilmchat.chessproblem.ui.util.taskLabel
import com.iilmchat.chessproblem.ui.viewmodel.CatalogUiState

@Composable
fun HomeScreen(
    state: CatalogUiState,
    onPuzzleClick: (String) -> Unit,
    onLevelClick: (Level) -> Unit,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading -> LoadingState(modifier)
        state.error != null -> ErrorState(onRetry = onRetry, modifier = modifier)
        else -> HomeContent(
            state = state,
            onPuzzleClick = onPuzzleClick,
            onLevelClick = onLevelClick,
            onProgressClick = onProgressClick,
            onSettingsClick = onSettingsClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeContent(
    state: CatalogUiState,
    onPuzzleClick: (String) -> Unit,
    onLevelClick: (Level) -> Unit,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val continuePuzzle = remember(state.progress, state.puzzles) {
        state.progress.values
            .asSequence()
            .filter { it.lastPlayedAt > 0L && !it.completed }
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { item -> state.puzzles.firstOrNull { it.id == item.puzzleId } }
            .firstOrNull()
    }
    val daily = state.dailyPuzzle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeHeader(
                streak = state.streakDays(),
                onProgressClick = onProgressClick,
                onSettingsClick = onSettingsClick,
            )
        }
        if (daily != null) {
            item {
                DailyCard(
                    puzzle = daily,
                    onClick = { onPuzzleClick(daily.id) },
                )
            }
        }
        if (continuePuzzle != null) {
            item {
                ContinueCard(
                    puzzle = continuePuzzle,
                    onClick = { onPuzzleClick(continuePuzzle.id) },
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.home_levels_title))
        }
        Level.entries.forEach { level ->
            item(key = level.id) {
                LevelCard(
                    level = level,
                    total = state.puzzlesFor(level).size,
                    solved = state.solvedInLevel(level),
                    stars = state.starsInLevel(level),
                    onClick = { onLevelClick(level) },
                )
            }
        }
        if (state.total in 1..12) {
            item {
                Text(
                    text = pluralStringResource(R.plurals.home_demo_note_plural, state.total, state.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    streak: Int,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(46.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "\u265E",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                InfoPill(
                    text = pluralStringResource(R.plurals.home_streak_days_plural, streak, streak),
                    icon = Icons.Filled.LocalFireDepartment,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onProgressClick),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.a11y_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyCard(
    puzzle: Puzzle,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                InfoPill(
                    text = stringResource(R.string.home_daily_title),
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    background = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = localized(puzzle.title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localized(puzzle.prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(text = taskLabel(puzzle))
                    InfoPill(text = stringResource(R.string.puzzle_rating, puzzle.rating))
                }
            }
            Spacer(Modifier.width(12.dp))
            MiniBoard(puzzle = puzzle)
        }
    }
}

@Composable
private fun MiniBoard(puzzle: Puzzle) {
    val board = remember(puzzle.fen) {
        runCatching { BoardSnapshot.fromFen(puzzle.fen) }.getOrNull()
    }
    if (board == null) return
    Surface(
        modifier = Modifier.size(112.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(Modifier.padding(3.dp)) {
            ChessBoard(
                board = board,
                selected = null,
                legalTargets = emptySet(),
                lastMove = null,
                flipped = false,
                hintPiece = null,
                hintMove = null,
                coordinates = false,
                boardTheme = com.iilmchat.chessproblem.core.model.BoardTheme.SAGE,
                enabled = false,
                onSquareClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ContinueCard(
    puzzle: Puzzle,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u25B6",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_continue_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = localized(puzzle.title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            InfoPill(text = taskLabel(puzzle))
        }
    }
}

@Composable
private fun LevelCard(
    level: Level,
    total: Int,
    solved: Int,
    stars: Int,
    onClick: () -> Unit,
) {
    val accent = when (level) {
        Level.NOVICE -> Emerald
        Level.PROFESSIONAL -> Brass
        Level.GRANDMASTER -> Violet
    }
    val glyph = when (level) {
        Level.NOVICE -> "\u265F"
        Level.PROFESSIONAL -> "\u265C"
        Level.GRANDMASTER -> "\u265B"
    }
    val fraction = if (total > 0) solved.toFloat() / total else 0f
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.14f),
                contentColor = accent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = glyph, fontSize = 27.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = levelTitle(level),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = level.ratingLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = levelDescription(level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(accent),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.home_level_progress, solved, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                StarRow(stars = stars.coerceAtMost(3), size = 12.dp)
            }
        }
    }
}
