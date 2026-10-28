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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.components.ChessTopBar
import com.iilmchat.chessproblem.ui.components.ErrorState
import com.iilmchat.chessproblem.ui.components.LoadingState
import com.iilmchat.chessproblem.ui.components.PrimaryButton
import com.iilmchat.chessproblem.ui.components.SectionHeader
import com.iilmchat.chessproblem.ui.components.StarRow
import com.iilmchat.chessproblem.ui.components.StatCard
import com.iilmchat.chessproblem.ui.theme.Brass
import com.iilmchat.chessproblem.ui.theme.Emerald
import com.iilmchat.chessproblem.ui.theme.Violet
import com.iilmchat.chessproblem.ui.util.formatShortTime
import com.iilmchat.chessproblem.ui.util.levelTitle
import com.iilmchat.chessproblem.ui.util.localized
import com.iilmchat.chessproblem.ui.viewmodel.CatalogUiState
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    state: CatalogUiState,
    onPuzzleClick: (String) -> Unit,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading -> LoadingState(modifier)
        state.error != null -> ErrorState(onRetry = onRetry, modifier = modifier)
        else -> {
            val solved = state.solvedCount
            val total = state.total
            val stars = state.totalStars
            val accuracy = if (total > 0) {
                ((solved * 100.0) / total).roundToInt()
            } else {
                0
            }
            val bestTimes = state.progress.values.mapNotNull { it.bestTimeMs }
            val avgTime = if (bestTimes.isEmpty()) 0L else bestTimes.average().toLong()
            val recent = state.recentProgress(5)

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ChessTopBar(
                        title = stringResource(R.string.progress_title),
                        subtitle = stringResource(R.string.progress_subtitle),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = stringResource(R.string.progress_solved),
                            value = "$solved/$total",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = stringResource(R.string.progress_stars),
                            value = stars.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = stringResource(R.string.progress_accuracy),
                            value = "$accuracy%",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = stringResource(R.string.progress_time),
                            value = if (avgTime > 0L) formatShortTime(avgTime) else "—",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SectionHeader(title = stringResource(R.string.progress_levels))
                }
                Level.entries.forEach { level ->
                    item(key = level.id) {
                        ProgressLevelCard(
                            level = level,
                            solved = state.solvedInLevel(level),
                            total = state.puzzlesFor(level).size,
                            stars = state.starsInLevel(level),
                        )
                    }
                }
                item {
                    SectionHeader(title = stringResource(R.string.progress_recent))
                }
                if (recent.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.progress_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(14.dp))
                                PrimaryButton(
                                    text = stringResource(R.string.progress_start),
                                    onClick = onPlay,
                                )
                            }
                        }
                    }
                } else {
                    recent.forEach { (puzzle, progress) ->
                        item(key = puzzle.id) {
                            RecentPuzzleRow(
                                title = localized(puzzle.title),
                                level = levelTitle(puzzle.level),
                                stars = progress.stars,
                                time = progress.bestTimeMs,
                                onClick = { onPuzzleClick(puzzle.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressLevelCard(
    level: Level,
    solved: Int,
    total: Int,
    stars: Int,
) {
    val accent = when (level) {
        Level.NOVICE -> Emerald
        Level.PROFESSIONAL -> Brass
        Level.GRANDMASTER -> Violet
    }
    val fraction = if (total > 0) solved.toFloat() / total else 0f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = levelTitle(level),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.home_level_progress, solved, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$stars ★",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun RecentPuzzleRow(
    title: String,
    level: String,
    stars: Int,
    time: Long?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = level,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (time != null) {
                Text(
                    text = formatShortTime(time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
            }
            StarRow(stars = stars, size = 13.dp)
        }
    }
}
