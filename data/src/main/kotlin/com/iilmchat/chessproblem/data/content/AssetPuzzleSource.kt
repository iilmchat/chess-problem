package com.iilmchat.chessproblem.data.content

import android.content.res.AssetManager
import com.iilmchat.chessproblem.core.model.Puzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetPuzzleSource(
    private val reader: PuzzlePackReader,
) {
    constructor(assets: AssetManager) : this(AssetPuzzleReader(assets))

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    suspend fun load(): List<Puzzle> = withContext(Dispatchers.IO) {
        reader.names().flatMap { name ->
            val pack = json.decodeFromString<PuzzlePackJson>(reader.read(name))
            pack.toDomain()
        }
    }

    companion object {
        const val CONTENT_DIR = "content"
    }
}
