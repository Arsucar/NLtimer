# 主页布局配置弹窗化 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在主页右下角 `BottomBarDragFab` 拖拽菜单中根据当前布局新增一个「<布局名>设置」选项，点击后居中弹出半透明配置面板，调整数值即时刷新主页并持久化。

**架构：** 抽出公共 `ConfigStepper` 到 `core/designsystem`；新增 `LayoutConfigDialog`（按 layout 渲染对应字段组），数据回写复用 `onHomeLayoutConfigChange` 现有通路；`HomeScreen` 新增本地状态控制弹窗显隐。保留 `feature/settings/HomeLayoutConfigScreen` 作为完整编辑入口（与弹窗读写同源）。

**技术栈：** Kotlin / Jetpack Compose / Material3 / Hilt / JUnit4（单元测试）

**关联设计：** `docs/superpowers/specs/2026-05-17-home-layout-config-dialog-design.md`

---

## 文件结构

| 文件 | 操作 | 职责 |
|---|---|---|
| `core/designsystem/src/main/java/com/nltimer/core/designsystem/component/ConfigStepper.kt` | 新建 | `±` + 数字弹框输入的步进器，所有布局配置项共享 |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/components/LayoutConfigDialog.kt` | 新建 | 半透明居中配置弹窗 + 4 个布局子区段 |
| `feature/home/src/main/java/com/nltimer/feature/home/model/LayoutConfigReset.kt` | 新建 | 纯函数：按 layout 重置 `HomeLayoutConfig` 对应字段为默认值（便于单元测试） |
| `feature/home/src/test/java/com/nltimer/feature/home/model/LayoutConfigResetTest.kt` | 新建 | `LayoutConfigReset` 单元测试 |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/HomeScreen.kt` | 修改 | DragFab `dragOptions` 末尾追加布局相关项；新增弹窗状态与渲染；`onOptionSelected` 增加分支 |
| `feature/settings/src/main/java/com/nltimer/feature/settings/ui/HomeLayoutConfigScreen.kt` | 修改 | 删除内部私有 `ConfigStepper`，改为 `import com.nltimer.core.designsystem.component.ConfigStepper` |

---

## 任务 1：抽出 `ConfigStepper` 到 designsystem

**文件：**
- 创建：`core/designsystem/src/main/java/com/nltimer/core/designsystem/component/ConfigStepper.kt`
- 修改：`feature/settings/src/main/java/com/nltimer/feature/settings/ui/HomeLayoutConfigScreen.kt`（删除私有 `ConfigStepper` + `LayoutResetButton`、改 import）

`LayoutResetButton` 也一并迁移（弹窗也会用，避免重复实现）。

- [ ] **步骤 1.1：创建 `ConfigStepper.kt`**

写入文件 `core/designsystem/src/main/java/com/nltimer/core/designsystem/component/ConfigStepper.kt`：

```kotlin
package com.nltimer.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ConfigStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    suffix: String = "",
) {
    var showInputDialog by remember { mutableStateOf(false) }
    var inputText by remember(value) { mutableStateOf(value.toString()) }
    val rangeText = "$min-$max$suffix"

    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { text ->
                        inputText = text.filterIndexed { index, char ->
                            char.isDigit() || (char == '-' && index == 0)
                        }
                    },
                    singleLine = true,
                    label = { Text("输入数值") },
                    supportingText = { Text("范围 $rangeText") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        inputText.toIntOrNull()?.let { parsed ->
                            onValueChange(parsed.coerceIn(min, max))
                        }
                        showInputDialog = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) { Text("取消") }
            },
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Surface(
            onClick = { onValueChange((value - step).coerceAtLeast(min)) },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Surface(
            onClick = {
                inputText = value.toString()
                showInputDialog = true
            },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.weight(0.25f),
        ) {
            Text(
                text = "$value$suffix",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
        Surface(
            onClick = { onValueChange((value + step).coerceAtMost(max)) },
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

@Composable
fun LayoutResetButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onClick) { Text(label) }
    }
}
```

