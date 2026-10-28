package com.iilmchat.chessproblem.data.content

import android.content.res.AssetManager
import com.iilmchat.chessproblem.core.model.Puzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetPuzzleSource(
    private val assets: AssetManager,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    suspend fun load(): List<Puzzle> = withContext(Dispatchers.IO) {
        val names = assets.list(CONTENT_DIR)
            ?.filter { it.endsWith(".json", ignoreCase = true) }
            ?.sorted()
            .orEmpty()

        names.flatMap { name ->
            val text = assets.open("$CONTENT_DIR/$name")
                .bufferedReader()
                .use { it.readText() }
            val pack = json.decodeFromString<PuzzlePackJson>(text)
            pack.toDomain()
        }
    }

    companion object {
        const val CONTENT_DIR = "content"
    }
}
