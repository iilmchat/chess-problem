package com.iilmchat.chessproblem.data.content

import android.content.res.AssetManager
import com.iilmchat.chessproblem.core.repository.ContentVersionProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PuzzlePackHeader(
    val contentVersion: String,
)

/**
 * KI-018: версия берётся из заголовков самих паков, а не хардкодится в коде —
 * при обновлении контента прогресс автоматически считается несовместимым.
 */
class AssetContentVersionProvider(
    private val reader: PuzzlePackReader,
) : ContentVersionProvider {
    constructor(assets: AssetManager) : this(AssetPuzzleReader(assets))

    private val json = Json { ignoreUnknownKeys = true }
    private val version: String by lazy {
        val versions = reader.names()
            .map { json.decodeFromString<PuzzlePackHeader>(reader.read(it)).contentVersion }
            .toSet()
        require(versions.isNotEmpty()) { "No puzzle packs found" }
        require(versions.size == 1) { "Puzzle packs have different contentVersion: $versions" }
        versions.single()
    }

    override fun current(): String = version
}
