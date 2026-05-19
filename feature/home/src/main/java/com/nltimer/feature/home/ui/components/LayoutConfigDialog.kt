package com.nltimer.feature.home.ui.components

import android.view.WindowManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.component.ConfigStepper
import com.nltimer.core.designsystem.component.LayoutResetButton
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.feature.home.model.resetLayout

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
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.70f),
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
