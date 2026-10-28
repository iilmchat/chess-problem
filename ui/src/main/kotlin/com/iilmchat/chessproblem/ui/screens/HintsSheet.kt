package com.iilmchat.chessproblem.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.session.PuzzleState
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.components.InfoPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HintsSheet(
    session: PuzzleState,
    balance: Int,
    onUseHint: (HintTier) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.puzzle_hints_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                InfoPill(
                    text = stringResource(R.string.puzzle_hints_balance, balance),
                    icon = Icons.Filled.Lightbulb,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                )
            }

            HintOption(
                tier = HintTier.IDEA,
                title = stringResource(R.string.puzzle_hint_idea),
                description = stringResource(R.string.puzzle_hint_idea_desc),
                icon = Icons.Filled.Lightbulb,
                accent = MaterialTheme.colorScheme.secondary,
                cost = 0,
                used = !session.ideaAvailable,
                onClick = { onUseHint(HintTier.IDEA) },
            )
            HintOption(
                tier = HintTier.PIECE,
                title = stringResource(R.string.puzzle_hint_piece),
                description = stringResource(R.string.puzzle_hint_piece_desc),
                icon = Icons.Filled.Search,
                accent = MaterialTheme.colorScheme.primary,
                cost = HintTier.PIECE.tokenCost,
                used = false,
                onClick = { onUseHint(HintTier.PIECE) },
            )
            HintOption(
                tier = HintTier.MOVE,
                title = stringResource(R.string.puzzle_hint_move),
                description = stringResource(R.string.puzzle_hint_move_desc),
                icon = Icons.Filled.ArrowForward,
                accent = MaterialTheme.colorScheme.tertiary,
                cost = HintTier.MOVE.tokenCost,
                used = false,
                onClick = { onUseHint(HintTier.MOVE) },
            )

            Text(
                text = stringResource(R.string.puzzle_hint_refill),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun HintOption(
    tier: HintTier,
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    cost: Int,
    used: Boolean,
    onClick: () -> Unit,
) {
    val enabled = !used
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.55f else 0.25f),
        border = BorderStroke(
            1.dp,
            if (enabled) accent.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = accent.copy(alpha = if (enabled) 0.15f else 0.07f),
                    contentColor = if (enabled) accent else accent.copy(alpha = 0.4f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (used) {
                        Spacer(Modifier.width(8.dp))
                        InfoPill(text = stringResource(R.string.puzzle_hint_used))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (cost == 0) {
                InfoPill(
                    text = stringResource(R.string.puzzle_hint_free),
                    contentColor = accent,
                    background = accent.copy(alpha = 0.12f),
                )
            } else {
                InfoPill(
                    text = stringResource(R.string.puzzle_hint_cost, cost),
                    icon = Icons.Filled.Lightbulb,
                    contentColor = accent,
                    background = accent.copy(alpha = 0.12f),
                )
            }
        }
    }
}
