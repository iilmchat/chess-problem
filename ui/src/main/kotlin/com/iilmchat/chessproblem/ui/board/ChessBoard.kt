package com.iilmchat.chessproblem.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iilmchat.chessproblem.core.chess.BoardSnapshot
import com.iilmchat.chessproblem.core.chess.Move
import com.iilmchat.chessproblem.core.chess.Piece
import com.iilmchat.chessproblem.core.chess.PieceType
import com.iilmchat.chessproblem.core.chess.Side
import com.iilmchat.chessproblem.core.chess.Square
import com.iilmchat.chessproblem.core.model.BoardTheme
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.theme.ManropeFamily
import kotlin.math.abs

@Composable
fun ChessBoard(
    board: BoardSnapshot,
    selected: Square?,
    legalTargets: Set<Square>,
    lastMove: Move?,
    flipped: Boolean,
    hintPiece: Square?,
    hintMove: Move?,
    coordinates: Boolean,
    boardTheme: BoardTheme,
    enabled: Boolean,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(boardTheme) { boardPalette(boardTheme) }
    val hintColor = MaterialTheme.colorScheme.secondary
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val currentOnSquareClick by rememberUpdatedState(onSquareClick)
    val boardDescription = stringResource(R.string.board_content_description)
    val labelSize = with(density) { 9.sp.toPx() }
    val labelStyle = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
    )

    val checkSquare = remember(board) {
        if (!board.inCheck) {
            null
        } else {
            board.pieces.entries.firstOrNull {
                it.value.type == PieceType.KING && it.value.side == board.sideToMove
            }?.key
        }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = boardDescription }
            .pointerInput(board.fen, flipped, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val square = offset.toSquare(size.width.toFloat(), size.height.toFloat(), flipped)
                    if (square != null) currentOnSquareClick(square)
                }
            },
    ) {
        val square = size.minDimension / 8f

        for (rank in 0..7) {
            for (file in 0..7) {
                val screenFile = if (flipped) 7 - file else file
                val screenRank = if (flipped) 7 - rank else rank
                val x = screenFile * square
                val y = (7 - screenRank) * square
                val light = (file + rank) % 2 == 1
                drawRect(
                    color = if (light) palette.light else palette.dark,
                    topLeft = Offset(x, y),
                    size = Size(square, square),
                )
            }
        }

        lastMove?.let { move ->
            drawSquareOverlay(move.from, square, flipped, palette.lastMove)
            drawSquareOverlay(move.to, square, flipped, palette.lastMove)
        }

        selected?.let { squareAt ->
            drawSquareOverlay(squareAt, square, flipped, palette.selected)
        }

        checkSquare?.let { king ->
            drawSquareOverlay(king, square, flipped, palette.check)
        }

        legalTargets.forEach { target ->
            val (x, y) = target.screenPosition(square, flipped)
            val occupied = board.pieceAt(target) != null
            if (occupied) {
                drawCircle(
                    color = palette.legalCapture,
                    radius = square * 0.41f,
                    center = Offset(x + square / 2f, y + square / 2f),
                    style = Stroke(width = square * 0.055f),
                )
            } else {
                drawCircle(
                    color = palette.legal,
                    radius = square * 0.13f,
                    center = Offset(x + square / 2f, y + square / 2f),
                )
            }
        }

        board.pieces.forEach { (squareAt, piece) ->
            val (x, y) = squareAt.screenPosition(square, flipped)
            drawPiece(
                piece = piece,
                left = x + square * 0.035f,
                top = y + square * 0.02f,
                size = square * 0.93f,
                palette = palette,
                lift = selected == squareAt,
            )
        }

        hintPiece?.let { hint ->
            val (x, y) = hint.screenPosition(square, flipped)
            drawCircle(
                color = hintColor.copy(alpha = 0.95f),
                radius = square * 0.45f,
                center = Offset(x + square / 2f, y + square / 2f),
                style = Stroke(width = square * 0.055f),
            )
        }

        hintMove?.let { move ->
            val (fromX, fromY) = move.from.screenPosition(square, flipped)
            val (toX, toY) = move.to.screenPosition(square, flipped)
            val start = Offset(fromX + square / 2f, fromY + square / 2f)
            val end = Offset(toX + square / 2f, toY + square / 2f)
            drawHintArrow(start, end, square, hintColor)
        }

        if (coordinates) {
            drawCoordinates(
                square = square,
                flipped = flipped,
                palette = palette,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                labelSize = labelSize,
            )
        }
    }
}

