package com.nltimer.feature.management_activities.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.DisplayColorConfig
import com.nltimer.core.data.model.Tag

/**
 * 活动管理页面的 UI 状态
 *
 * @property uncategorizedActivities 未归类的活动列表
 * @property groups 分组及其活动列表
 * @property allGroups 全部分组的简要列表
 * @property isLoading 是否正在加载
 * @property expandedGroupIds 当前展开的分组 ID 集合
 * @property dialogState 当前打开的弹窗状态，为 null 表示无弹窗
 */
@Immutable
data class ActivityManagementUiState(
    val uncategorizedActivities: List<Activity> = emptyList(),
    val groups: List<GroupWithActivities> = emptyList(),
    val allGroups: List<ActivityGroup> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val expandedGroupIds: Set<Long> = emptySet(),
    val dialogState: DialogState? = null,
    val displayColorConfig: DisplayColorConfig = DisplayColorConfig(),
)

/**
 * 分组及其关联的活动列表
 *
 * @property group 分组信息
 * @property activities 该分组下的活动列表
 */
@Immutable
data class GroupWithActivities(
    val group: ActivityGroup,
    val activities: List<Activity>,
)

/**
 * 弹窗状态密封接口，每种弹窗类型对应一个子类
 */
sealed interface DialogState {
    object AddActivity : DialogState
    data class AddActivityToGroup(val group: ActivityGroup) : DialogState
    data class EditActivity(val activity: Activity, val tagIds: Set<Long> = emptySet()) : DialogState
    object AddGroup : DialogState
    data class RenameGroup(val group: ActivityGroup) : DialogState
    data class DeleteGroup(val group: ActivityGroup) : DialogState
    data class DeleteActivity(val activity: Activity) : DialogState
    data class MoveToGroup(val activity: Activity) : DialogState
    data class ActivityDetail(val activity: Activity) : DialogState
}