- [ ] **步骤 1.2：删除 `HomeLayoutConfigScreen.kt` 中的私有 `ConfigStepper` 与 `LayoutResetButton`**

修改 `feature/settings/src/main/java/com/nltimer/feature/settings/ui/HomeLayoutConfigScreen.kt`：
1. 删除文件末尾的 `private fun ConfigStepper(...)`（约 257-358 行）
2. 删除 `private fun LayoutResetButton(...)`（约 360-373 行）
3. 在 import 段添加：
   ```kotlin
   import com.nltimer.core.designsystem.component.ConfigStepper
   import com.nltimer.core.designsystem.component.LayoutResetButton
   ```
4. 删除已不再被引用的 import（如 `AlertDialog`、`OutlinedTextField`、`KeyboardOptions`、`KeyboardType`、`Surface`、`FontWeight`、`TextAlign`、`RoundedCornerShape` 等仅服务于这两个私有函数的 import）

依靠下一步的编译输出指引清理 import。

- [ ] **步骤 1.3：编译并验证设置页未破**

运行：`./gradlew :feature:settings:compileDebugKotlin :core:designsystem:compileDebugKotlin -q`
预期：BUILD SUCCESSFUL

如有未删干净的 import 警告/错误，按错误信息逐一删除。

- [ ] **步骤 1.4：Commit**

```bash
git add core/designsystem/src/main/java/com/nltimer/core/designsystem/component/ConfigStepper.kt \
        feature/settings/src/main/java/com/nltimer/feature/settings/ui/HomeLayoutConfigScreen.kt
git commit -m "refactor(designsystem): 抽出 ConfigStepper / LayoutResetButton 为公共组件

主页布局配置弹窗将复用同一组件，避免双份实现。"
```

---

## 任务 2：纯函数 `LayoutConfigReset`（TDD）

提取「按布局重置默认值」逻辑到纯函数，便于单元测试，弹窗 UI 调用即可。

**文件：**
- 创建：`feature/home/src/main/java/com/nltimer/feature/home/model/LayoutConfigReset.kt`
- 测试：`feature/home/src/test/java/com/nltimer/feature/home/model/LayoutConfigResetTest.kt`

- [ ] **步骤 2.1：先写失败的测试**

创建 `feature/home/src/test/java/com/nltimer/feature/home/model/LayoutConfigResetTest.kt`：

```kotlin
package com.nltimer.feature.home.model

import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.theme.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutConfigResetTest {

    private val customized = HomeLayoutConfig(
        grid = GridLayoutStyle(columns = 8, minRowHeight = 200),
        log = LogLayoutStyle(cardPadding = 24),
        timeline = TimelineLayoutStyle(itemSpacing = 24),
        moment = MomentLayoutStyle(cardPadding = 32),
    )

    @Test
    fun `reset GRID only resets grid block`() {
        val result = customized.resetLayout(HomeLayout.GRID)
        assertEquals(GridLayoutStyle(), result.grid)
        assertEquals(customized.log, result.log)
        assertEquals(customized.timeline, result.timeline)
        assertEquals(customized.moment, result.moment)
    }

    @Test
    fun `reset LOG only resets log block`() {
        val result = customized.resetLayout(HomeLayout.LOG)
        assertEquals(customized.grid, result.grid)
        assertEquals(LogLayoutStyle(), result.log)
        assertEquals(customized.timeline, result.timeline)
        assertEquals(customized.moment, result.moment)
    }

    @Test
    fun `reset TIMELINE_REVERSE only resets timeline block`() {
        val result = customized.resetLayout(HomeLayout.TIMELINE_REVERSE)
        assertEquals(customized.grid, result.grid)
        assertEquals(customized.log, result.log)
        assertEquals(TimelineLayoutStyle(), result.timeline)
        assertEquals(customized.moment, result.moment)
    }

    @Test
    fun `reset MOMENT only resets moment block`() {
        val result = customized.resetLayout(HomeLayout.MOMENT)
        assertEquals(customized.grid, result.grid)
        assertEquals(customized.log, result.log)
        assertEquals(customized.timeline, result.timeline)
        assertEquals(MomentLayoutStyle(), result.moment)
    }
}
```