private fun Offset.toSquare(width: Float, height: Float, flipped: Boolean): Square? {
    if (width <= 0f || height <= 0f) return null
    val squareSize = minOf(width, height) / 8f
    val fileIndex = (x / squareSize).toInt()
    val rankIndex = (y / squareSize).toInt()
    if (fileIndex !in 0..7 || rankIndex !in 0..7) return null
    val file = if (flipped) 7 - fileIndex else fileIndex
    val rank = if (flipped) rankIndex else 7 - rankIndex
    return Square(file, rank)
}

private fun Square.screenPosition(squareSize: Float, flipped: Boolean): Pair<Float, Float> {
    val screenFile = if (flipped) 7 - file else file
    val screenRank = if (flipped) 7 - rank else rank
    return (screenFile * squareSize) to ((7 - screenRank) * squareSize)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSquareOverlay(
    square: Square,
    squareSize: Float,
    flipped: Boolean,
    color: Color,
) {
    val (x, y) = square.screenPosition(squareSize, flipped)
    drawRoundRect(
        color = color,
        topLeft = Offset(x + 1f, y + 1f),
        size = Size(squareSize - 2f, squareSize - 2f),
        cornerRadius = CornerRadius(squareSize * 0.06f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHintArrow(
    start: Offset,
    end: Offset,
    squareSize: Float,
    color: Color,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    if (length < 1f) return
    val ux = dx / length
    val uy = dy / length
    val inset = squareSize * 0.16f
    val actualStart = Offset(start.x + ux * inset, start.y + uy * inset)
    val actualEnd = Offset(end.x - ux * inset, end.y - uy * inset)

    drawLine(
        color = color,
        start = actualStart,
        end = actualEnd,
        strokeWidth = squareSize * 0.075f,
        cap = StrokeCap.Round,
    )

    val head = squareSize * 0.2f
    val left = Offset(
        actualEnd.x - ux * head - uy * head * 0.62f,
        actualEnd.y - uy * head + ux * head * 0.62f,
    )
    val right = Offset(
        actualEnd.x - ux * head + uy * head * 0.62f,
        actualEnd.y - uy * head - ux * head * 0.62f,
    )
    drawLine(color, actualEnd, left, strokeWidth = squareSize * 0.075f, cap = StrokeCap.Round)
    drawLine(color, actualEnd, right, strokeWidth = squareSize * 0.075f, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoordinates(
    square: Float,
    flipped: Boolean,
    palette: BoardPalette,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    labelStyle: TextStyle,
    labelSize: Float,
) {
    for (screenFile in 0..7) {
        val file = if (flipped) 7 - screenFile else screenFile
        val rank = if (flipped) 0 else 7
        val light = (file + rank) % 2 == 1
        val color = if (light) palette.coordinateOnLight else palette.coordinateOnDark
        drawText(
            textMeasurer = textMeasurer,
            text = ('a' + file).toString(),
            topLeft = Offset(
                screenFile * square + square * 0.05f,
                7 * square + square - labelSize * 1.5f,
            ),
            style = labelStyle.copy(color = color),
        )
    }

    for (screenRank in 0..7) {
        val rank = if (flipped) screenRank else 7 - screenRank
        val file = if (flipped) 7 else 0
        val light = (file + rank) % 2 == 1
        val color = if (light) palette.coordinateOnLight else palette.coordinateOnDark
        drawText(
            textMeasurer = textMeasurer,
            text = (rank + 1).toString(),
            topLeft = Offset(square * 0.05f, screenRank * square + square * 0.05f),
            style = labelStyle.copy(color = color),
        )
    }
}

private fun kingSquare(board: BoardSnapshot, side: Side): Square? =
    board.pieces.entries.firstOrNull {
        it.value.type == PieceType.KING && it.value.side == side
    }?.key

@Suppress("unused")
private fun moveDistance(from: Square, to: Square): Int =
    abs(from.file - to.file) + abs(from.rank - to.rank)

@Suppress("unused")
private fun pieceLabel(piece: Piece): String = "${piece.side.name} ${piece.type.name}"
