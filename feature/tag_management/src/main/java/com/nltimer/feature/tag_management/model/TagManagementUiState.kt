package com.nltimer.feature.tag_management.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Tag

/**
 * 标签管理界面的 UI 状态
 *
 * @property uncategorizedTags 未分类的标签列表
 * @property categories 按分类组织的标签列表
 * @property isLoading 是否正在加载数据
 * @property dialogState 当前显示的对话框状态，null 表示无对话框
 */
@Immutable
data class TagManagementUiState(
    val uncategorizedTags: List<Tag> = emptyList(),
    val categories: List<CategoryWithTags> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val expandedCategoryNames: Set<String> = emptySet(),
    val allActivities: List<Activity> = emptyList(),
    val activityGroups: List<ActivityGroup> = emptyList(),
    val isLoading: Boolean = true,
    val dialogState: DialogState? = null,
)

/**
 * 分类及其包含的标签
 *
 * @property categoryName 分类名称
 * @property tags 该分类下的标签列表
 */
@Immutable
data class CategoryWithTags(
    val categoryName: String,
    val tags: List<Tag>,
)

/**
 * 对话框状态 sealed 接口，每种变体对应一种对话框
 */
sealed interface DialogState {
    /** 添加标签对话框，可选指定初始分类 */
    data class AddTag(val category: String? = null) : DialogState
    data class EditTag(val tag: Tag, val activityId: Long? = null) : DialogState
    /** 删除标签确认对话框 */
    data class DeleteTag(val tag: Tag) : DialogState
    /** 移动标签到其他分类对话框 */
    data class MoveTag(val tag: Tag, val currentCategory: String?) : DialogState
    /** 添加分类对话框 */
    object AddCategory : DialogState
    /** 重命名分类对话框 */
    data class RenameCategory(val name: String) : DialogState
    /** 删除分类确认对话框 */
    data class DeleteCategory(val name: String, val tagCount: Int) : DialogState
}
