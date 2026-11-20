package com.iilmchat.chessproblem.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iilmchat.chessproblem.core.model.ChapterProgress

@Entity(tableName = "chapter_progress")
data class ChapterProgressEntity(
    @PrimaryKey val chapterId: String,
    val solvedCount: Int,
    val stars: Int,
    val completedAt: Long?,
)

fun ChapterProgressEntity.toModel() = ChapterProgress(
    chapterId = chapterId,
    solvedCount = solvedCount,
    stars = stars,
    completedAt = completedAt,
)