- [ ] **步骤 2.2：运行测试确认失败**

运行：`./gradlew :feature:home:testDebugUnitTest --tests "com.nltimer.feature.home.model.LayoutConfigResetTest" -q`
预期：FAIL，报 `Unresolved reference: resetLayout`

- [ ] **步骤 2.3：创建实现**

写入 `feature/home/src/main/java/com/nltimer/feature/home/model/LayoutConfigReset.kt`：

```kotlin
package com.nltimer.feature.home.model

import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.theme.HomeLayout

fun HomeLayoutConfig.resetLayout(layout: HomeLayout): HomeLayoutConfig = when (layout) {
    HomeLayout.GRID -> copy(grid = GridLayoutStyle())
    HomeLayout.LOG -> copy(log = LogLayoutStyle())
    HomeLayout.TIMELINE_REVERSE -> copy(timeline = TimelineLayoutStyle())
    HomeLayout.MOMENT -> copy(moment = MomentLayoutStyle())
}
```

- [ ] **步骤 2.4：运行测试确认通过**

运行：`./gradlew :feature:home:testDebugUnitTest --tests "com.nltimer.feature.home.model.LayoutConfigResetTest" -q`
预期：BUILD SUCCESSFUL，4 个测试全部 PASS

- [ ] **步骤 2.5：Commit**

```bash
git add feature/home/src/main/java/com/nltimer/feature/home/model/LayoutConfigReset.kt \
        feature/home/src/test/java/com/nltimer/feature/home/model/LayoutConfigResetTest.kt
git commit -m "feat(home): 添加按布局重置默认值的纯函数 resetLayout"
```

---

## 任务 3：新增 `LayoutConfigDialog`

**文件：**
- 创建：`feature/home/src/main/java/com/nltimer/feature/home/ui/components/LayoutConfigDialog.kt`

- [ ] **步骤 3.1：创建弹窗文件**

写入 `feature/home/src/main/java/com/nltimer/feature/home/ui/components/LayoutConfigDialog.kt`：

```kotlin
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
        // 透明化默认 scrim，让底层主页可见
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
```

- [ ] **步骤 3.2：编译验证**

运行：`./gradlew :feature:home:compileDebugKotlin -q`
预期：BUILD SUCCESSFUL

- [ ] **步骤 3.3：Commit**

```bash
git add feature/home/src/main/java/com/nltimer/feature/home/ui/components/LayoutConfigDialog.kt
git commit -m "feat(home): 添加主页布局配置半透明弹窗 LayoutConfigDialog"
```

---

## 任务 4：在 `HomeScreen` 中接入弹窗

**文件：**
- 修改：`feature/home/src/main/java/com/nltimer/feature/home/ui/HomeScreen.kt`

- [ ] **步骤 4.1：补充 import**

在 `feature/home/src/main/java/com/nltimer/feature/home/ui/HomeScreen.kt` 顶部 import 区添加：

```kotlin
import com.nltimer.feature.home.ui.components.LayoutConfigDialog
```

- [ ] **步骤 4.2：新增弹窗状态**

在 `HomeScreen` 函数体内、`showTimeLabelSettings` 声明附近（约 124 行）加：

```kotlin
var configDialogLayout: HomeLayout? by remember { mutableStateOf(null) }
```

- [ ] **步骤 4.3：DragFab 选项追加**

将原本的：

```kotlin
val dragOptions = if (uiState.hasActiveBehavior) DragOptionsWithActive else DragOptionsWithoutActive
```

改为：

```kotlin
val layoutSettingsLabel = when (layout) {
    HomeLayout.GRID -> "网格设置"
    HomeLayout.TIMELINE_REVERSE -> "时间轴设置"
    HomeLayout.LOG -> "日志设置"
    HomeLayout.MOMENT -> "当前时刻设置"
}
val dragOptions = (if (uiState.hasActiveBehavior) DragOptionsWithActive else DragOptionsWithoutActive) + layoutSettingsLabel
```

