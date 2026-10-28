package com.iilmchat.chessproblem.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.LocalizedText
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.TaskType
import com.iilmchat.chessproblem.ui.R
import androidx.compose.ui.res.stringResource
import java.util.Locale

@Composable
fun isEnglish(): Boolean {
    val locales = LocalConfiguration.current.locales
    return locales[0]?.language == "en"
}

@Composable
fun localized(text: LocalizedText): String = text.resolve(isEnglish())

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun formatShortTime(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        stringResource(R.string.time_short_minutes, minutes, seconds)
    } else {
        stringResource(R.string.time_short_seconds, seconds)
    }
}

@Composable
fun levelTitle(level: Level): String = stringResource(
    when (level) {
        Level.NOVICE -> R.string.home_level_novice
        Level.PROFESSIONAL -> R.string.home_level_professional
        Level.GRANDMASTER -> R.string.home_level_grandmaster
    },
)

@Composable
fun levelDescription(level: Level): String = stringResource(
    when (level) {
        Level.NOVICE -> R.string.home_level_novice_desc
        Level.PROFESSIONAL -> R.string.home_level_professional_desc
        Level.GRANDMASTER -> R.string.home_level_grandmaster_desc
    },
)

@Composable
fun taskLabel(puzzle: Puzzle): String = when {
    puzzle.task.type == TaskType.MATE && puzzle.task.plies <= 1 -> stringResource(R.string.puzzle_task_mate_one)
    puzzle.task.type == TaskType.MATE -> stringResource(R.string.puzzle_task_mate, puzzle.task.plies)
    puzzle.task.plies <= 1 -> stringResource(R.string.puzzle_task_win_one)
    else -> stringResource(R.string.puzzle_task_win, puzzle.task.plies)
}

@Composable
fun chapterTitle(chapter: String, level: Level): String {
    val key = "chapter_${level.id.lowercase()}_${chapter.replace('-', '_')}"
    val id = when (key) {
        "chapter_novice_mate_in_1" -> R.string.chapter_mate_in_1
        "chapter_novice_promotion" -> R.string.chapter_promotion
        "chapter_novice_capture_queen" -> R.string.chapter_capture_queen
        "chapter_professional_mate_in_2" -> R.string.chapter_mate_in_2
        "chapter_professional_deflection" -> R.string.chapter_deflection
        "chapter_professional_mixed_review" -> R.string.chapter_mixed_review
        "chapter_grandmaster_quiet_move" -> R.string.chapter_quiet_move
        "chapter_grandmaster_deep_sacrifice" -> R.string.chapter_deep_sacrifice
        "chapter_grandmaster_final_exam" -> R.string.chapter_final_exam
        else -> null
    }
    return if (id != null) stringResource(id) else chapter.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
fun sideToMoveLabel(puzzle: Puzzle): String {
    val white = puzzle.fen.split(' ').getOrNull(1) != "b"
    return stringResource(if (white) R.string.puzzle_side_white else R.string.puzzle_side_black)
}
