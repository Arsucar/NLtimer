package com.nltimer.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.SecondsStrategy
import com.nltimer.core.designsystem.component.ExpandableCard
import com.nltimer.core.designsystem.component.atom.SelectableOptionChip
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode

@Composable
fun DialogConfigRoute(
    viewModel: DialogConfigViewModel = hiltViewModel(),
) {
    val config by viewModel.dialogConfig.collectAsStateWithLifecycle()
    DialogConfigScreen(
        config = config,
        onUpdateConfig = viewModel::updateConfig,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogConfigScreen(
    config: DialogGridConfig,
    onUpdateConfig: (DialogGridConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedActivity by remember { mutableStateOf(false) }
    var expandedTag by remember { mutableStateOf(false) }
    var expandedOther by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExpandableCard(
                title = "活动配置",
                expanded = expandedActivity,
                onToggle = { expandedActivity = !expandedActivity },
            ) {
                ChipFlowSelector(
                    label = "样式",
                    options = ChipDisplayMode.entries,
                    selected = config.activityDisplayMode,
                    display = { it.displayName() },
                    onSelect = { onUpdateConfig(config.copy(activityDisplayMode = it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                LayoutWithStepperRow(
                    layoutOptions = GridLayoutMode.entries,
                    selectedLayout = config.activityLayoutMode,
                    displayLayout = { it.displayName() },
                    onSelectLayout = { onUpdateConfig(config.copy(activityLayoutMode = it)) },
                    stepperLabel = if (config.activityLayoutMode == GridLayoutMode.Vertical) "行数" else "行数",
                    stepperValue = if (config.activityLayoutMode == GridLayoutMode.Vertical) config.activityColumnLines else config.activityHorizontalLines,
                    stepperMin = if (config.activityLayoutMode == GridLayoutMode.Vertical) 1 else 0,
                    stepperMax = 10,
                    infiniteAtMin = config.activityLayoutMode == GridLayoutMode.Horizontal,
                    onStepperChange = {
                        if (config.activityLayoutMode == GridLayoutMode.Vertical) {
                            onUpdateConfig(config.copy(activityColumnLines = it))
                        } else {
                            onUpdateConfig(config.copy(activityHorizontalLines = it))
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                InlineToggleRow(
                    label = "配色",
                    options = listOf("强调色" to false, "活动色" to true),
                    selected = config.activityUseColorForText,
                    onSelect = { onUpdateConfig(config.copy(activityUseColorForText = it)) },
                )
            }
        }

        item {
            ExpandableCard(
                title = "标签配置",
                expanded = expandedTag,
                onToggle = { expandedTag = !expandedTag },
            ) {
                ChipFlowSelector(
                    label = "样式",
                    options = ChipDisplayMode.entries,
                    selected = config.tagDisplayMode,
                    display = { it.displayName() },
                    onSelect = { onUpdateConfig(config.copy(tagDisplayMode = it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                LayoutWithStepperRow(
                    layoutOptions = GridLayoutMode.entries,
                    selectedLayout = config.tagLayoutMode,
                    displayLayout = { it.displayName() },
                    onSelectLayout = { onUpdateConfig(config.copy(tagLayoutMode = it)) },
                    stepperLabel = if (config.tagLayoutMode == GridLayoutMode.Vertical) "行数" else "行数",
                    stepperValue = if (config.tagLayoutMode == GridLayoutMode.Vertical) config.tagColumnLines else config.tagHorizontalLines,
                    stepperMin = if (config.tagLayoutMode == GridLayoutMode.Vertical) 1 else 0,
                    stepperMax = 10,
                    infiniteAtMin = config.tagLayoutMode == GridLayoutMode.Horizontal,
                    onStepperChange = {
                        if (config.tagLayoutMode == GridLayoutMode.Vertical) {
                            onUpdateConfig(config.copy(tagColumnLines = it))
                        } else {
                            onUpdateConfig(config.copy(tagHorizontalLines = it))
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                InlineToggleRow(
                    label = "配色",
                    options = listOf("强调色" to false, "活动色" to true),
                    selected = config.tagUseColorForText,
                    onSelect = { onUpdateConfig(config.copy(tagUseColorForText = it)) },
                )
            }
        }

        item {
            ExpandableCard(
                title = "其他",
                expanded = expandedOther,
                onToggle = { expandedOther = !expandedOther },
            ) {
                InlineToggleRow(
                    label = "行为选择器",
                    options = listOf("隐藏" to false, "显示" to true),
                    selected = config.showBehaviorNature,
                    onSelect = { onUpdateConfig(config.copy(showBehaviorNature = it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChipFlowSelector(
                    label = "路径动画",
                    options = PathDrawMode.entries,
                    selected = config.pathDrawMode,
                    display = { it.displayName() },
                    onSelect = { onUpdateConfig(config.copy(pathDrawMode = it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChipFlowSelector(
                    label = "秒数策略",
                    options = SecondsStrategy.entries,
                    selected = config.secondsStrategy,
                    display = { it.displayName() },
                    onSelect = { onUpdateConfig(config.copy(secondsStrategy = it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                InlineToggleRow(
                    label = "智能识别",
                    options = listOf("关闭" to false, "自动" to true),
                    selected = config.autoMatchNote,
                    onSelect = { onUpdateConfig(config.copy(autoMatchNote = it)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipFlowSelector(
    label: String,
    options: List<T>,
    selected: T,
    display: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            SelectableOptionChip(
                text = display(option),
                selected = option == selected,
                onSelect = { onSelect(option) },
                shape = RoundedCornerShape(8.dp),
                showCheckIcon = false,
                showBorder = false,
                textStyle = MaterialTheme.typography.labelMedium,
                contentPaddingVertical = 5.dp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> LayoutWithStepperRow(
    layoutOptions: List<T>,
    selectedLayout: T,
    displayLayout: (T) -> String,
    onSelectLayout: (T) -> Unit,
    stepperLabel: String,
    stepperValue: Int,
    stepperMin: Int,
    stepperMax: Int,
    infiniteAtMin: Boolean = false,
    onStepperChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "布局",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            layoutOptions.forEach { option ->
                SelectableOptionChip(
                    text = displayLayout(option),
                    selected = option == selectedLayout,
                    onSelect = { onSelectLayout(option) },
                    shape = RoundedCornerShape(8.dp),
                    showCheckIcon = false,
                    showBorder = false,
                    textStyle = MaterialTheme.typography.labelMedium,
                    contentPaddingVertical = 5.dp,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stepperLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                onClick = { if (stepperValue > stepperMin) onStepperChange(stepperValue - 1) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = "\u2212",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            Text(
                text = if (infiniteAtMin && stepperValue == stepperMin) "\u221E" else "$stepperValue",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Surface(
                onClick = {
                    if (stepperValue < stepperMax) onStepperChange(stepperValue + 1)
                    else if (infiniteAtMin) onStepperChange(stepperMin)
                },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun InlineToggleRow(
    label: String,
    options: List<Pair<String, Boolean>>,
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        options.forEach { (text, value) ->
            SelectableOptionChip(
                text = text,
                selected = selected == value,
                onSelect = { onSelect(value) },
                shape = RoundedCornerShape(8.dp),
                showCheckIcon = false,
                showBorder = false,
                textStyle = MaterialTheme.typography.labelMedium,
                contentPaddingVertical = 5.dp,
            )
        }
    }
}

private fun ChipDisplayMode.displayName() = when (this) {
    ChipDisplayMode.None -> "无"
    ChipDisplayMode.Filled -> "填充"
    ChipDisplayMode.Underline -> "下划线"
    ChipDisplayMode.Capsules -> "胶囊"
    ChipDisplayMode.RoundedCorners -> "圆角"
    ChipDisplayMode.Squares -> "方块"
    ChipDisplayMode.SquareBorder -> "方框"
    ChipDisplayMode.HandDrawn -> "手绘"
    ChipDisplayMode.DashedLines -> "虚线"
}

private fun GridLayoutMode.displayName() = when (this) {
    GridLayoutMode.Horizontal -> "水平"
    GridLayoutMode.Vertical -> "垂直"
}

private fun PathDrawMode.displayName() = when (this) {
    PathDrawMode.StartToEnd -> "单向"
    PathDrawMode.BothSidesToMiddle -> "双向"
    PathDrawMode.Random -> "随机"
    PathDrawMode.None -> "无"
    PathDrawMode.WrigglingMaggot -> "蛆动"
}

private fun SecondsStrategy.displayName() = when (this) {
    SecondsStrategy.OPEN_TIME -> "打开时"
    SecondsStrategy.CONFIRM_TIME -> "确认时"
}
