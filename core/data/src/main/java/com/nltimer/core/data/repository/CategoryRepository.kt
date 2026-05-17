package com.nltimer.core.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * CategoryRepository 分类管理仓库接口
 * 分别管理活动分类与标签分类的增删改名操作
 */
interface CategoryRepository {
    fun getDistinctActivityCategories(parent: String? = null): Flow<List<String>>
    suspend fun addActivityCategory(name: String)
    suspend fun renameActivityCategory(oldName: String, newName: String, parent: String? = null)
    suspend fun resetActivityCategory(category: String)

    fun getDistinctTagCategories(parent: String? = null): Flow<List<String>>
    suspend fun renameTagCategory(oldName: String, newName: String, parent: String? = null)
    suspend fun resetTagCategory(category: String)

    /**
     * 声明一个标签分类名（占位语义）
     *
     * 由于 Tag.category 是字符串字段、没有独立 TagCategory 表，
     * "创建标签分类"实际上是声明一个未来要用的名字 —— 待首个 tag 写入后从 distinct 自然出现。
     * 该方法只做查重：
     * - 名字非空且不在现有 distinct categories 中 → 返回 true（视为新增声明）
     * - 已存在 → 返回 false
     */
    suspend fun addTagCategoryStub(name: String): Boolean
}
