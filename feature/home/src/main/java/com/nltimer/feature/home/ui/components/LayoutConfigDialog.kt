package com.nltimer.feature.home.ui.components

import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TextListColumnMode
import com.nltimer.core.data.model.TextListFieldColorMode
import com.nltimer.core.data.model.TextListFieldConfig
import com.nltimer.core.data.model.TextListFieldType
import com.nltimer.core.data.model.TextListLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.component.ConfigStepper
import com.nltimer.core.designsystem.component.LayoutResetButton
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.feature.home.model.resetLayout

private val DialogShape = RoundedCornerShape(16.dp)
private val FieldShape = RoundedCornerShape(12.dp)

@Composable
fun LayoutConfigDialog(
    layout: HomeLayout,
    config: HomeLayoutConfig,
    onConfigChange: (HomeLayoutConfig) -> Unit,
    onDismiss: () -> Unit,
) {
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = DialogShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(340.dp)
                    .heightIn(max = 540.dp)
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
                            text = layout.toConfigTitle(),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (layout) {
                            HomeLayout.GRID -> GridConfigSection(
                                grid = config.grid,
                                onChange = { onConfigChange(config.copy(grid = it)) },
                            )
                            HomeLayout.LOG -> LogConfigSection(
                                log = config.log,
                                onChange = { onConfigChange(config.copy(log = it)) },
                            )
                            HomeLayout.TIMELINE_REVERSE -> TimelineConfigSection(
                                timeline = config.timeline,
                                onChange = { onConfigChange(config.copy(timeline = it)) },
                            )
                            HomeLayout.MOMENT -> MomentConfigSection(
                                moment = config.moment,
                                onChange = { onConfigChange(config.copy(moment = it)) },
                            )
                            HomeLayout.TEXT_LIST -> TextListConfigSection(
                                textList = config.textList,
                                onChange = { onConfigChange(config.copy(textList = it)) },
                            )
                        }
                    }

                    LayoutResetButton(
                        label = "恢复默认",
                        onClick = { onConfigChange(config.resetLayout(layout)) },
                    )
                }
            }
        }
    }
}

private fun HomeLayout.toConfigTitle(): String = when (this) {
    HomeLayout.GRID -> "网格布局设置"
    HomeLayout.LOG -> "日志布局设置"
    HomeLayout.TIMELINE_REVERSE -> "时间轴布局设置"
    HomeLayout.MOMENT -> "当前时刻布局设置"
    HomeLayout.TEXT_LIST -> "纯文字列表设置"
}

@Composable
private fun GridConfigSection(grid: GridLayoutStyle, onChange: (GridLayoutStyle) -> Unit) {
    ConfigStepper(label = "列数", value = grid.columns, min = 1, max = 24,
        onValueChange = { onChange(grid.copy(columns = it)) })
    ConfigStepper(label = "最小行高", value = grid.minRowHeight, suffix = "dp", min = 0, max = 1000,
        onValueChange = { onChange(grid.copy(minRowHeight = it)) })
    ConfigStepper(label = "最大单元高", value = grid.maxCellHeight, suffix = "dp", min = 0, max = 1000,
        onValueChange = { onChange(grid.copy(maxCellHeight = it)) })
    ConfigStepper(label = "列间距", value = grid.columnSpacing, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(grid.copy(columnSpacing = it)) })
    ConfigStepper(label = "内边距", value = grid.cellPadding, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(grid.copy(cellPadding = it)) })
    ConfigStepper(label = "图标大小", value = grid.iconSize, suffix = "dp", min = 1, max = 256,
        onValueChange = { onChange(grid.copy(iconSize = it)) })
    ConfigStepper(label = "标签缩放", value = (grid.tagScale * 10).toInt(), suffix = "/10", min = 1, max = 30,
        onValueChange = { onChange(grid.copy(tagScale = it / 10f)) })
    ConfigStepper(label = "标签间距", value = grid.tagSpacing, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(grid.copy(tagSpacing = it)) })
    ConfigStepper(label = "激活背景透明度", value = (grid.activeBgAlpha * 100).toInt(), suffix = "%",
        min = 0, max = 100, step = 5,
        onValueChange = { onChange(grid.copy(activeBgAlpha = it / 100f)) })
}

