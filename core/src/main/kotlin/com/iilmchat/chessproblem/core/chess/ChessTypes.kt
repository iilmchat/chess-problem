package com.iilmchat.chessproblem.core.chess

import kotlin.math.abs

enum class Side {
    WHITE,
    BLACK;

    fun opposite(): Side = if (this == WHITE) BLACK else WHITE
}

enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING;

    val letter: String
        get() = when (this) {
            PAWN -> ""
            KNIGHT -> "N"
            BISHOP -> "B"
            ROOK -> "R"
            QUEEN -> "Q"
            KING -> "K"
        }
}

data class Piece(
    val type: PieceType,
    val side: Side,
)

data class Square(
    val file: Int,
    val rank: Int,
) {
    init {
        require(file in 0..7) { "file must be in 0..7, was $file" }
        require(rank in 0..7) { "rank must be in 0..7, was $rank" }
    }

    val algebraic: String
        get() = "${('a'.code + file).toChar()}${rank + 1}"

    override fun toString(): String = algebraic

    companion object {
        fun fromAlgebraic(value: String): Square {
            require(value.length == 2) { "Square must be two characters: $value" }
            val file = value[0].lowercaseChar() - 'a'
            val rank = value[1].digitToInt() - 1
            return Square(file, rank)
        }

        fun all(): List<Square> = buildList(64) {
            for (rank in 0..7) {
                for (file in 0..7) {
                    add(Square(file, rank))
                }
            }
        }
    }
}

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
) {
    val uci: String
        get() = buildString {
            append(from.algebraic)
            append(to.algebraic)
            promotion?.let { append(it.letter.lowercase()) }
        }

    fun isCastle(piece: Piece?): Boolean =
        piece?.type == PieceType.KING && abs(to.file - from.file) == 2

    override fun toString(): String = uci
}

data class BoardSnapshot(
    val fen: String,
    val pieces: Map<Square, Piece>,
    val sideToMove: Side,
    val lastMove: Move? = null,
    val inCheck: Boolean = false,
    val checkmate: Boolean = false,
    val stalemate: Boolean = false,
    val draw: Boolean = false,
) {
    fun pieceAt(square: Square): Piece? = pieces[square]

    companion object {
        fun fromFen(fen: String): BoardSnapshot {
            val fields = fen.trim().split(Regex("\\s+"))
            require(fields.isNotEmpty()) { "FEN is empty" }

            val pieces = mutableMapOf<Square, Piece>()
            var rank = 7
            var file = 0

            for (symbol in fields[0]) {
                when {
                    symbol == '/' -> {
                        require(file == 8) { "FEN rank has $file files, expected 8" }
                        rank -= 1
                        file = 0
                    }

                    symbol.isDigit() -> file += symbol.digitToInt()
                    else -> {
                        val piece = pieceFromFen(symbol)
                            ?: error("Unknown FEN piece: $symbol")
                        require(file in 0..7) { "FEN rank overflows: $file" }
                        pieces[Square(file, rank)] = piece
                        file += 1
                    }
                }
            }

            require(rank == 0) { "FEN must contain eight ranks" }
            require(file == 8) { "FEN last rank has $file files, expected 8" }

            val sideToMove = if (fields.getOrNull(1) == "b") Side.BLACK else Side.WHITE
            return BoardSnapshot(fen = fen, pieces = pieces, sideToMove = sideToMove)
        }

        private fun pieceFromFen(symbol: Char): Piece? {
            val side = if (symbol.isUpperCase()) Side.WHITE else Side.BLACK
            val type = when (symbol.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, side)
        }
    }
}

data class MoveResult(
    val board: BoardSnapshot,
    val san: String,
    val isCheck: Boolean,
    val isCheckmate: Boolean,
    val isStalemate: Boolean,
    val isDraw: Boolean,
)

interface ChessRules {
    fun load(fen: String): BoardSnapshot

    fun legalMoves(board: BoardSnapshot, from: Square): List<Move>

    fun legalMoves(board: BoardSnapshot): List<Move>

    fun move(board: BoardSnapshot, move: Move): MoveResult?

    fun toSan(board: BoardSnapshot, move: Move): String

    fun isCheckmate(board: BoardSnapshot): Boolean

    fun isStalemate(board: BoardSnapshot): Boolean

    fun isDraw(board: BoardSnapshot): Boolean
}
