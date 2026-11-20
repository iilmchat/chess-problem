package com.iilmchat.chessproblem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PuzzleProgressEntity::class, ChapterProgressEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun puzzleProgressDao(): PuzzleProgressDao

    abstract fun chapterProgressDao(): ChapterProgressDao

    companion object {
        /**
         * KI-018: версия 1 не различала демо-прогресс v0.2.0 и production ID.
         * Пустая версия у старых строк — явный признак несовместимости; такие
         * записи удаляются репозиторием при инициализации.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE puzzle_progress ADD COLUMN chapterId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE puzzle_progress ADD COLUMN contentVersion TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chapter_progress` (
                        `chapterId` TEXT NOT NULL,
                        `solvedCount` INTEGER NOT NULL,
                        `stars` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        PRIMARY KEY(`chapterId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun create(context: Context): ChessDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ChessDatabase::class.java,
                "chess-problem.db",
            ).addMigrations(MIGRATION_1_2)
                .build()
    }
}
