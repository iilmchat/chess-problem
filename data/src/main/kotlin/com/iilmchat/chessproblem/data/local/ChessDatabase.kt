package com.iilmchat.chessproblem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PuzzleProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun puzzleProgressDao(): PuzzleProgressDao

    companion object {
        fun create(context: Context): ChessDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ChessDatabase::class.java,
                "chess-problem.db",
            ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
