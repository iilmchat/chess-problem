package com.iilmchat.chessproblem.data.repository

import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.data.content.AssetPuzzleSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultPuzzleRepository(
    private val source: AssetPuzzleSource,
) : PuzzleRepository {
    private val mutex = Mutex()
    private var cache: List<Puzzle>? = null

    override suspend fun all(): List<Puzzle> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: source.load()
                .sortedWith(compareBy<Puzzle> { it.level.ordinal }.thenBy { it.rating })
                .also { cache = it }
        }
    }

    override suspend fun byId(id: String): Puzzle? = all().firstOrNull { it.id == id }

    override suspend fun byLevel(level: Level): List<Puzzle> =
        all().filter { it.level == level }
}
