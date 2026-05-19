package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.BorderTokens
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.appBorder
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledBorder
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.core.data.util.formatGridDurationHours
import com.nltimer.feature.home.model.GridCellUiState

private const val PLATINUM_RED_BASE = 0.78f
private const val PLATINUM_GREEN_BASE = 0.69f
private const val PLATINUM_RED_STRENGTH = 0.22f
private const val PLATINUM_GREEN_STRENGTH = 0.31f
private const val BACKGROUND_ICON_ALPHA = 0.15f
private const val BACKGROUND_ICON_SIZE_RATIO = 0.25f
private const val BACKGROUND_ICON_WIDTH_RATIO = 1.6f
private const val BACKGROUND_ICON_HEIGHT_RATIO = 1.45f

@Composable
fun GridCell(
    cell: GridCellUiState,
    modifier: Modifier = Modifier,
    gridStyle: GridLayoutStyle = GridLayoutStyle(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
) {
    val isPlatinum = cell.wasPlanned && cell.status == BehaviorNature.COMPLETED
    val platinumStrength = cell.platinumStrength

    val backgroundColor = when {
        cell.isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = styledAlpha(gridStyle.activeBgAlpha))
        isPlatinum -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = gridStyle.maxCellHeight.dp)
            .clipToBounds()
            .background(backgroundColor, RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)))
            .appBorder(
                borderProducer = {
                    val borderColor = when {
                        isPlatinum -> {
                            val strength = platinumStrength
                            Color(
                                red = (PLATINUM_RED_BASE + PLATINUM_RED_STRENGTH * strength),
                                green = (PLATINUM_GREEN_BASE + PLATINUM_GREEN_STRENGTH * strength),
                                blue = 1.0f,
                                alpha = styledAlpha(0.5f) + 0.5f * strength,
                            )
                        }
                        cell.isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    val borderWidth = if (cell.isCurrent || isPlatinum) styledBorder(BorderTokens.STANDARD) else styledBorder(BorderTokens.THIN)
                    BorderStroke(borderWidth, borderColor)
                },
                shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM))
            )
            .padding(gridStyle.cellPadding.dp),
    ) {
        cell.activityIconKey?.let { iconKey ->
            val backgroundIconSize = (gridStyle.maxCellHeight * BACKGROUND_ICON_SIZE_RATIO).dp
            val backgroundIconWidth = backgroundIconSize * BACKGROUND_ICON_WIDTH_RATIO
            val backgroundIconHeight = backgroundIconSize * BACKGROUND_ICON_HEIGHT_RATIO
            val backgroundIconAlpha = styledAlpha(BACKGROUND_ICON_ALPHA)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, bottom = 2.dp)
                        .requiredWidth(backgroundIconWidth)
                        .requiredHeight(backgroundIconHeight)
                        .graphicsLayer {
                            alpha = backgroundIconAlpha
                        },
                    contentAlignment = Alignment.BottomStart,
                ) {
                    IconRenderer(
                        iconKey = iconKey,
                        defaultEmoji = "❓",
                        tint = MaterialTheme.colorScheme.onSurface,
                        iconSize = backgroundIconSize,
                        modifier = Modifier
                            .requiredWidth(backgroundIconWidth)
                            .requiredHeight(backgroundIconHeight),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            if (cell.activityName != null || cell.formattedDuration.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    cell.activityName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } ?: Spacer(modifier = Modifier.weight(1f))

                    GridDurationLabel(
                        cell = cell,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            if (cell.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.scale(gridStyle.tagScale),
                    horizontalArrangement = Arrangement.spacedBy(gridStyle.tagSpacing.dp),
                    verticalArrangement = Arrangement.spacedBy(gridStyle.tagSpacing.dp),
                    maxLines = 2,
                ) {
                    cell.tags.forEach { tag -> TagChip(tag = tag, tagDisplayConfig = tagDisplayConfig) }
                }
            }
            cell.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = styledAlpha(0.6f))
                )
            }
        }
    }
}

@Composable
private fun GridDurationLabel(
    cell: GridCellUiState,
    style: TextStyle,
    color: Color,
) {
    val durationText = cell.formattedDuration

    if (durationText.isNotEmpty()) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL)),
                )
                .padding(horizontal = 3.dp, vertical = 0.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = durationText,
                style = style,
                color = color,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
