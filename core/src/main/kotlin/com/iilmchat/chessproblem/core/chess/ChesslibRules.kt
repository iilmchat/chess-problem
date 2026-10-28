package com.iilmchat.chessproblem.core.chess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece as LibPiece
import com.github.bhlangonijr.chesslib.PieceType as LibPieceType
import com.github.bhlangonijr.chesslib.Side as LibSide
import com.github.bhlangonijr.chesslib.Square as LibSquare
import com.github.bhlangonijr.chesslib.move.Move as LibMove

class ChesslibRules : ChessRules {

    override fun load(fen: String): BoardSnapshot {
        val board = boardFrom(fen)
        return BoardSnapshot.fromFen(fen).withFlags(board)
    }

    override fun legalMoves(board: BoardSnapshot, from: Square): List<Move> =
        boardFrom(board.fen)
            .legalMoves()
            .asSequence()
            .filter { it.from.toCore() == from }
            .map { it.toCore() }
            .toList()

    override fun legalMoves(board: BoardSnapshot): List<Move> =
        boardFrom(board.fen)
            .legalMoves()
            .map { it.toCore() }

    override fun move(board: BoardSnapshot, move: Move): MoveResult? {
        val board = boardFrom(board.fen)
        val legalMove = board.legalMoves()
            .firstOrNull { it.matches(move) }
            ?: return null

        val san = SanGenerator.of(board, legalMove)
        if (!board.doMove(legalMove, true)) return null

        return MoveResult(
            board = BoardSnapshot.fromFen(board.fen)
                .withFlags(board)
                .copy(lastMove = move),
            san = san,
            isCheck = board.isKingAttacked(),
            isCheckmate = board.isMated(),
            isStalemate = board.isStaleMate(),
            isDraw = board.isDraw(),
        )
    }

    override fun toSan(board: BoardSnapshot, move: Move): String {
        val libBoard = boardFrom(board.fen)
        val legalMove = libBoard.legalMoves()
            .firstOrNull { it.matches(move) }
            ?: return move.uci
        return SanGenerator.of(libBoard, legalMove)
    }

    override fun isCheckmate(board: BoardSnapshot): Boolean =
        boardFrom(board.fen).isMated()

    override fun isStalemate(board: BoardSnapshot): Boolean =
        boardFrom(board.fen).isStaleMate()

    override fun isDraw(board: BoardSnapshot): Boolean =
        boardFrom(board.fen).isDraw()

    private fun boardFrom(fen: String): Board = Board().apply {
        loadFromFen(fen)
    }

    private fun BoardSnapshot.withFlags(board: Board): BoardSnapshot = copy(
        inCheck = board.isKingAttacked(),
        checkmate = board.isMated(),
        stalemate = board.isStaleMate(),
        draw = board.isDraw(),
    )

    private fun LibMove.toCore(): Move = Move(
        from = from.toCore(),
        to = to.toCore(),
        promotion = promotion.toCoreType(),
    )

    private fun LibMove.matches(move: Move): Boolean =
        from.toCore() == move.from &&
            to.toCore() == move.to &&
            promotion.toCoreType() == move.promotion

    private fun LibSquare.toCore(): Square = Square.fromAlgebraic(name.lowercase())

    private fun LibPiece.toCoreType(): PieceType? {
        if (this == LibPiece.NONE) return null
        return when (pieceType) {
            LibPieceType.PAWN -> PieceType.PAWN
            LibPieceType.KNIGHT -> PieceType.KNIGHT
            LibPieceType.BISHOP -> PieceType.BISHOP
            LibPieceType.ROOK -> PieceType.ROOK
            LibPieceType.QUEEN -> PieceType.QUEEN
            LibPieceType.KING -> PieceType.KING
            else -> error("Unsupported piece type: $pieceType")
        }
    }
}

internal object SanGenerator {
    fun of(board: Board, move: LibMove): String {
        val moving = board.getPiece(move.from)
        val target = board.getPiece(move.to)
        val enPassant = board.enPassantTarget
        val isEnPassant = moving.pieceType == LibPieceType.PAWN &&
            enPassant != LibSquare.NONE &&
            move.to == enPassant
        val isCapture = target != LibPiece.NONE || isEnPassant
        val isCastle = moving.pieceType == LibPieceType.KING &&
            kotlin.math.abs(move.from.name[0].code - move.to.name[0].code) == 2

        if (isCastle) {
            val suffix = suffixAfter(board, move)
            val castle = if (move.to.name[0] > move.from.name[0]) "O-O" else "O-O-O"
            return castle + suffix
        }

        val builder = StringBuilder()
        if (moving.pieceType != LibPieceType.PAWN) {
            builder.append(pieceLetter(moving.pieceType))
            appendDisambiguation(board, move, moving, builder)
        } else if (isCapture) {
            builder.append(move.from.name[0].lowercaseChar())
        }

        if (isCapture) builder.append('x')
        builder.append(move.to.name.lowercase())

        if (move.promotion != LibPiece.NONE) {
            builder.append('=')
            builder.append(pieceLetter(move.promotion.pieceType))
        }

        builder.append(suffixAfter(board, move))
        return builder.toString()
    }

    private fun appendDisambiguation(
        board: Board,
        move: LibMove,
        moving: LibPiece,
        builder: StringBuilder,
    ) {
        val alternatives = board.legalMoves().filter {
            it.to == move.to &&
                it.from != move.from &&
                board.getPiece(it.from).pieceType == moving.pieceType
        }
        if (alternatives.isEmpty()) return

        val sameFile = alternatives.any { it.from.name[0] == move.from.name[0] }
        val sameRank = alternatives.any { it.from.name[1] == move.from.name[1] }
        when {
            !sameFile -> builder.append(move.from.name[0].lowercaseChar())
            !sameRank -> builder.append(move.from.name[1])
            else -> {
                builder.append(move.from.name[0].lowercaseChar())
                builder.append(move.from.name[1])
            }
        }
    }

    private fun suffixAfter(board: Board, move: LibMove): String {
        val copy = Board().apply { loadFromFen(board.fen) }
        if (!copy.doMove(move, true)) return ""
        return when {
            copy.isMated() -> "#"
            copy.isKingAttacked() -> "+"
            else -> ""
        }
    }

    private fun pieceLetter(type: LibPieceType): String = when (type) {
        LibPieceType.PAWN -> ""
        LibPieceType.KNIGHT -> "N"
        LibPieceType.BISHOP -> "B"
        LibPieceType.ROOK -> "R"
        LibPieceType.QUEEN -> "Q"
        LibPieceType.KING -> "K"
        else -> ""
    }
}
