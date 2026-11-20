package com.iilmchat.chessproblem.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    onMove: (Square, Square) -> Unit = { _, _ -> },
    haptics: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = remember(boardTheme) { boardPalette(boardTheme) }
    val hintColor = MaterialTheme.colorScheme.secondary
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnSquareClick by rememberUpdatedState(onSquareClick)
    val currentOnMove by rememberUpdatedState(onMove)
    val boardDescription = stringResource(R.string.board_content_description)
    val sideToMove = stringResource(
        if (board.sideToMove == Side.WHITE) {
            R.string.board_white_to_move
        } else {
            R.string.board_black_to_move
        },
    )
    val checkDescription = if (board.inCheck) stringResource(R.string.board_in_check) else null
    val selectionDescription = if (selected != null) {
        stringResource(R.string.board_selected, selected.algebraic) + ". " +
            stringResource(R.string.board_legal_moves, legalTargets.size)
    } else {
        null
    }
    val boardSummary = listOfNotNull(
        boardDescription,
        sideToMove,
        checkDescription,
        selectionDescription,
    ).joinToString(". ")
    val labelSize = with(density) { 9.sp.toPx() }
    val labelStyle = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
    )
    var dragState by remember { mutableStateOf<DragState?>(null) }

    val checkSquare = remember(board) {
        if (!board.inCheck) {
            null
        } else {
            board.pieces.entries.firstOrNull {
                it.value.type == PieceType.KING && it.value.side == board.sideToMove
            }?.key
        }
    }

    LaunchedEffect(board.fen, enabled) {
        dragState = null
    }

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(board.fen, flipped, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val square = offset.toSquare(
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            flipped = flipped,
                        )
                        if (square != null) currentOnSquareClick(square)
                    }
                }
                .pointerInput(board.fen, flipped, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val square = offset.toSquare(
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                flipped = flipped,
                            )
                            val piece = square?.let(board::pieceAt)
                            if (square != null && piece != null && piece.side == board.sideToMove) {
                                dragState = DragState(
                                    origin = square,
                                    piece = piece,
                                    position = offset,
                                    target = null,
                                )
                                if (haptics) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            val current = dragState ?: return@detectDragGestures
                            change.consume()
                            dragState = current.copy(
                                position = change.position,
                                target = change.position.toSquare(
                                    width = size.width.toFloat(),
                                    height = size.height.toFloat(),
                                    flipped = flipped,
                                ),
                            )
                        },
                        onDragEnd = {
                            val drag = dragState
                            dragState = null
                            if (drag != null) {
                                val target = drag.position.toSquare(
                                    width = size.width.toFloat(),
                                    height = size.height.toFloat(),
                                    flipped = flipped,
                                )
                                if (target == drag.origin) {
                                    currentOnSquareClick(drag.origin)
                                } else if (target != null) {
                                    currentOnMove(drag.origin, target)
                                }
                            }
                        },
                        onDragCancel = {
                            dragState = null
                        },
                    )
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

            dragState?.target?.let { target ->
                drawSquareOverlay(target, square, flipped, palette.selected.copy(alpha = 0.35f))
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
                if (dragState?.origin == squareAt) return@forEach
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

            dragState?.let { drag ->
                val pieceSize = square * 1.16f
                val left = (drag.position.x - pieceSize / 2f)
                    .coerceIn(0f, size.width - pieceSize)
                val top = (drag.position.y - pieceSize * 0.82f)
                    .coerceIn(0f, size.height - pieceSize)
                drawPiece(
                    piece = drag.piece,
                    left = left,
                    top = top,
                    size = pieceSize,
                    palette = palette,
                    lift = true,
                )
            }
        }

        BoardAccessibilityLayer(
            board = board,
            selected = selected,
            legalTargets = legalTargets,
            flipped = flipped,
            enabled = enabled,
            summary = boardSummary,
            onSquareClick = currentOnSquareClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private data class DragState(
    val origin: Square,
    val piece: Piece,
    val position: Offset,
    val target: Square?,
)

@Composable
private fun BoardAccessibilityLayer(
    board: BoardSnapshot,
    selected: Square?,
    legalTargets: Set<Square>,
    flipped: Boolean,
    enabled: Boolean,
    summary: String,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = summary },
    ) {
        repeat(8) { screenRank ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                repeat(8) { screenFile ->
                    val file = if (flipped) 7 - screenFile else screenFile
                    val rank = if (flipped) screenRank else 7 - screenRank
                    val square = Square(file, rank)
                    val piece = board.pieceAt(square)
                    val squareName = square.algebraic
                    val pieceName = piece?.let { pieceDisplayName(it) }
                        ?: stringResource(R.string.board_square_empty)
                    val isSelected = selected == square
                    val isLegalTarget = square in legalTargets
                    val isCheckedKing = piece?.type == PieceType.KING &&
                        piece.side == board.sideToMove &&
                        board.inCheck
                    val selectedState = if (isSelected) {
                        stringResource(R.string.board_state_selected)
                    } else {
                        null
                    }
                    val legalState = if (isLegalTarget) {
                        stringResource(
                            if (piece != null) {
                                R.string.board_state_capture
                            } else {
                                R.string.board_state_legal
                            },
                        )
                    } else {
                        null
                    }
                    val checkState = if (isCheckedKing) {
                        stringResource(R.string.board_state_check)
                    } else {
                        null
                    }
                    val stateText = listOfNotNull(selectedState, legalState, checkState)
                        .joinToString(", ")
                    val actionLabel = if (isLegalTarget) {
                        stringResource(R.string.board_action_move, squareName)
                    } else {
                        stringResource(R.string.board_action_select, squareName)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = "$squareName, $pieceName"
                                if (stateText.isNotEmpty()) stateDescription = stateText
                                if (enabled) {
                                    onClick(label = actionLabel) {
                                        onSquareClick(square)
                                        true
                                    }
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun pieceDisplayName(piece: Piece): String = stringResource(
    when (piece.side) {
        Side.WHITE -> when (piece.type) {
            PieceType.PAWN -> R.string.board_piece_white_pawn
            PieceType.KNIGHT -> R.string.board_piece_white_knight
            PieceType.BISHOP -> R.string.board_piece_white_bishop
            PieceType.ROOK -> R.string.board_piece_white_rook
            PieceType.QUEEN -> R.string.board_piece_white_queen
            PieceType.KING -> R.string.board_piece_white_king
        }

        Side.BLACK -> when (piece.type) {
            PieceType.PAWN -> R.string.board_piece_black_pawn
            PieceType.KNIGHT -> R.string.board_piece_black_knight
            PieceType.BISHOP -> R.string.board_piece_black_bishop
            PieceType.ROOK -> R.string.board_piece_black_rook
            PieceType.QUEEN -> R.string.board_piece_black_queen
            PieceType.KING -> R.string.board_piece_black_king
        }
    },
)

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

@Suppress("unused")
private fun moveDistance(from: Square, to: Square): Int =
    abs(from.file - to.file) + abs(from.rank - to.rank)

@Suppress("unused")
private fun pieceLabel(piece: Piece): String = "${piece.side.name} ${piece.type.name}"
