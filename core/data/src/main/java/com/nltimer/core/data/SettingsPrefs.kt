package com.nltimer.core.data

import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.StatsDashboardConfig
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import kotlinx.coroutines.flow.Flow

/**
 * SettingsPrefs 用户偏好设置接口
 * 提供主题配置、标签分类和弹窗配置的持久化读写能力
 */
interface SettingsPrefs {
    /** 以 Flow 形式监听当前主题设置 */
    fun getThemeFlow(): Flow<Theme>
    /** 更新主题设置并持久化 */
    suspend fun updateTheme(theme: Theme)

    /** 获取已保存的标签分类集合 */
    fun getSavedTagCategories(): Flow<Set<String>>
    /** 获取已保存的标签分类顺序 */
    fun getSavedTagCategoriesOrder(): Flow<List<String>>
    /** 保存标签分类列表到持久化存储 */
    suspend fun saveTagCategories(categories: Set<String>)
    /** 保存标签分类顺序到持久化存储 */
    suspend fun saveTagCategoriesOrder(categories: List<String>)

    /** 以 Flow 形式监听弹窗配置 */
    fun getDialogConfigFlow(): Flow<DialogGridConfig>
    /** 更新弹窗配置并持久化 */
    suspend fun updateDialogConfig(config: DialogGridConfig)

    /** 以 Flow 形式监听时间标签配置 */
    fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig>
    /** 更新时间标签配置并持久化 */
    suspend fun updateTimeLabelConfig(config: TimeLabelConfig)

    /** 以 Flow 形式监听主页布局样式配置 */
    fun getHomeLayoutConfigFlow(): Flow<HomeLayoutConfig>
    /** 更新主页布局样式配置并持久化 */
    suspend fun updateHomeLayoutConfig(config: HomeLayoutConfig)

    /** 以 Flow 形式监听是否已看过引导页 */
    fun getHasSeenIntroFlow(): Flow<Boolean>
    /** 设置已看过引导页 */
    suspend fun setHasSeenIntro(seen: Boolean)

    /** 以 Flow 形式监听显示颜色配置 */
    fun getDisplayColorConfigFlow(): Flow<com.nltimer.core.data.model.DisplayColorConfig>
    /** 更新显示颜色配置并持久化 */
    suspend fun updateDisplayColorConfig(config: com.nltimer.core.data.model.DisplayColorConfig)

    /** 以 Flow 形式监听标签显示配置 */
    fun getTagDisplayConfigFlow(): Flow<com.nltimer.core.data.model.TagDisplayConfig>
    /** 更新标签显示配置并持久化 */
    suspend fun updateTagDisplayConfig(config: com.nltimer.core.data.model.TagDisplayConfig)

    /** 以 Flow 形式监听专注卡片配置 */
    fun getFocusCardConfigFlow(): Flow<FocusCardConfig>
    /** 更新专注卡片配置并持久化 */
    suspend fun updateFocusCardConfig(config: FocusCardConfig)

    fun getStatsDashboardConfigFlow(): Flow<StatsDashboardConfig>
    suspend fun updateStatsDashboardConfig(config: StatsDashboardConfig)
}
