package com.nltimer.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TextListFieldConfig
import com.nltimer.core.data.model.TextListFieldColorMode
import com.nltimer.core.data.model.TextListFieldType
import com.nltimer.core.data.model.TextListLayoutStyle
import com.nltimer.core.data.model.TextListColumnMode
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.component.ConfigStepper
import com.nltimer.core.designsystem.component.ExpandableCard
import com.nltimer.core.designsystem.component.LayoutResetButton

@Composable
fun HomeLayoutConfigRoute(
    viewModel: DialogConfigViewModel = hiltViewModel(),
) {
    val config by viewModel.homeLayoutConfig.collectAsStateWithLifecycle()
    HomeLayoutConfigScreen(
        config = config,
        onUpdateConfig = viewModel::updateHomeLayoutConfig,
    )
}

@Composable
fun HomeLayoutConfigScreen(
    config: HomeLayoutConfig,
    onUpdateConfig: (HomeLayoutConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedGrid by remember { mutableStateOf(false) }
    var expandedLog by remember { mutableStateOf(false) }
    var expandedTimeline by remember { mutableStateOf(false) }
    var expandedMoment by remember { mutableStateOf(false) }
    var expandedTextList by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ExpandableCard(
                    title = "网格布局",
                    expanded = expandedGrid,
                    onToggle = { expandedGrid = !expandedGrid },
                ) {
                    ConfigStepper(
                        label = "列数",
                        value = config.grid.columns,
                        min = 1, max = 24,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(columns = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "最小行高",
                        value = config.grid.minRowHeight,
                        suffix = "dp",
                        min = 0, max = 1000,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(minRowHeight = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "最大单元高",
                        value = config.grid.maxCellHeight,
                        suffix = "dp",
                        min = 0, max = 1000,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(maxCellHeight = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "列间距",
                        value = config.grid.columnSpacing,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(columnSpacing = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "内边距",
                        value = config.grid.cellPadding,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(cellPadding = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "图标大小",
                        value = config.grid.iconSize,
                        suffix = "dp",
                        min = 1, max = 256,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(iconSize = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "标签缩放",
                        value = (config.grid.tagScale * 10).toInt(),
                        suffix = "/10",
                        min = 1, max = 30,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(tagScale = it / 10f))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "标签间距",
                        value = config.grid.tagSpacing,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(tagSpacing = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "激活背景透明度",
                        value = (config.grid.activeBgAlpha * 100).toInt(),
                        suffix = "%",
                        min = 0, max = 100, step = 5,
                        onValueChange = { onUpdateConfig(config.copy(grid = config.grid.copy(activeBgAlpha = it / 100f))) },
                    )
                    LayoutResetButton(
                        label = "恢复网格默认",
                        onClick = { onUpdateConfig(config.copy(grid = GridLayoutStyle())) },
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "日志布局",
                    expanded = expandedLog,
                    onToggle = { expandedLog = !expandedLog },
                ) {
                    ConfigStepper(
                        label = "卡片内边距",
                        value = config.log.cardPadding,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(log = config.log.copy(cardPadding = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "图标大小",
                        value = config.log.iconSize,
                        suffix = "dp",
                        min = 1, max = 256,
                        onValueChange = { onUpdateConfig(config.copy(log = config.log.copy(iconSize = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "图标间距",
                        value = config.log.iconSpacing,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(log = config.log.copy(iconSpacing = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "标签行间距",
                        value = config.log.tagRowSpacing,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(log = config.log.copy(tagRowSpacing = it))) },
                    )
                    LayoutResetButton(
                        label = "恢复日志默认",
                        onClick = { onUpdateConfig(config.copy(log = LogLayoutStyle())) },
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "时间轴布局",
                    expanded = expandedTimeline,
                    onToggle = { expandedTimeline = !expandedTimeline },
                ) {
                    ConfigStepper(
                        label = "条目间距",
                        value = config.timeline.itemSpacing,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = {
                            onUpdateConfig(config.copy(timeline = config.timeline.copy(itemSpacing = it)))
                        },
                    )
                    LayoutResetButton(
                        label = "恢复时间轴默认",
                        onClick = { onUpdateConfig(config.copy(timeline = TimelineLayoutStyle())) },
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "当前时刻布局",
                    expanded = expandedMoment,
                    onToggle = { expandedMoment = !expandedMoment },
                ) {
                    ConfigStepper(
                        label = "卡片内边距",
                        value = config.moment.cardPadding,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = {
                            onUpdateConfig(config.copy(moment = config.moment.copy(cardPadding = it)))
                        },
                    )
                    LayoutResetButton(
                        label = "恢复当前时刻默认",
                        onClick = { onUpdateConfig(config.copy(moment = MomentLayoutStyle())) },
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "纯文字列表布局",
                    expanded = expandedTextList,
                    onToggle = { expandedTextList = !expandedTextList },
                ) {
                    ConfigStepper(
                        label = "行间距",
                        value = config.textList.rowSpacing,
                        suffix = "dp",
                        min = 0, max = 100,
                        onValueChange = { onUpdateConfig(config.copy(textList = config.textList.copy(rowSpacing = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "字段间距",
                        value = config.textList.fieldSpacing,
                        suffix = "dp",
                        min = 0, max = 100,
                        onValueChange = { onUpdateConfig(config.copy(textList = config.textList.copy(fieldSpacing = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "水平内边距",
                        value = config.textList.paddingH,
                        suffix = "dp",
                        min = 0, max = 200,
                        onValueChange = { onUpdateConfig(config.copy(textList = config.textList.copy(paddingH = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "垂直内边距",
                        value = config.textList.paddingV,
                        suffix = "dp",
                        min = 0, max = 100,
                        onValueChange = { onUpdateConfig(config.copy(textList = config.textList.copy(paddingV = it))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigStepper(
                        label = "全局字体缩放",
                        value = (config.textList.globalFontScale * 100).toInt(),
                        suffix = "%",
                        min = 50, max = 200, step = 5,
                        onValueChange = { onUpdateConfig(config.copy(textList = config.textList.copy(globalFontScale = it / 100f))) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var separatorText by remember(config.textList.separator) { mutableStateOf(config.textList.separator) }
                    OutlinedTextField(
                        value = separatorText,
                        onValueChange = { separatorText = it; onUpdateConfig(config.copy(textList = config.textList.copy(separator = it))) },
                        label = { Text("分隔符") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextListColumnModePicker(
                        current = config.textList.columnMode,
                        onChange = { onUpdateConfig(config.copy(textList = config.textList.copy(columnMode = it))) },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "字段配置",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    config.textList.fieldConfigs.forEachIndexed { index, fieldConfig ->
                        TextListFieldConfigRow(
                            fieldConfig = fieldConfig,
                            canMoveUp = index > 0,
                            canMoveDown = index < config.textList.fieldConfigs.lastIndex,
                            onToggleVisible = {
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                newConfigs[index] = fieldConfig.copy(visible = !fieldConfig.visible)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                            onToggleBold = {
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                newConfigs[index] = fieldConfig.copy(bold = !fieldConfig.bold)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                            onToggleItalic = {
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                newConfigs[index] = fieldConfig.copy(italic = !fieldConfig.italic)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                            onFontScaleChange = { newScale ->
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                newConfigs[index] = fieldConfig.copy(fontScale = newScale)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                            onColorChange = { newColor ->
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                newConfigs[index] = fieldConfig.copy(colorMode = newColor)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                            onMoveUp = {
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                val item = newConfigs.removeAt(index)
                                newConfigs.add(index - 1, item)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                            onMoveDown = {
                                val newConfigs = config.textList.fieldConfigs.toMutableList()
                                val item = newConfigs.removeAt(index)
                                newConfigs.add(index + 1, item)
                                onUpdateConfig(config.copy(textList = config.textList.copy(fieldConfigs = newConfigs)))
                            },
                        )
                    }

                    LayoutResetButton(
                        label = "恢复纯文字列表默认",
                        onClick = { onUpdateConfig(config.copy(textList = TextListLayoutStyle())) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = { onUpdateConfig(HomeLayoutConfig()) }) {
                        Text("恢复全部默认")
                    }
                }
            }
        }
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
private fun TextListColumnModePicker(current: TextListColumnMode, onChange: (TextListColumnMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = when (current) {
                TextListColumnMode.FLOW -> "流式布局"
                TextListColumnMode.TABLE -> "表格对齐"
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            label = { Text("列模式") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("流式布局") }, onClick = { onChange(TextListColumnMode.FLOW); expanded = false })
            DropdownMenuItem(text = { Text("表格对齐") }, onClick = { onChange(TextListColumnMode.TABLE); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextListFieldConfigRow(
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Switch(
                checked = fieldConfig.visible,
                onCheckedChange = { onToggleVisible() },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = fieldConfig.field.displayName(),
                style = MaterialTheme.typography.bodyMedium,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "B", style = MaterialTheme.typography.labelSmall)
                Checkbox(checked = fieldConfig.bold, onCheckedChange = { onToggleBold() })
                Text(text = "I", style = MaterialTheme.typography.labelSmall)
                Checkbox(checked = fieldConfig.italic, onCheckedChange = { onToggleItalic() })
                Spacer(modifier = Modifier.width(8.dp))
                var colorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = colorExpanded, onExpandedChange = { colorExpanded = !colorExpanded }) {
                    OutlinedTextField(
                        value = fieldConfig.colorMode.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).height(40.dp),
                        textStyle = MaterialTheme.typography.labelSmall,
                    )
                    ExposedDropdownMenu(expanded = colorExpanded, onDismissRequest = { colorExpanded = false }) {
                        TextListFieldColorMode.entries.forEach { mode ->
                            DropdownMenuItem(text = { Text(mode.displayName()) }, onClick = { onColorChange(mode); colorExpanded = false })
                        }
                    }
                }
            }
            ConfigStepper(
                label = "字号缩放",
                value = (fieldConfig.fontScale * 100).toInt(),
                suffix = "%",
                min = 50, max = 200, step = 5,
                onValueChange = { onFontScaleChange(it / 100f) },
            )
        }
    }
}
