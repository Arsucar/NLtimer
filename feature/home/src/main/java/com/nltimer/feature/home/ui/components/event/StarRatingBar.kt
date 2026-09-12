package com.nltimer.feature.home.ui.components.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 单星图标尺寸（紧凑型评分，表单行内使用） */
private const val STAR_SIZE_DP = 28

/**
 * StarRatingBar 星级评分条（1~5 星，轻点已选星清除）
 * 半星不需要；RATING 字段值以 Int 存 behavior_event_value.valueNumber
 */
@Composable
internal fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (star in 1..maxRating) {
            IconButton(
                onClick = { onRatingChange(if (star == rating) 0 else star) },
                modifier = Modifier.size(STAR_SIZE_DP.dp),
            ) {
                Icon(
                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$star 星",
                    tint = if (star <= rating) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                )
            }
        }
    }
}
