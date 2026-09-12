package com.nltimer.feature.home.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.BehaviorNature
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime

enum class AddSheetMode(val nature: BehaviorNature) {
    COMPLETED(BehaviorNature.COMPLETED),
    CURRENT(BehaviorNature.ACTIVE),
    TARGET(BehaviorNature.PENDING),
}

/** 事件 BottomSheet 初始页（列表页 = 行为已有事件一览，表单页 = 快速打点/编辑） */
enum class EventSheetPage {
    LIST,
    FORM,
}

/**
 * 事件表单（打点）弹层目标信息
 * behaviorId 非空 = 挂载在某次行为上的事件；为空 = 独立事件（无活跃计时入口亦如此）
 * editEventId 非空 = 编辑已有事件（新增入口始终为 null）
 */
@Immutable
data class EventSheetTarget(
    val behaviorId: Long? = null,
    val activityId: Long? = null,
    val activityName: String? = null,
    val startEpochMs: Long? = null,
    /** 打开时经标签匹配/上次使用回退解析出的模板 id；null 表示无可用模板 */
    val initialTemplateId: Long? = null,
    /** 打开即编辑已有事件（列表页点条目进入表单时设置） */
    val editEventId: Long? = null,
    val initialPage: EventSheetPage = EventSheetPage.FORM,
)

@Immutable
data class HomeUiState(
    val items: PersistentList<HomeListItem> = persistentListOf(),
    val gridSections: PersistentList<GridDaySection> = persistentListOf(),
    val momentCells: PersistentList<GridCellUiState> = persistentListOf(),
    val isLoadingMore: Boolean = false,
    val hasReachedEarliest: Boolean = false,
    val addSheetMode: AddSheetMode? = null,
    val selectedTimeHour: Int = 0,
    val isLoading: Boolean = true,
    val isIdleMode: Boolean = false,
    val hasActiveBehavior: Boolean = false,
    val isDetailSheetVisible: Boolean = false,
    val detailBehavior: BehaviorDetailUiState? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val lastBehaviorEndTime: LocalDateTime? = null,
    val idleStartTime: LocalDateTime? = null,
    val idleEndTime: LocalDateTime? = null,
    val editBehaviorId: Long? = null,
    val editInitialActivityId: Long? = null,
    val editInitialTagIds: PersistentList<Long> = persistentListOf(),
    val editInitialNote: String? = null,
    /** 编辑 PENDING 目标时回填预估时长；新建为 null */
    val editInitialEstimatedDurationMs: Long? = null,
    val showAiQuickInput: Boolean = false,
    /** 「+ 记一笔」/ 事件列表 BottomSheet 目标；null = 未打开 */
    val eventSheet: EventSheetTarget? = null,
    /** 事件已保存/编辑等即时反馈（Snackbar 一次性消息，不与 errorMessage 混用） */
    val eventFeedback: String? = null,
)
