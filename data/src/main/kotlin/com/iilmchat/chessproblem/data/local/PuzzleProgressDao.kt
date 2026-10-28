package com.iilmchat.chessproblem.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleProgressDao {
    @Query("SELECT * FROM puzzle_progress ORDER BY lastPlayedAt DESC")
    fun observeAll(): Flow<List<PuzzleProgressEntity>>

    @Query("SELECT * FROM puzzle_progress WHERE puzzleId = :puzzleId LIMIT 1")
    suspend fun find(puzzleId: String): PuzzleProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PuzzleProgressEntity)

    @Query("DELETE FROM puzzle_progress")
    suspend fun clear()
}
