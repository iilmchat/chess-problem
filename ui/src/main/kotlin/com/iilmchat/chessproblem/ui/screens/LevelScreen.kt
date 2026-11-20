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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.components.ChessTopBar
import com.iilmchat.chessproblem.ui.components.ErrorState
import com.iilmchat.chessproblem.ui.components.InfoPill
import com.iilmchat.chessproblem.ui.components.LoadingState
import com.iilmchat.chessproblem.ui.components.ResponsiveScaffoldContainer
import com.iilmchat.chessproblem.ui.components.SectionHeader
import com.iilmchat.chessproblem.ui.components.StarRow
import com.iilmchat.chessproblem.ui.theme.Brass
import com.iilmchat.chessproblem.ui.theme.Emerald
import com.iilmchat.chessproblem.ui.theme.Violet
import com.iilmchat.chessproblem.ui.util.chapterTitle
import com.iilmchat.chessproblem.ui.util.levelTitle
import com.iilmchat.chessproblem.ui.viewmodel.CatalogUiState
import com.iilmchat.chessproblem.ui.viewmodel.ChapterUi

@Composable
fun LevelScreen(
    level: Level,
    state: CatalogUiState,
    onBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsiveScaffoldContainer(modifier = modifier) {
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(onRetry = onRetry)
            else -> {
                val chapters = state.chaptersFor(level)
                val accent = levelAccent(level)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ChessTopBar(
                            title = levelTitle(level),
                            subtitle = stringResource(R.string.level_subtitle),
                            onBack = onBack,
                        )
                    }
                    item {
                        LevelHero(
                            level = level,
                            total = state.puzzlesFor(level).size,
                            solved = state.solvedInLevel(level),
                            stars = state.starsInLevel(level),
                            accent = accent,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item {
                        SectionHeader(
                            title = stringResource(R.string.level_chapters),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (chapters.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.chapter_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        chapters.forEach { chapter ->
                            item(key = chapter.id) {
                                ChapterCard(
                                    chapter = chapter,
                                    level = level,
                                    accent = accent,
                                    onClick = { onChapterClick(chapter.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelHero(
    level: Level,
    total: Int,
    solved: Int,
    stars: Int,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total > 0) solved.toFloat() / total else 0f
    val glyph = when (level) {
        Level.NOVICE -> "\u265F"
        Level.PROFESSIONAL -> "\u265C"
        Level.GRANDMASTER -> "\u265B"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = glyph, fontSize = 30.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = level.ratingLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_level_progress, solved, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$stars ★",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.level_puzzles),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
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
    }
}

@Composable
private fun ChapterCard(
    chapter: ChapterUi,
    level: Level,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = chapter.puzzles.size
    val fraction = if (total > 0) chapter.solved.toFloat() / total else 0f
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chapter.id.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = chapterTitle(chapter.id, level),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.home_level_progress, chapter.solved, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${chapter.stars} ★",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
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
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun ChapterScreen(
    level: Level,
    chapterId: String,
    state: CatalogUiState,
    onBack: () -> Unit,
    onPuzzleClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsiveScaffoldContainer(modifier = modifier) {
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(onRetry = onRetry)
            else -> {
                val chapter = state.chapter(level, chapterId)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        ChessTopBar(
                            title = chapterTitle(chapterId, level),
                            subtitle = levelTitle(level),
                            onBack = onBack,
                        )
                    }
                    if (chapter == null || chapter.puzzles.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.chapter_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                InfoPill(
                                    text = stringResource(
                                        R.string.home_level_progress,
                                        chapter.solved,
                                        chapter.puzzles.size,
                                    ),
                                )
                                InfoPill(text = "${chapter.stars} ★")
                            }
                        }
                        chapter.puzzles.forEachIndexed { index, puzzle ->
                            item(key = puzzle.id) {
                                PuzzleRow(
                                    index = index + 1,
                                    puzzle = puzzle,
                                    stars = state.progressFor(puzzle.id)?.stars ?: 0,
                                    solved = state.progressFor(puzzle.id)?.solved == true,
                                    onClick = { onPuzzleClick(puzzle.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleRow(
    index: Int,
    puzzle: com.iilmchat.chessproblem.core.model.Puzzle,
    stars: Int,
    solved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (solved) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (solved) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (solved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = com.iilmchat.chessproblem.ui.util.localized(puzzle.title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = com.iilmchat.chessproblem.ui.util.taskLabel(puzzle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StarRow(stars = stars, size = 13.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.puzzle_rating, puzzle.rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

private fun levelAccent(level: Level): androidx.compose.ui.graphics.Color = when (level) {
    Level.NOVICE -> Emerald
    Level.PROFESSIONAL -> Brass
    Level.GRANDMASTER -> Violet
}
