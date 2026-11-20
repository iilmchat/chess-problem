package com.iilmchat.chessproblem.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterProgressDao {
    @Query("SELECT * FROM chapter_progress ORDER BY chapterId")
    fun observeAll(): Flow<List<ChapterProgressEntity>>

    @Query("SELECT * FROM chapter_progress WHERE chapterId = :chapterId LIMIT 1")
    suspend fun find(chapterId: String): ChapterProgressEntity?

    @Query("DELETE FROM chapter_progress")
    suspend fun clear()

    /**
     * Денормализованный кэш пересчитывается целиком из puzzle_progress:
     * это дешевле и надёжнее инкрементальных обновлений на 360 задачах.
     */
    @Query(
        """
        INSERT INTO chapter_progress (chapterId, solvedCount, stars, completedAt)
        SELECT chapterId, COUNT(*), SUM(stars), MAX(lastPlayedAt)
        FROM puzzle_progress
        WHERE solved = 1 AND contentVersion = :contentVersion
        GROUP BY chapterId
        """,
    )
    suspend fun recompute(contentVersion: String)
}
