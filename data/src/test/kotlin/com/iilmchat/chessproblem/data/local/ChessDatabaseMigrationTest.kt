package com.iilmchat.chessproblem.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChessDatabaseMigrationTest {
    private val dbName = "migration-test-${System.nanoTime()}.db"

    @Test
    fun `миграция с версии 1 добавляет версию контента и таблицу глав`() {
        val legacy = openLegacyDatabase()

        legacy.writableDatabase.use { db ->
            db.execSQL(
                """
                INSERT INTO puzzle_progress
                    (puzzleId, solved, reviewed, stars, bestTimeMs, attempts, hintsUsed, lastPlayedAt)
                VALUES ('nov-001', 1, 0, 3, NULL, 0, 0, 1000)
                """.trimIndent(),
            )

            ChessDatabase.MIGRATION_1_2.migrate(db)

            val columns = db.query("PRAGMA table_info(puzzle_progress)").use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }
            assertTrue("chapterId" in columns)
            assertTrue("contentVersion" in columns)

            db.query("SELECT chapterId, contentVersion FROM puzzle_progress").use { cursor ->
                assertTrue(cursor.moveToNext())
                assertEquals("", cursor.getString(0))
                assertEquals("", cursor.getString(1))
            }

            val tables = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'chapter_progress'",
            ).use { cursor -> cursor.count }
            assertEquals(1, tables)
        }
        legacy.close()
    }

    @Test
    fun `база после миграции открывается Room без потери демо-записи`() = runTest {
        val legacy = openLegacyDatabase()
        legacy.writableDatabase.use { db ->
            db.execSQL(
                """
                INSERT INTO puzzle_progress
                    (puzzleId, solved, reviewed, stars, bestTimeMs, attempts, hintsUsed, lastPlayedAt)
                VALUES ('nov-001', 1, 0, 2, NULL, 0, 0, 1000)
                """.trimIndent(),
            )
        }
        legacy.close()

        // Room сам применяет миграцию при открытии базы версии 1.
        val database = Room.databaseBuilder(
            RuntimeEnvironment.getApplication(),
            ChessDatabase::class.java,
            dbName,
        ).addMigrations(ChessDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val progress = database.puzzleProgressDao().observeAll().first().single()
        assertEquals("nov-001", progress.puzzleId)
        assertEquals(2, progress.stars)
        assertEquals("", progress.contentVersion)
        assertTrue(database.chapterProgressDao().observeAll().first().isEmpty())
        database.close()
    }

    private fun openLegacyDatabase(): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE puzzle_progress (
                                    puzzleId TEXT NOT NULL,
                                    solved INTEGER NOT NULL,
                                    reviewed INTEGER NOT NULL,
                                    stars INTEGER NOT NULL,
                                    bestTimeMs INTEGER,
                                    attempts INTEGER NOT NULL,
                                    hintsUsed INTEGER NOT NULL,
                                    lastPlayedAt INTEGER NOT NULL,
                                    PRIMARY KEY(puzzleId)
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        )
}
