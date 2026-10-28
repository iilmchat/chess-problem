package com.iilmchat.chessproblem.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.iilmchat.chessproblem.core.chess.Piece
import com.iilmchat.chessproblem.core.chess.PieceType
import com.iilmchat.chessproblem.core.chess.Side

private sealed interface PiecePart {
    data class Svg(
        val data: String,
        val fill: Boolean = true,
        val stroke: Boolean = true,
        val detail: Boolean = false,
        val strokeWidth: Float = 2.2f,
        val alpha: Float = 1f,
    ) : PiecePart

    data class Rect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val radius: Float,
        val fill: Boolean = true,
        val stroke: Boolean = true,
    ) : PiecePart

    data class Circle(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val fill: Boolean = true,
        val stroke: Boolean = false,
        val detail: Boolean = false,
    ) : PiecePart
}

private data class PieceArt(val parts: List<PiecePart>)

private val pawnArt = PieceArt(
    listOf(
        PiecePart.Circle(cx = 32f, cy = 16.5f, radius = 8.5f),
        PiecePart.Svg("M26.4 26.5h11.2c.3 3.7-1.3 6.8-4 8.8 7 2.8 11 8.4 12 16.2H18.4c1-7.8 5-13.4 12-16.2-2.7-2-4.3-5.1-4-8.8z"),
        PiecePart.Rect(x = 15f, y = 50.5f, width = 34f, height = 6f, radius = 3f),
    ),
)

private val rookArt = PieceArt(
    listOf(
        PiecePart.Svg("M19 11h6v5h4v-5h6v5h4v-5h6v13l-4 4v18l4 5v5H19v-5l4-5V28l-4-4z"),
        PiecePart.Rect(x = 15f, y = 50.5f, width = 34f, height = 6f, radius = 3f),
    ),
)

// Silhouette adapted from the brand knight in design/icon.svg.
private val knightArt = PieceArt(
    listOf(
        PiecePart.Svg("M22.625 53.25L22.625 47C22.625 42.938 24.188 39.5 27 37L32.938 31.688L30.438 28.25C29.188 26.375 29.5 23.875 31.063 22.313L36.063 17.938L35.125 13.25L38.875 17L41.375 12L42.625 17.938L46.688 19.5C49.813 20.75 51.688 23.563 52 27L52.313 31.375C52.313 34.188 51.063 36.688 48.875 38.25L44.813 41.063C42.938 42.313 42 44.5 42.313 46.688L42.938 53.25Z"),
        PiecePart.Rect(x = 15.5f, y = 50.5f, width = 33f, height = 6f, radius = 3f),
        PiecePart.Circle(cx = 45.75f, cy = 26.375f, radius = 2f, fill = true, stroke = false, detail = true),
    ),
)

private val bishopArt = PieceArt(
    listOf(
        PiecePart.Svg("M32 8c4.5 5.5 7 10 7 14 0 3-1.8 5.6-4.5 7.5 5.6 2.8 9 8 9 14.5H20.5c0-6.5 3.4-11.7 9-14.5-2.7-1.9-4.5-4.5-4.5-7.5 0-4 2.5-8.5 7-14z"),
        PiecePart.Rect(x = 17f, y = 45.5f, width = 30f, height = 5f, radius = 2.5f),
        PiecePart.Rect(x = 13f, y = 50.5f, width = 38f, height = 6f, radius = 3f),
        PiecePart.Svg(
            data = "M32 21.5v11",
            fill = false,
            stroke = true,
            strokeWidth = 1.7f,
            alpha = 0.5f,
        ),
    ),
)

private val queenArt = PieceArt(
    listOf(
        PiecePart.Svg("M14.5 20.5l7 8.5 6.5-14 4 14.5 4-14.5 6.5 14 7-8.5-4.2 21H18.7z"),
        PiecePart.Svg("M21.5 42h21l3 9h-27z"),
        PiecePart.Rect(x = 14f, y = 50.5f, width = 36f, height = 6f, radius = 3f),
        PiecePart.Circle(cx = 14.5f, cy = 18.5f, radius = 3f),
        PiecePart.Circle(cx = 28f, cy = 13.5f, radius = 3f),
        PiecePart.Circle(cx = 36f, cy = 13.5f, radius = 3f),
        PiecePart.Circle(cx = 49.5f, cy = 18.5f, radius = 3f),
    ),
)

