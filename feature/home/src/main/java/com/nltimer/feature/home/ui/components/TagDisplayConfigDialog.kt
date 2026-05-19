package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.TagDisplayConfig
import com.nltimer.core.designsystem.theme.ChipDisplayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDisplayConfigDialog(
    config: TagDisplayConfig,
    onConfigChange: (TagDisplayConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayMode by remember { mutableStateOf(config.displayMode) }
    var useColorForText by remember { mutableStateOf(config.useColorForText) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "标签样式",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "显示样式",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val modes = remember {
                        listOf(
                            ChipDisplayMode.Filled to "填充",
                            ChipDisplayMode.Underline to "下划线",
                            ChipDisplayMode.Capsules to "胶囊",
                            ChipDisplayMode.RoundedCorners to "圆角",
                            ChipDisplayMode.Squares to "方块",
                            ChipDisplayMode.SquareBorder to "方框",
                            ChipDisplayMode.HandDrawn to "手绘",
                            ChipDisplayMode.DashedLines to "虚线",
                            ChipDisplayMode.None to "无",
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size,
                                ),
                                onClick = { displayMode = mode },
                                selected = displayMode == mode,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "配色",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { useColorForText = false },
                            selected = !useColorForText,
                        ) {
                            Text("强调色", style = MaterialTheme.typography.labelSmall)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { useColorForText = true },
                            selected = useColorForText,
                        ) {
                            Text("活动色", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            onConfigChange(config.copy(displayMode = displayMode, useColorForText = useColorForText))
                            onDismiss()
                        },
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
