package com.iilmchat.chessproblem.data.repository

import android.content.Context
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.data.content.AssetPuzzleSource
import com.iilmchat.chessproblem.data.local.ChessDatabase

fun createPuzzleRepository(context: Context): PuzzleRepository =
    DefaultPuzzleRepository(AssetPuzzleSource(context.assets))

fun createProgressRepository(context: Context): ProgressRepository =
    DefaultProgressRepository(ChessDatabase.create(context).puzzleProgressDao())
