package com.iilmchat.chessproblem.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iilmchat.chessproblem.core.model.PuzzleProgress

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val puzzleId: String,
    val solved: Boolean,
    val reviewed: Boolean,
    val stars: Int,
    val bestTimeMs: Long?,
    val attempts: Int,
    val hintsUsed: Int,
    val lastPlayedAt: Long,
    // KI-018: обе колонки добавлены миграцией 1 -> 2 со значением по умолчанию ''.
    val chapterId: String = "",
    val contentVersion: String = "",
)

fun PuzzleProgressEntity.toModel() = PuzzleProgress(
    puzzleId = puzzleId,
    solved = solved,
    reviewed = reviewed,
    stars = stars,
    bestTimeMs = bestTimeMs,
    attempts = attempts,
    hintsUsed = hintsUsed,
    lastPlayedAt = lastPlayedAt,
    chapterId = chapterId,
    contentVersion = contentVersion,
)

fun PuzzleProgress.toEntity() = PuzzleProgressEntity(
    puzzleId = puzzleId,
    solved = solved,
    reviewed = reviewed,
    stars = stars,
    bestTimeMs = bestTimeMs,
    attempts = attempts,
    hintsUsed = hintsUsed,
    lastPlayedAt = lastPlayedAt,
    chapterId = chapterId,
    contentVersion = contentVersion,
)
