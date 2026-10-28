package com.iilmchat.chessproblem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iilmchat.chessproblem.core.chess.Side
import com.iilmchat.chessproblem.core.session.PlayedMove
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.theme.SanTextStyle

@Composable
fun MoveLog(
    moves: List<PlayedMove>,
    userMoves: Int,
    totalUserMoves: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.puzzle_moves_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.puzzle_moves_count, userMoves, totalUserMoves),
                    style = SanTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (moves.isEmpty()) {
                Text(
                    text = "—",
                    style = SanTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(moves) { index, move ->
                        val moveNumber = 1 + moves.take(index).count { it.side == Side.BLACK }
                        val prefix = if (move.side == Side.WHITE) "$moveNumber." else "$moveNumber…"
                        MovePill(
                            text = "$prefix ${move.san}",
                            byUser = move.byUser,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovePill(
    text: String,
    byUser: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (byUser) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (byUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            },
        ),
    ) {
        Text(
            text = text,
            style = SanTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}
