package com.iilmchat.chessproblem.data.repository

import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.data.local.PuzzleProgressDao
import com.iilmchat.chessproblem.data.local.toEntity
import com.iilmchat.chessproblem.data.local.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultProgressRepository(
    private val dao: PuzzleProgressDao,
) : ProgressRepository {
    override fun observeAll(): Flow<List<PuzzleProgress>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun forPuzzle(puzzleId: String): PuzzleProgress? =
        dao.find(puzzleId)?.toModel()

    override suspend fun save(progress: PuzzleProgress) {
        dao.upsert(progress.toEntity())
    }

    override suspend fun clear() {
        dao.clear()
    }
}
