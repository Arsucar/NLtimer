package com.nltimer.feature.management_activities.ui.components.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.behaviorui.sheet.ActivityGroupCategorizable
import com.nltimer.core.behaviorui.sheet.CategoryGroup
import com.nltimer.core.behaviorui.sheet.CategoryPickerDialog

/**
 * 移动活动到分组弹窗
 *
 * 将指定活动移动到目标分组，支持移回"未分类"。
 *
 * @param activity 待移动的活动
 * @param allGroups 全部分组列表
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认移动回调，参数为目标分组 ID（null 表示未分类）
 */
@Composable
fun MoveToGroupDialog(
    activity: Activity,
    allGroups: List<ActivityGroup>,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val groupItems = remember(allGroups) {
        allGroups.map { ActivityGroupCategorizable(it) }
    }
    val groupedGroups = remember(groupItems) {
        listOf(
            CategoryGroup(
                id = 0L,
                name = "所有分组",
                items = groupItems,
                onClear = {
                    onConfirm(null)
                },
                clearLabel = "清除",
            )
        )
    }

    CategoryPickerDialog(
        title = "移动活动「${activity.name}」到：",
        items = groupItems,
        categoryGroups = groupedGroups,
        selectedId = activity.groupId ?: 0L,
        onItemSelected = { id ->
            onConfirm(id)
        },
        onDismiss = onDismiss,
        showHeader = false,
    )
}
