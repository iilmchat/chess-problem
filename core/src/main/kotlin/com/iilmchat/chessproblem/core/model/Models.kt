package com.iilmchat.chessproblem.core.model

enum class Level(
    val id: String,
    val minRating: Int,
    val maxRating: Int?,
) {
    NOVICE("NOVICE", 600, 1199),
    PROFESSIONAL("PROFESSIONAL", 1200, 1799),
    GRANDMASTER("GRANDMASTER", 1800, null),
    ;

    val ratingLabel: String
        get() = if (maxRating == null) "$minRating+" else "$minRating–$maxRating"

    companion object {
        fun fromId(value: String): Level = entries.firstOrNull {
            it.id.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        } ?: error("Unknown level: $value")
    }
}

data class LocalizedText(
    val ru: String,
    val en: String,
) {
    fun resolve(english: Boolean): String = if (english) en else ru
}

enum class TaskType {
    MATE,
    WIN,
    DRAW,
    ;

    companion object {
        fun fromId(value: String): TaskType = when (value.uppercase()) {
            "MATE" -> MATE
            "WIN" -> WIN
            "DRAW" -> DRAW
            else -> error("Unknown task type: $value")
        }
    }
}

data class PuzzleTask(
    val type: TaskType,
    val plies: Int,
)

enum class HintTier(
    val tokenCost: Int,
) {
    IDEA(0),
    PIECE(1),
    MOVE(2),
}

data class PuzzleHint(
    val tier: HintTier,
    val text: LocalizedText,
)

data class PuzzleSource(
    val provider: String,
    val id: String?,
    val license: String,
)

data class Puzzle(
    val id: String,
    val level: Level,
    val chapter: String,
    val fen: String,
    val setupMove: String?,
    val solution: List<String>,
    val task: PuzzleTask,
    val rating: Int,
    val themes: List<String>,
    val timeLimitSec: Int,
    val title: LocalizedText,
    val prompt: LocalizedText,
    val hints: List<PuzzleHint>,
    val explanation: LocalizedText,
    val source: PuzzleSource? = null,
) {
    val timeLimitMs: Long
        get() = timeLimitSec * 1_000L

    fun hint(tier: HintTier): PuzzleHint? = hints.firstOrNull { it.tier == tier }
}

data class PuzzleProgress(
    val puzzleId: String,
    val solved: Boolean = false,
    val reviewed: Boolean = false,
    val stars: Int = 0,
    val bestTimeMs: Long? = null,
    val attempts: Int = 0,
    val hintsUsed: Int = 0,
    val lastPlayedAt: Long = 0L,
) {
    val completed: Boolean
        get() = solved || reviewed
}

enum class AppLanguage {
    SYSTEM,
    RU,
    EN,
}

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

enum class BoardTheme {
    SAGE,
    CLASSIC,
    SLATE,
    HIGH_CONTRAST,
}

data class UserSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val boardTheme: BoardTheme = BoardTheme.SAGE,
    val coordinates: Boolean = true,
    val autoFlip: Boolean = false,
    val reduceMotion: Boolean = false,
    val sound: Boolean = true,
    val haptics: Boolean = true,
)