- [ ] **步骤 4.4：`onOptionSelected` 增加分支**

定位现有 `onOptionSelected = { option -> when (option) { ... } }`（约 225-238 行），在 `when (option)` 的 `else` 分支之前插入：

```kotlin
layoutSettingsLabel -> configDialogLayout = layout
```

注意：Kotlin `when` 允许变量作为分支表达式。

- [ ] **步骤 4.5：渲染弹窗**

在外层 `Box` 末尾、`if (showTimeLabelSettings)` 块之后追加：

```kotlin
configDialogLayout?.let { dialogLayout ->
    LayoutConfigDialog(
        layout = dialogLayout,
        config = homeLayoutConfig,
        onConfigChange = onHomeLayoutConfigChange,
        onDismiss = { configDialogLayout = null },
    )
}
```

- [ ] **步骤 4.6：编译验证**

运行：`./gradlew :feature:home:compileDebugKotlin -q`
预期：BUILD SUCCESSFUL

- [ ] **步骤 4.7：Commit**

```bash
git add feature/home/src/main/java/com/nltimer/feature/home/ui/HomeScreen.kt
git commit -m "feat(home): DragFab 拖拽菜单追加当前布局配置入口

按当前 HomeLayout 在选项末尾注入「<布局名>设置」，选中后弹出半透明
LayoutConfigDialog，调整即时生效。"
```

---

## 任务 5：全量回归验证

- [ ] **步骤 5.1：跑全项目单元测试**

运行：`./gradlew testDebugUnitTest -q`
预期：BUILD SUCCESSFUL，无新失败

如有失败，对照失败信息修复（最有可能的是 `HomeLayoutConfigScreen` 残留 import 或 `HomeScreen` import 顺序）。

- [ ] **步骤 5.2：装机手测脚本**

运行：`./gradlew :app:installDebug` 后在设备上：
1. 切换到 GRID 布局 → 拖拽右下 FAB → 选「网格设置」→ 弹窗居中半透明出现 → 修改列数 → 主页网格列数立即变化 → 关闭弹窗 → 配置仍生效
2. 切到 LOG → 拖拽 FAB → 仅看到「日志设置」（而非"网格设置"）→ 验证只暴露当前布局相关项
3. TIMELINE_REVERSE 和 MOMENT 同样验证
4. 在弹窗中点「恢复默认」→ 当前布局重置，其它布局参数保留
5. 关闭弹窗后再进入 `设置 → 主页布局配置`，确认数值与刚才主页弹窗调整后的一致

- [ ] **步骤 5.3：PR 描述**

在最终 PR 描述中引用 `Closes #2`。

---

## 自检结果

- **规格覆盖度：**
  - §2.1 4 个布局对应 4 个弹窗 → 任务 3
  - §2.2 半透明背景实时预览 → 任务 3 步骤 3.1（`FLAG_DIM_BEHIND` + alpha 0.85）+ 任务 5.2 手测
  - §2.3 即时生效自动保存 → 任务 4 步骤 4.5（直接复用 `onHomeLayoutConfigChange`）
  - §2.4 不引入页面切换 → 全程使用 Dialog 覆盖式渲染
  - §3 设置页保留 → 任务 1 步骤 1.2 仅重构 import，不删除页面
  - §6.1 `ConfigStepper` 抽出 → 任务 1
  - §6.2 `LayoutConfigDialog` 设计 → 任务 3 完全对应
  - §6.3 `HomeScreen` 改造 → 任务 4 完全对应
  - §7 测试策略：单元覆盖 `resetLayout`（任务 2）；UI 行为靠手测（任务 5.2）

- **占位符扫描：** 无 TBD/TODO；所有代码块完整可粘贴。

- **类型一致性：** `HomeLayoutConfig` / `HomeLayout` / `resetLayout` / `LayoutResetButton` / `ConfigStepper` 在各任务间名称一致。

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-05-17-home-layout-config-dialog-plan.md`。两种执行方式：

1. **子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代
2. **内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

选哪种方式？
