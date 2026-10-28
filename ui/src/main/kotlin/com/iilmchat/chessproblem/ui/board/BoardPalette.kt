package com.iilmchat.chessproblem.ui.board

import androidx.compose.ui.graphics.Color
import com.iilmchat.chessproblem.core.model.BoardTheme

data class BoardPalette(
    val light: Color,
    val dark: Color,
    val selected: Color,
    val lastMove: Color,
    val legal: Color,
    val legalCapture: Color,
    val check: Color,
    val coordinateOnLight: Color,
    val coordinateOnDark: Color,
    val whitePiece: Color,
    val whiteStroke: Color,
    val whiteDetail: Color,
    val blackPiece: Color,
    val blackStroke: Color,
    val blackDetail: Color,
)

fun boardPalette(theme: BoardTheme): BoardPalette = when (theme) {
    BoardTheme.SAGE -> BoardPalette(
        light = Color(0xFFE9DFC8),
        dark = Color(0xFF7E9B85),
        selected = Color(0xFF34D399).copy(alpha = 0.55f),
        lastMove = Color(0xFFD6A756).copy(alpha = 0.42f),
        legal = Color(0xFF0B3B2A).copy(alpha = 0.32f),
        legalCapture = Color(0xFF0B3B2A).copy(alpha = 0.5f),
        check = Color(0xFFEF4444).copy(alpha = 0.62f),
        coordinateOnLight = Color(0xFF3E4A3F).copy(alpha = 0.85f),
        coordinateOnDark = Color(0xFFF4F0E5).copy(alpha = 0.9f),
        whitePiece = Color(0xFFF6F0E2),
        whiteStroke = Color(0xFF8B5E25),
        whiteDetail = Color(0xFF15251E),
        blackPiece = Color(0xFF263A30),
        blackStroke = Color(0xFF0B1511),
        blackDetail = Color(0xFFEADFC8),
    )

    BoardTheme.CLASSIC -> BoardPalette(
        light = Color(0xFFF0D9B5),
        dark = Color(0xFFB58863),
        selected = Color(0xFF34D399).copy(alpha = 0.55f),
        lastMove = Color(0xFFD6A756).copy(alpha = 0.5f),
        legal = Color(0xFF3A2516).copy(alpha = 0.3f),
        legalCapture = Color(0xFF3A2516).copy(alpha = 0.55f),
        check = Color(0xFFDC2626).copy(alpha = 0.6f),
        coordinateOnLight = Color(0xFF5B4028).copy(alpha = 0.75f),
        coordinateOnDark = Color(0xFFFFF7E8).copy(alpha = 0.85f),
        whitePiece = Color(0xFFFFF8EA),
        whiteStroke = Color(0xFF7A4E1D),
        whiteDetail = Color(0xFF2A1C05),
        blackPiece = Color(0xFF33261C),
        blackStroke = Color(0xFF120D09),
        blackDetail = Color(0xFFF0D9B5),
    )

    BoardTheme.SLATE -> BoardPalette(
        light = Color(0xFFD8DEE9),
        dark = Color(0xFF5C6B73),
        selected = Color(0xFF34D399).copy(alpha = 0.5f),
        lastMove = Color(0xFFD6A756).copy(alpha = 0.45f),
        legal = Color(0xFF14232B).copy(alpha = 0.32f),
        legalCapture = Color(0xFF14232B).copy(alpha = 0.55f),
        check = Color(0xFFF87171).copy(alpha = 0.6f),
        coordinateOnLight = Color(0xFF2E3A40).copy(alpha = 0.75f),
        coordinateOnDark = Color(0xFFF4F0E5).copy(alpha = 0.85f),
        whitePiece = Color(0xFFF4F7FA),
        whiteStroke = Color(0xFF55636B),
        whiteDetail = Color(0xFF1B2420),
        blackPiece = Color(0xFF26343B),
        blackStroke = Color(0xFF0A1013),
        blackDetail = Color(0xFFD8DEE9),
    )

    BoardTheme.HIGH_CONTRAST -> BoardPalette(
        light = Color(0xFFFFFDF5),
        dark = Color(0xFF2B4A3F),
        selected = Color(0xFF34D399).copy(alpha = 0.65f),
        lastMove = Color(0xFFFFD166).copy(alpha = 0.55f),
        legal = Color(0xFF052E1F).copy(alpha = 0.5f),
        legalCapture = Color(0xFF052E1F).copy(alpha = 0.75f),
        check = Color(0xFFFF3B30).copy(alpha = 0.7f),
        coordinateOnLight = Color(0xFF052E1F).copy(alpha = 0.9f),
        coordinateOnDark = Color(0xFFFFFFFF).copy(alpha = 0.95f),
        whitePiece = Color(0xFFFFFFFF),
        whiteStroke = Color(0xFF5B3A12),
        whiteDetail = Color(0xFF000000),
        blackPiece = Color(0xFF101A16),
        blackStroke = Color(0xFF000000),
        blackDetail = Color(0xFFFFFFFF),
    )
}