private val kingArt = PieceArt(
    listOf(
        PiecePart.Svg("M29 6h6v5h5v6h-5v5h-6v-5h-5v-6h5z"),
        PiecePart.Svg("M20 24.5c0-2.2 1.8-4 4-4h16c2.2 0 4 1.8 4 4l-3 15.5H23z"),
        PiecePart.Rect(x = 21f, y = 40f, width = 22f, height = 9f, radius = 2f),
        PiecePart.Rect(x = 14f, y = 50.5f, width = 36f, height = 6f, radius = 3f),
    ),
)

private fun artFor(type: PieceType): PieceArt = when (type) {
    PieceType.PAWN -> pawnArt
    PieceType.ROOK -> rookArt
    PieceType.KNIGHT -> knightArt
    PieceType.BISHOP -> bishopArt
    PieceType.QUEEN -> queenArt
    PieceType.KING -> kingArt
}

private val pathCache = HashMap<String, Path>()

private fun cachedPath(data: String): Path = pathCache.getOrPut(data) {
    PathParser().parsePathString(data).toPath()
}

fun DrawScope.drawPiece(
    piece: Piece,
    left: Float,
    top: Float,
    size: Float,
    palette: BoardPalette,
    lift: Boolean = false,
) {
    val scale = size / 64f
    val fill = if (piece.side == Side.WHITE) palette.whitePiece else palette.blackPiece
    val stroke = if (piece.side == Side.WHITE) palette.whiteStroke else palette.blackStroke
    val detail = if (piece.side == Side.WHITE) palette.whiteDetail else palette.blackDetail
    val yOffset = if (lift) -size * 0.025f else 0f

    withTransform({
        translate(left, top + yOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        withTransform({ translate(0f, 2.2f) }) {
            drawArt(artFor(piece.type), shadow = true, fill, stroke, detail)
        }
        drawArt(artFor(piece.type), shadow = false, fill, stroke, detail)
    }
}

private fun DrawScope.drawArt(
    art: PieceArt,
    shadow: Boolean,
    fillColor: Color,
    strokeColor: Color,
    detailColor: Color,
) {
    art.parts.forEach { part ->
        when (part) {
            is PiecePart.Svg -> {
                val path = cachedPath(part.data)
                if (part.fill) {
                    drawPath(
                        path = path,
                        color = if (shadow) Color.Black.copy(alpha = 0.2f) else if (part.detail) detailColor else fillColor,
                    )
                } else if (shadow) {
                    return@forEach
                }
                if (!shadow && part.stroke) {
                    drawPath(
                        path = path,
                        color = if (part.detail) detailColor else strokeColor,
                        style = Stroke(
                            width = part.strokeWidth,
                            cap = StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                        alpha = part.alpha,
                    )
                }
            }

            is PiecePart.Rect -> {
                if (shadow) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(part.x, part.y),
                        size = androidx.compose.ui.geometry.Size(part.width, part.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(part.radius),
                    )
                } else {
                    if (part.fill) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(part.x, part.y),
                            size = androidx.compose.ui.geometry.Size(part.width, part.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(part.radius),
                        )
                    }
                    if (part.stroke) {
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = Offset(part.x, part.y),
                            size = androidx.compose.ui.geometry.Size(part.width, part.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(part.radius),
                            style = Stroke(width = 2.2f),
                        )
                    }
                }
            }

            is PiecePart.Circle -> {
                if (shadow) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = part.radius,
                        center = Offset(part.cx, part.cy),
                    )
                } else {
                    if (part.fill) {
                        drawCircle(
                            color = if (part.detail) detailColor else fillColor,
                            radius = part.radius,
                            center = Offset(part.cx, part.cy),
                        )
                    }
                    if (part.stroke) {
                        drawCircle(
                            color = strokeColor,
                            radius = part.radius,
                            center = Offset(part.cx, part.cy),
                            style = Stroke(width = 2.2f),
                        )
                    }
                }
            }
        }
    }
}
