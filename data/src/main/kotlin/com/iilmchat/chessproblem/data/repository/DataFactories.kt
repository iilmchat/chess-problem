package com.iilmchat.chessproblem.data.repository

import android.content.Context
import com.iilmchat.chessproblem.core.repository.ContentVersionProvider
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.data.content.AssetContentVersionProvider
import com.iilmchat.chessproblem.data.content.AssetPuzzleSource
import com.iilmchat.chessproblem.data.local.ChessDatabase

fun createPuzzleRepository(context: Context): PuzzleRepository =
    DefaultPuzzleRepository(AssetPuzzleSource(context.assets))

fun createContentVersionProvider(context: Context): ContentVersionProvider =
    AssetContentVersionProvider(context.assets)

fun createProgressRepository(
    context: Context,
    contentVersionProvider: ContentVersionProvider,
): ProgressRepository {
    val database = ChessDatabase.create(context)
    return DefaultProgressRepository(
        dao = database.puzzleProgressDao(),
        chapterDao = database.chapterProgressDao(),
        contentVersionProvider = contentVersionProvider,
    )
}
