package com.nltimer.feature.home.ui.components

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.FocusCardCornerStyle
import com.nltimer.core.data.model.FocusCardShadowStyle
import com.nltimer.core.designsystem.component.ConfigStepper

private val PresetColors = listOf(
    null,
    0xFFE53935.toInt(),
    0xFF1E88E5.toInt(),
    0xFF43A047.toInt(),
    0xFFFB8C00.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00ACC1.toInt(),
    0xFFD81B60.toInt(),
    0xFF5E35B1.toInt(),
    0xFF00897B.toInt(),
    0xFFF4511E.toInt(),
    0xFF3949AB.toInt(),
    0xFF7CB342.toInt(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusCardConfigDialog(
    config: FocusCardConfig,
    onConfigChange: (FocusCardConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var cardHeight by remember { mutableStateOf(config.cardHeight) }
    var cardPadding by remember { mutableStateOf(config.cardPadding) }
    var enableCardStyle by remember { mutableStateOf(config.enableCardStyle) }
    var themeColor by remember { mutableStateOf(config.themeColor) }
    var cornerStyle by remember { mutableStateOf(config.cornerStyle) }
    var customCornerSize by remember { mutableStateOf(config.customCornerSize) }
    var shadowStyle by remember { mutableStateOf(config.shadowStyle) }
    var customColorInput by remember(themeColor) {
        mutableStateOf(themeColor?.let { "#${String.format("%08X", it).takeLast(6)}" } ?: "")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as DialogWindowProvider).window
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(360.dp)
                    .heightIn(max = 600.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "专注卡片配置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ConfigStepper(
                            label = "卡片高度",
                            value = cardHeight,
                            suffix = "dp",
                            min = 100,
                            max = 500,
                            onValueChange = { cardHeight = it },
                        )

                        ConfigStepper(
                            label = "内边距",
                            value = cardPadding,
                            suffix = "dp",
                            min = 0,
                            max = 64,
                            onValueChange = { cardPadding = it },
                        )

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "卡片样式",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = enableCardStyle,
                                onCheckedChange = { enableCardStyle = it },
                            )
                        }

                        if (enableCardStyle) {
                            Text(
                                text = "主题色",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PresetColors.forEach { preset ->
                                    val selected = themeColor == preset?.toLong()
                                    val color = preset?.let { Color(it) }
                                        ?: MaterialTheme.colorScheme.primaryContainer
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (preset == null) Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    CircleShape,
                                                ) else Modifier
                                            )
                                            .then(
                                                if (selected) Modifier.border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape,
                                                ) else Modifier
                                            )
                                            .clickable { themeColor = preset?.toLong() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (preset == null) {
                                            Text(
                                                text = "自",
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "自定义",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = customColorInput,
                                    onValueChange = { input ->
                                        val cleaned = input.removePrefix("#")
                                        customColorInput = cleaned
                                        val argb = cleaned.toLongOrNull(16)
                                        if (argb != null && cleaned.length == 6) {
                                            themeColor = argb or 0xFF000000
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("RRGGBB") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                )
                            }

                            HorizontalDivider()

                            Text(
                                text = "圆角",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val cornerOptions = remember {
                                listOf(
                                    FocusCardCornerStyle.NONE to "无",
                                    FocusCardCornerStyle.SMALL to "小",
                                    FocusCardCornerStyle.LARGE to "大",
                                    FocusCardCornerStyle.CUSTOM to "自定义",
                                )
                            }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                cornerOptions.forEachIndexed { index, (style, label) ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = cornerOptions.size,
                                        ),
                                        onClick = { cornerStyle = style },
                                        selected = cornerStyle == style,
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            if (cornerStyle == FocusCardCornerStyle.CUSTOM) {
                                ConfigStepper(
                                    label = "圆角大小",
                                    value = customCornerSize,
                                    suffix = "dp",
                                    min = 0,
                                    max = 100,
                                    onValueChange = { customCornerSize = it },
                                )
                            }

                            HorizontalDivider()

                            Text(
                                text = "阴影",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val shadowOptions = remember {
                                listOf(
                                    FocusCardShadowStyle.NONE to "无",
                                    FocusCardShadowStyle.LIGHT to "轻度",
                                    FocusCardShadowStyle.STANDARD to "标准",
                                    FocusCardShadowStyle.HEAVY to "重度",
                                )
                            }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                shadowOptions.forEachIndexed { index, (style, label) ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = shadowOptions.size,
                                        ),
                                        onClick = { shadowStyle = style },
                                        selected = shadowStyle == style,
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            val default = FocusCardConfig()
                            cardHeight = default.cardHeight
                            cardPadding = default.cardPadding
                            enableCardStyle = default.enableCardStyle
                            themeColor = default.themeColor
                            cornerStyle = default.cornerStyle
                            customCornerSize = default.customCornerSize
                            shadowStyle = default.shadowStyle
                        }) {
                            Text("重置默认")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            onConfigChange(
                                FocusCardConfig(
                                    cardHeight = cardHeight,
                                    cardPadding = cardPadding,
                                    enableCardStyle = enableCardStyle,
                                    themeColor = themeColor,
                                    cornerStyle = cornerStyle,
                                    customCornerSize = customCornerSize,
                                    shadowStyle = shadowStyle,
                                )
                            )
                            onDismiss()
                        }) {
                            Text("应用")
                        }
                    }
                }
            }
        }
    }
}
