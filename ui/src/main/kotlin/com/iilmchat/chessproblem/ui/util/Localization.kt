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
    puzzle.task.type == TaskType.DRAW && puzzle.task.plies <= 1 -> stringResource(R.string.puzzle_task_draw_one)
    puzzle.task.type == TaskType.DRAW -> stringResource(R.string.puzzle_task_draw, puzzle.task.plies)
    puzzle.task.plies <= 1 -> stringResource(R.string.puzzle_task_win_one)
    else -> stringResource(R.string.puzzle_task_win, puzzle.task.plies)
}

@Composable
fun chapterTitle(chapter: String, level: Level): String {
    val key = "chapter_${level.id.lowercase()}_${chapter.replace('-', '_')}"
    val id = when (key) {
        "chapter_novice_mate_in_1" -> R.string.chapter_mate_in_1
        "chapter_novice_capture_queen" -> R.string.chapter_capture_queen
        "chapter_novice_fork" -> R.string.chapter_fork
        "chapter_novice_pin" -> R.string.chapter_pin
        "chapter_novice_back_rank" -> R.string.chapter_back_rank
        "chapter_novice_promotion" -> R.string.chapter_promotion
        "chapter_professional_mate_in_2" -> R.string.chapter_mate_in_2
        "chapter_professional_deflection" -> R.string.chapter_deflection
        "chapter_professional_decoy" -> R.string.chapter_decoy
        "chapter_professional_interference" -> R.string.chapter_interference
        "chapter_professional_endgame" -> R.string.chapter_endgame
        "chapter_professional_sacrifice" -> R.string.chapter_sacrifice
        "chapter_professional_mixed_review" -> R.string.chapter_mixed_review
        "chapter_grandmaster_quiet_move" -> R.string.chapter_quiet_move
        "chapter_grandmaster_deep_sacrifice" -> R.string.chapter_deep_sacrifice
        "chapter_grandmaster_zugzwang" -> R.string.chapter_zugzwang
        "chapter_grandmaster_underpromotion" -> R.string.chapter_underpromotion
        "chapter_grandmaster_rare_mate" -> R.string.chapter_rare_mate
        "chapter_grandmaster_calculation" -> R.string.chapter_calculation
        "chapter_grandmaster_final_exam" -> R.string.chapter_final_exam
        "chapter_novice_mate_in_2" -> R.string.chapter_mate_in_2
        "chapter_novice_double_attack" -> R.string.chapter_double_attack
        "chapter_novice_remove_defender" -> R.string.chapter_remove_defender
        "chapter_novice_discovered_attack" -> R.string.chapter_discovered_attack
        "chapter_novice_defense" -> R.string.chapter_defense
        "chapter_novice_mixed_review" -> R.string.chapter_mixed_review
        "chapter_professional_mate_in_3" -> R.string.chapter_mate_in_3
        "chapter_professional_x_ray" -> R.string.chapter_x_ray
        "chapter_professional_zwischenzug" -> R.string.chapter_zwischenzug
        "chapter_professional_pawn_break" -> R.string.chapter_pawn_break
        "chapter_professional_minor_endgame" -> R.string.chapter_minor_endgame
        "chapter_grandmaster_study_draw" -> R.string.chapter_study_draw
        "chapter_grandmaster_punish" -> R.string.chapter_punish
        "chapter_grandmaster_stalemate" -> R.string.chapter_stalemate
        "chapter_grandmaster_fortress" -> R.string.chapter_fortress
        "chapter_grandmaster_perpetual_check" -> R.string.chapter_perpetual_check
        else -> null
    }
    return if (id != null) stringResource(id) else chapter.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
fun sideToMoveLabel(puzzle: Puzzle): String {
    val white = puzzle.fen.split(' ').getOrNull(1) != "b"
    return stringResource(if (white) R.string.puzzle_side_white else R.string.puzzle_side_black)
}
