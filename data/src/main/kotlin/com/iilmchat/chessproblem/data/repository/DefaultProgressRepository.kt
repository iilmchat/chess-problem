package com.iilmchat.chessproblem.data.repository

import com.iilmchat.chessproblem.core.model.ChapterProgress
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.repository.ContentVersionProvider
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.data.local.ChapterProgressDao
import com.iilmchat.chessproblem.data.local.ChapterProgressEntity
import com.iilmchat.chessproblem.data.local.PuzzleProgressDao
import com.iilmchat.chessproblem.data.local.PuzzleProgressEntity
import com.iilmchat.chessproblem.data.local.toEntity
import com.iilmchat.chessproblem.data.local.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultProgressRepository(
    private val dao: PuzzleProgressDao,
    private val chapterDao: ChapterProgressDao,
    private val contentVersionProvider: ContentVersionProvider,
) : ProgressRepository {
    private val initMutex = Mutex()
    private var initialized = false

    override fun observeAll(): Flow<List<PuzzleProgress>> = flow {
        ensureInitialized()
        emitAll(dao.observeAll().map { entities -> entities.map(PuzzleProgressEntity::toModel) })
    }

    override fun observeChapters(): Flow<List<ChapterProgress>> = flow {
        ensureInitialized()
        emitAll(chapterDao.observeAll().map { entities -> entities.map(ChapterProgressEntity::toModel) })
    }

    override suspend fun forPuzzle(puzzleId: String): PuzzleProgress? {
        ensureInitialized()
        return dao.find(puzzleId)?.toModel()
    }

    override suspend fun save(progress: PuzzleProgress) {
        val version = ensureInitialized()
        dao.upsert(progress.copy(contentVersion = version).toEntity())
        recomputeChapters(version)
    }

    override suspend fun clear() {
        ensureInitialized()
        dao.clear()
        chapterDao.clear()
    }

    /**
     * KI-018: ленивая инициализация при первом обращении к репозиторию.
     * Несовместимые записи (демо v0.2.0 с пустой версией и прогресс других
     * версий паков) удаляются, после чего кэш глав строится заново.
     */
    private suspend fun ensureInitialized(): String = initMutex.withLock {
        val version = contentVersionProvider.current()
        if (!initialized) {
            dao.deleteIncompatible(version)
            recomputeChapters(version)
            initialized = true
        }
        version
    }

    private suspend fun recomputeChapters(version: String) {
        chapterDao.clear()
        chapterDao.recompute(version)
    }
}