@Composable
private fun LogConfigSection(log: LogLayoutStyle, onChange: (LogLayoutStyle) -> Unit) {
    ConfigStepper(label = "卡片内边距", value = log.cardPadding, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(log.copy(cardPadding = it)) })
    ConfigStepper(label = "图标大小", value = log.iconSize, suffix = "dp", min = 1, max = 256,
        onValueChange = { onChange(log.copy(iconSize = it)) })
    ConfigStepper(label = "图标间距", value = log.iconSpacing, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(log.copy(iconSpacing = it)) })
    ConfigStepper(label = "标签行间距", value = log.tagRowSpacing, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(log.copy(tagRowSpacing = it)) })
}

@Composable
private fun TimelineConfigSection(timeline: TimelineLayoutStyle, onChange: (TimelineLayoutStyle) -> Unit) {
    ConfigStepper(label = "条目间距", value = timeline.itemSpacing, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(timeline.copy(itemSpacing = it)) })
}

@Composable
private fun MomentConfigSection(moment: MomentLayoutStyle, onChange: (MomentLayoutStyle) -> Unit) {
    ConfigStepper(label = "卡片内边距", value = moment.cardPadding, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(moment.copy(cardPadding = it)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextListConfigSection(textList: TextListLayoutStyle, onChange: (TextListLayoutStyle) -> Unit) {
    ConfigStepper(label = "行间距", value = textList.rowSpacing, suffix = "dp", min = 0, max = 100,
        onValueChange = { onChange(textList.copy(rowSpacing = it)) })
    ConfigStepper(label = "字段间距", value = textList.fieldSpacing, suffix = "dp", min = 0, max = 100,
        onValueChange = { onChange(textList.copy(fieldSpacing = it)) })
    ConfigStepper(label = "水平内边距", value = textList.paddingH, suffix = "dp", min = 0, max = 200,
        onValueChange = { onChange(textList.copy(paddingH = it)) })
    ConfigStepper(label = "垂直内边距", value = textList.paddingV, suffix = "dp", min = 0, max = 100,
        onValueChange = { onChange(textList.copy(paddingV = it)) })
    ConfigStepper(label = "全局字体缩放", value = (textList.globalFontScale * 100).toInt(), suffix = "%",
        min = 50, max = 200, step = 5,
        onValueChange = { onChange(textList.copy(globalFontScale = it / 100f)) })

    Spacer(Modifier.height(8.dp))

    var separatorText by remember(textList.separator) { mutableStateOf(textList.separator) }
    OutlinedTextField(
        value = separatorText,
        onValueChange = { separatorText = it; onChange(textList.copy(separator = it)) },
        label = { Text("分隔符") },
        singleLine = true,
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(8.dp))

    var colModeExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = colModeExpanded,
        onExpandedChange = { colModeExpanded = !colModeExpanded },
    ) {
        OutlinedTextField(
            value = when (textList.columnMode) {
                TextListColumnMode.FLOW -> "流式布局"
                TextListColumnMode.TABLE -> "表格对齐"
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colModeExpanded) },
            label = { Text("列模式") },
            shape = FieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = colModeExpanded, onDismissRequest = { colModeExpanded = false }) {
            DropdownMenuItem(text = { Text("流式布局") }, onClick = { onChange(textList.copy(columnMode = TextListColumnMode.FLOW)); colModeExpanded = false })
            DropdownMenuItem(text = { Text("表格对齐") }, onClick = { onChange(textList.copy(columnMode = TextListColumnMode.TABLE)); colModeExpanded = false })
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        "字段配置",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.height(4.dp))

    textList.fieldConfigs.forEachIndexed { index, fieldConfig ->
        TextListDialogFieldRow(
            fieldConfig = fieldConfig,
            canMoveUp = index > 0,
            canMoveDown = index < textList.fieldConfigs.lastIndex,
            onToggleVisible = {
                val newConfigs = textList.fieldConfigs.toMutableList()
                newConfigs[index] = fieldConfig.copy(visible = !fieldConfig.visible)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
            onToggleBold = {
                val newConfigs = textList.fieldConfigs.toMutableList()
                newConfigs[index] = fieldConfig.copy(bold = !fieldConfig.bold)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
            onToggleItalic = {
                val newConfigs = textList.fieldConfigs.toMutableList()
                newConfigs[index] = fieldConfig.copy(italic = !fieldConfig.italic)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
            onFontScaleChange = { newScale ->
                val newConfigs = textList.fieldConfigs.toMutableList()
                newConfigs[index] = fieldConfig.copy(fontScale = newScale)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
            onColorChange = { newColor ->
                val newConfigs = textList.fieldConfigs.toMutableList()
                newConfigs[index] = fieldConfig.copy(colorMode = newColor)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
            onMoveUp = {
                val newConfigs = textList.fieldConfigs.toMutableList()
                val item = newConfigs.removeAt(index)
                newConfigs.add(index - 1, item)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
            onMoveDown = {
                val newConfigs = textList.fieldConfigs.toMutableList()
                val item = newConfigs.removeAt(index)
                newConfigs.add(index + 1, item)
                onChange(textList.copy(fieldConfigs = newConfigs))
            },
        )
    }
}

private fun TextListFieldType.displayName(): String = when (this) {
    TextListFieldType.NAME -> "名称"
    TextListFieldType.TIME_RANGE -> "时间"
    TextListFieldType.DURATION -> "用时"
    TextListFieldType.TAGS -> "标签"
    TextListFieldType.STATUS -> "状态"
    TextListFieldType.NOTE -> "备注"
    TextListFieldType.POMODORO -> "番茄钟"
    TextListFieldType.ESTIMATED -> "预估"
    TextListFieldType.ACHIEVEMENT -> "完成度"
    TextListFieldType.PLANNED -> "计划内"
    TextListFieldType.ICON -> "图标"
}

private fun TextListFieldColorMode.displayName(): String = when (this) {
    TextListFieldColorMode.DEFAULT -> "默认"
    TextListFieldColorMode.PRIMARY -> "主色"
    TextListFieldColorMode.SECONDARY -> "次色"
    TextListFieldColorMode.TERTIARY -> "三色"
    TextListFieldColorMode.ERROR -> "错误"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextListDialogFieldRow(
    fieldConfig: TextListFieldConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleVisible: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onColorChange: (TextListFieldColorMode) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val cardColor by animateColorAsState(
        if (fieldConfig.visible) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
        label = "fieldCard",
    )

    Surface(
        shape = FieldShape,
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .clickable { onToggleVisible() }
                    .padding(vertical = 4.dp),
            ) {
                Switch(
                    checked = fieldConfig.visible,
                    onCheckedChange = { onToggleVisible() },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fieldConfig.field.displayName(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
                if (canMoveUp) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "上移", modifier = Modifier.size(14.dp))
                    }
                }
                if (canMoveDown) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "下移", modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (fieldConfig.visible) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Surface(
                        onClick = onToggleBold,
                        shape = RoundedCornerShape(6.dp),
                        color = if (fieldConfig.bold) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            "B",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onToggleItalic,
                        shape = RoundedCornerShape(6.dp),
                        color = if (fieldConfig.italic) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            "I",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontStyle = FontStyle.Italic,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    var colorExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = colorExpanded, onExpandedChange = { colorExpanded = !colorExpanded }) {
                        OutlinedTextField(
                            value = fieldConfig.colorMode.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                            shape = FieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .height(40.dp),
                            textStyle = MaterialTheme.typography.labelSmall,
                        )
                        ExposedDropdownMenu(expanded = colorExpanded, onDismissRequest = { colorExpanded = false }) {
                            TextListFieldColorMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.displayName()) }, onClick = { onColorChange(mode); colorExpanded = false })
                            }
                        }
                    }
                }
                ConfigStepper(label = "字号", value = (fieldConfig.fontScale * 100).toInt(), suffix = "%",
                    min = 50, max = 200, step = 5,
                    onValueChange = { onFontScaleChange(it / 100f) })
            }
        }
    }
}
