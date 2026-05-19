package com.nltimer.feature.home.ui.components.moment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.nltimer.core.designsystem.theme.styledAlpha

@Composable
internal fun EmptyCard(
    onClick: () -> Unit,
    momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    focusCardConfig: FocusCardConfig = FocusCardConfig(),
    modifier: Modifier = Modifier,
) {
    val cornerDp = focusCardConfig.resolvedCornerDp()
    val cardShape = RoundedCornerShape(cornerDp.dp)
    val cardHeight = focusCardConfig.cardHeight.dp
    val containerColor = focusCardConfig.resolvedContainerColor()
    val contentColor = focusCardConfig.resolvedContentColor()
    val effectivePadding = focusCardConfig.cardPadding.dp

    if (focusCardConfig.enableCardStyle) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(cardHeight),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = focusCardConfig.resolvedElevation().dp),
        ) {
            EmptyCardContent(
                contentColor = contentColor,
                effectivePadding = effectivePadding,
                cardHeight = cardHeight,
            )
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(cardHeight),
            color = Color.Transparent,
            shape = cardShape,
        ) {
            EmptyCardContent(
                contentColor = contentColor,
                effectivePadding = effectivePadding,
                cardHeight = cardHeight,
            )
        }
    }
}

@Composable
private fun EmptyCardContent(
    contentColor: Color,
    effectivePadding: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .height(cardHeight)
            .padding(effectivePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = contentColor.copy(alpha = styledAlpha(0.6f)),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "添加行为",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "点击开始记录你的行为",
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = styledAlpha(0.5f)),
            textAlign = TextAlign.Center,
        )
    }
}
