package com.iilmchat.chessproblem.data.local

import androidx.room.Room
import com.iilmchat.chessproblem.core.repository.ContentVersionProvider
import com.iilmchat.chessproblem.data.repository.DefaultProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private class FixedVersionProvider(
    private val version: String,
) : ContentVersionProvider {
    override fun current(): String = version
}

@RunWith(RobolectricTestRunner::class)
class ProgressDaoTest {
    private val database: ChessDatabase by lazy {
        Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ChessDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @Test
    fun `сохранение результата задачи сохраняет версию контента и главу`() = runTest {
        val dao = database.puzzleProgressDao()
        dao.upsert(
            PuzzleProgressEntity(
                puzzleId = "nov-001",
                solved = true,
                reviewed = false,
                stars = 3,
                bestTimeMs = 12_000L,
                attempts = 1,
                hintsUsed = 0,
                lastPlayedAt = 1_000L,
                chapterId = "mate-in-1",
                contentVersion = "2026.10.4",
            ),
        )

        val saved = dao.find("nov-001")

        assertNotNull(saved)
        assertEquals("mate-in-1", saved.chapterId)
        assertEquals("2026.10.4", saved.contentVersion)
        assertEquals(3, saved.stars)
        assertEquals(12_000L, saved.bestTimeMs)
    }

    @Test
    fun `пересчёт главы суммирует звёзды решённых задач актуальной версии`() = runTest {
        val dao = database.puzzleProgressDao()
        val chapters = database.chapterProgressDao()
        listOf(
            progress("nov-001", stars = 3, lastPlayedAt = 1_000L),
            progress("nov-002", stars = 2, lastPlayedAt = 2_000L),
            progress("nov-003", stars = 3, solved = false, lastPlayedAt = 3_000L),
            progress("nov-004", stars = 3, contentVersion = "2026.10.2", lastPlayedAt = 4_000L),
        ).forEach { dao.upsert(it) }

        chapters.clear()
        chapters.recompute("2026.10.4")

        val chapter = chapters.find("mate-in-1")
        assertNotNull(chapter)
        assertEquals(2, chapter.solvedCount)
        assertEquals(5, chapter.stars)
        assertEquals(2_000L, chapter.completedAt)
    }

    @Test
    fun `удаление несовместимых записей оставляет только актуальную версию`() = runTest {
        val dao = database.puzzleProgressDao()
        listOf(
            progress("nov-001", contentVersion = "2026.10.4"),
            progress("nov-002", contentVersion = ""),
            progress("nov-003", contentVersion = "2026.10.2"),
        ).forEach { dao.upsert(it) }

        dao.deleteIncompatible("2026.10.4")

        assertEquals(listOf("nov-001"), dao.observeAll().first().map { it.puzzleId })
    }

    @Test
    fun `репозиторий при инициализации очищает демо-прогресс и пересчитывает главы`() = runTest {
        val dao = database.puzzleProgressDao()
        val chapters = database.chapterProgressDao()
        listOf(
            progress("nov-001", stars = 3, lastPlayedAt = 1_000L),
            progress("pro-001", stars = 3, lastPlayedAt = 2_000L, contentVersion = ""),
        ).forEach { dao.upsert(it) }
        val repository = DefaultProgressRepository(
            dao = dao,
            chapterDao = chapters,
            contentVersionProvider = FixedVersionProvider("2026.10.4"),
        )

        val first = repository.forPuzzle("nov-001")

        assertNotNull(first)
        assertEquals("2026.10.4", first.contentVersion)
        assertEquals(listOf("nov-001"), repository.observeAll().first().map { it.puzzleId })
        assertEquals(
            listOf("mate-in-1"),
            repository.observeChapters().first().map { it.chapterId },
        )
        assertEquals(3, repository.observeChapters().first().single().stars)
    }

    @Test
    fun `запись через репозиторий штампует текущую версию контента`() = runTest {
        val repository = DefaultProgressRepository(
            dao = database.puzzleProgressDao(),
            chapterDao = database.chapterProgressDao(),
            contentVersionProvider = FixedVersionProvider("2026.11.1"),
        )

        repository.save(
            com.iilmchat.chessproblem.core.model.PuzzleProgress(
                puzzleId = "grm-001",
                chapterId = "final-exam",
                solved = true,
                stars = 2,
                lastPlayedAt = 5_000L,
            ),
        )

        val saved = repository.forPuzzle("grm-001")
        assertNotNull(saved)
        assertEquals("2026.11.1", saved.contentVersion)
        assertEquals(2, repository.observeChapters().first().single().stars)
    }

    private fun progress(
        puzzleId: String,
        stars: Int = 3,
        solved: Boolean = true,
        lastPlayedAt: Long = 1_000L,
        contentVersion: String = "2026.10.4",
    ) = PuzzleProgressEntity(
        puzzleId = puzzleId,
        solved = solved,
        reviewed = false,
        stars = stars,
        bestTimeMs = null,
        attempts = 0,
        hintsUsed = 0,
        lastPlayedAt = lastPlayedAt,
        chapterId = "mate-in-1",
        contentVersion = contentVersion,
    )
}
