package com.iilmchat.chessproblem.data.content

import android.content.res.AssetManager

/** Абстракция над каталогом паков: позволяет тестировать загрузку без Android. */
interface PuzzlePackReader {
    fun names(): List<String>

    fun read(name: String): String
}

class AssetPuzzleReader(
    private val assets: AssetManager,
) : PuzzlePackReader {
    override fun names(): List<String> =
        assets.list(AssetPuzzleSource.CONTENT_DIR)
            ?.filter { it.endsWith(".json", ignoreCase = true) }
            ?.sorted()
            .orEmpty()

    override fun read(name: String): String =
        assets.open("${AssetPuzzleSource.CONTENT_DIR}/$name")
            .bufferedReader()
            .use { it.readText() }
}
