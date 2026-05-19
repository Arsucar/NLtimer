package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.designsystem.theme.BorderTokens
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.appBorder
import com.nltimer.core.designsystem.theme.styledBorder
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.TagUiState

@Composable
fun Modifier.behaviorCardStyle(
    cardBackground: Color,
    borderColor: Color,
    logStyle: LogLayoutStyle = LogLayoutStyle(),
): Modifier = this
    .clip(RoundedCornerShape(styledCorner(ShapeTokens.CORNER_LARGE)))
    .background(cardBackground)
    .appBorder(
        borderProducer = { BorderStroke(styledBorder(BorderTokens.THIN), borderColor) },
        shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_LARGE))
    )
    .padding(logStyle.cardPadding.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BehaviorTagRow(tags: List<TagUiState>, logStyle: LogLayoutStyle = LogLayoutStyle(), tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig()) {
    if (tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(logStyle.tagRowSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                TagChipSmall(tag.name, tagDisplayConfig = tagDisplayConfig)
            }
        }
    }
}
