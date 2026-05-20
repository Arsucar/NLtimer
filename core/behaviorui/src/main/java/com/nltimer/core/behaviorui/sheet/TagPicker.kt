package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Tag
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.styledAlpha
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun TagPicker(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    if (tags.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag -> key(tag.id) {
            val isSelected = tag.id in selectedTagIds
            val tagColor = tag.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
            val backgroundColor = if (isSelected) {
                tagColor.copy(alpha = styledAlpha(0.2f))
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val borderColor = if (isSelected) tagColor else Color.Transparent
            val textColor = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                onClick = { onTagToggle(tag.id) },
                shape = RoundedCornerShape(14.dp),
                color = backgroundColor,
                border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showIcon && tag.iconKey != null) {
                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconRenderer(
                                iconKey = tag.iconKey,
                                defaultEmoji = "hi:Tag01",
                                iconSize = 14.dp,
                                tint = textColor,
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = tag.name,
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } }
    }
}
