package com.nltimer.core.behaviorui.sheet

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Tag

enum class SortMode(val label: String) {
    FREQUENCY("频率"),
    ALPHA("字母"),
    RECENT("最近"),
}

@Immutable
data class CategoryGroup<T>(
    val id: Long,
    val name: String,
    val items: List<T>,
    val onClear: (() -> Unit)? = null,
    val clearLabel: String? = null,
)

interface CategorizableItem {
    val itemId: Long
    val itemName: String
    val category: String?
    val usageCount: Int
    val lastUsedTimestamp: Long?
    val iconKey: String?
}

data class ActivityCategorizable(
    val activity: Activity,
    override val lastUsedTimestamp: Long? = null,
) : CategorizableItem {
    override val itemId: Long = activity.id
    override val itemName: String = activity.name
    override val category: String? = null
    override val usageCount: Int = activity.usageCount
    override val iconKey: String? = activity.iconKey
}

data class TagCategorizable(
    val tag: Tag,
    override val lastUsedTimestamp: Long? = null,
) : CategorizableItem {
    override val itemId: Long = tag.id
    override val itemName: String = tag.name
    override val category: String? = tag.category
    override val usageCount: Int = tag.usageCount
    override val iconKey: String? = null
}

data class ActivityGroupCategorizable(
    val group: ActivityGroup,
) : CategorizableItem {
    override val itemId: Long = group.id
    override val itemName: String = group.name
    override val category: String? = null
    override val usageCount: Int = 0
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = null
}

data class StringCategoryCategorizable(
    val name: String,
) : CategorizableItem {
    override val itemId: Long = name.hashCode().toLong()
    override val itemName: String = name
    override val category: String? = null
    override val usageCount: Int = 0
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = null
}
