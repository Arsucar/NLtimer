package com.nltimer.feature.home.ui.components.moment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.ui.components.SlideActionPill

@Composable
internal fun PendingCard(
    cell: GridCellUiState,
    onStart: () -> Unit,
    _momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    focusCardConfig: FocusCardConfig = FocusCardConfig(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
    modifier: Modifier = Modifier,
) {
    val estimatedText = cell.estimatedDuration?.let { "预计 ${formatDuration(it)}" } ?: ""

    val cornerDp = focusCardConfig.resolvedCornerDp()
    val cardShape = RoundedCornerShape(cornerDp.dp)
    val cardHeight = focusCardConfig.cardHeight.dp
    val containerColor = focusCardConfig.resolvedContainerColor()
    val contentColor = focusCardConfig.resolvedContentColor()
    val effectivePadding = focusCardConfig.cardPadding.dp

    if (focusCardConfig.enableCardStyle) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(cardHeight),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = focusCardConfig.resolvedElevation().dp),
        ) {
            PendingCardContent(
                cell = cell,
                estimatedText = estimatedText,
                contentColor = contentColor,
                effectivePadding = effectivePadding,
                cardHeight = cardHeight,
                onStart = onStart,
                tagDisplayConfig = tagDisplayConfig,
            )
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(cardHeight),
            color = Color.Transparent,
            shape = cardShape,
        ) {
            PendingCardContent(
                cell = cell,
                estimatedText = estimatedText,
                contentColor = contentColor,
                effectivePadding = effectivePadding,
                cardHeight = cardHeight,
                onStart = onStart,
                tagDisplayConfig = tagDisplayConfig,
            )
        }
    }
}

@Composable
private fun PendingCardContent(
    cell: GridCellUiState,
    estimatedText: String,
    contentColor: Color,
    effectivePadding: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    onStart: () -> Unit,
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .height(cardHeight)
            .padding(effectivePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconRenderer(
                iconKey = cell.activityIconKey,
                defaultEmoji = "📌",
                iconSize = 32.dp,
            )
            Text(
                text = cell.activityName ?: "",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
            )
        }

        Spacer(Modifier.height(12.dp))

        SlideActionPill(
            onActivate = onStart,
            activeLabel = "滑动开启",
            activatedLabel = "释放开启",
            leadingIcon = Icons.Filled.PlayArrow,
            activatedIcon = Icons.Filled.Check,
        )

        TagNoteRow(tags = cell.tags, note = cell.note, tagDisplayConfig = tagDisplayConfig)

        Spacer(Modifier.height(8.dp))

        if (estimatedText.isNotEmpty()) {
            Text(
                text = estimatedText,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = styledAlpha(0.7f)),
            )
        }

        Text(
            text = "滑动开启目标",
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = styledAlpha(0.5f)),
            textAlign = TextAlign.Center,
        )
    }
}
