package com.iilmchat.chessproblem.ui

import com.iilmchat.chessproblem.core.chess.BoardSnapshot
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.LocalizedText
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleHint
import com.iilmchat.chessproblem.core.model.PuzzleTask
import com.iilmchat.chessproblem.core.model.TaskType

internal const val TEST_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

internal fun testBoard(): BoardSnapshot = BoardSnapshot.fromFen(TEST_FEN)

internal fun testPuzzle(): Puzzle = Puzzle(
    id = "test-001",
    level = Level.NOVICE,
    chapter = "mate-in-1",
    fen = TEST_FEN,
    setupMove = null,
    solution = listOf("e2e4", "e7e5"),
    task = PuzzleTask(type = TaskType.MATE, plies = 1),
    rating = 1200,
    themes = listOf("fork"),
    timeLimitSec = 180,
    title = LocalizedText(ru = "Тихий ход", en = "Quiet move"),
    prompt = LocalizedText(ru = "Найдите лучший ход", en = "Find the best move"),
    hints = listOf(
        PuzzleHint(
            tier = HintTier.IDEA,
            text = LocalizedText(ru = "Ищите активное продолжение", en = "Look for an active move"),
        ),
        PuzzleHint(
            tier = HintTier.PIECE,
            text = LocalizedText(ru = "Обратите внимание на пешку", en = "Watch the pawn"),
        ),
        PuzzleHint(
            tier = HintTier.MOVE,
            text = LocalizedText(ru = "Первый ход — e4", en = "The first move is e4"),
        ),
    ),
    explanation = LocalizedText(
        ru = "Ладья удерживает линию, ферзь завершает атаку.",
        en = "The rook holds the line and the queen finishes the attack.",
    ),
)
