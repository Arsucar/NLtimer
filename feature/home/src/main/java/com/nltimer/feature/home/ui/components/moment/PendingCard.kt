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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.designsystem.icon.IconRenderer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.ui.components.SlideActionPill

private val FOCUS_CARD_HEIGHT = 260.dp

@Composable
internal fun PendingCard(
    cell: GridCellUiState,
    onStart: () -> Unit,
    momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
    modifier: Modifier = Modifier,
) {
    val estimatedText = cell.estimatedDuration?.let { "预计 ${formatDuration(it)}" } ?: ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(FOCUS_CARD_HEIGHT),
        shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_FULL)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .height(FOCUS_CARD_HEIGHT)
                .padding(momentStyle.cardPadding.dp),
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.7f)),
                )
            }

            Text(
                text = "滑动开启目标",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.5f)),
                textAlign = TextAlign.Center,
            )
        }
    }
}
