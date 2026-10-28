package com.iilmchat.chessproblem.core.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChesslibRulesTest {
    private val rules = ChesslibRules()

    @Test
    fun `starting position has twenty legal moves`() {
        val board = rules.load("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(20, rules.legalMoves(board).size)
    }

    @Test
    fun `back rank mate is recognized and notated`() {
        val board = rules.load("6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1")
        val result = rules.move(board, Move(Square.fromAlgebraic("e1"), Square.fromAlgebraic("e8")))

        assertNotNull(result)
        assertEquals("Re8#", result.san)
        assertTrue(result.isCheckmate)
        assertTrue(result.board.checkmate)
    }

    @Test
    fun `promotion is applied with a queen`() {
        val board = rules.load("6k1/P7/8/8/8/8/8/6K1 w - - 0 1")
        val result = rules.move(
            board,
            Move(
                from = Square.fromAlgebraic("a7"),
                to = Square.fromAlgebraic("a8"),
                promotion = PieceType.QUEEN,
            ),
        )

        assertNotNull(result)
        assertEquals(Piece(PieceType.QUEEN, Side.WHITE), result.board.pieceAt(Square.fromAlgebraic("a8")))
        assertTrue(result.san.startsWith("a8=Q"))
        assertTrue(result.isCheck)
    }

    @Test
    fun `knight fork line produces san`() {
        var board = rules.load("2q3k1/5ppp/8/5N2/8/8/5PPP/6K1 w - - 0 1")

        val first = rules.move(board, Move(Square.fromAlgebraic("f5"), Square.fromAlgebraic("e7")))
        assertNotNull(first)
        assertEquals("Ne7+", first.san)
        assertTrue(first.isCheck)
        board = first.board

        val reply = rules.move(board, Move(Square.fromAlgebraic("g8"), Square.fromAlgebraic("h8")))
        assertNotNull(reply)
        assertEquals("Kh8", reply.san)
        board = reply.board

        val capture = rules.move(board, Move(Square.fromAlgebraic("e7"), Square.fromAlgebraic("c8")))
        assertNotNull(capture)
        assertEquals("Nxc8", capture.san)
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), capture.board.pieceAt(Square.fromAlgebraic("c8")))
    }

    @Test
    fun `mate in two line produces san`() {
        var board = rules.load("2k5/5p2/K5pp/8/8/8/1Q6/1R6 w - - 0 1")

        val first = rules.move(board, Move(Square.fromAlgebraic("b1"), Square.fromAlgebraic("d1")))
        assertNotNull(first)
        assertEquals("Rd1", first.san)
        board = first.board

        val reply = rules.move(board, Move(Square.fromAlgebraic("c8"), Square.fromAlgebraic("c7")))
        assertNotNull(reply)
        assertEquals("Kc7", reply.san)
        board = reply.board

        val mate = rules.move(board, Move(Square.fromAlgebraic("b2"), Square.fromAlgebraic("b7")))
        assertNotNull(mate)
        assertEquals("Qb7#", mate.san)
        assertTrue(mate.isCheckmate)
    }

    @Test
    fun `illegal move is rejected`() {
        val board = rules.load("6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1")
        assertNull(rules.move(board, Move(Square.fromAlgebraic("e1"), Square.fromAlgebraic("f2"))))
    }
}
