package com.nltimer.feature.home.viewmodel

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.Tag
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.GridDaySection
import com.nltimer.feature.home.model.GridRowUiState
import com.nltimer.feature.home.model.HomeListItem
import com.nltimer.feature.home.model.HomeUiState
import com.nltimer.feature.home.model.TagUiState
import com.nltimer.core.data.util.formatGridDurationHours
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HomeUiStateBuilder {

    companion object {
        const val STATE_TIMEOUT_MS = 5_000L
        private const val ROW_ID_FORMAT = "row-%d-%s"
        const val DEFAULT_GRID_COLUMNS = 4
    }

    fun buildUiState(
        behaviors: List<Behavior>,
        activities: List<Activity>,
        tagsByBehaviorId: Map<Long, List<Tag>>,
        now: LocalTime,
        currentTimeMs: Long,
        today: LocalDate,
        gridColumns: Int = DEFAULT_GRID_COLUMNS,
    ): HomeUiState {
        if (behaviors.isEmpty()) {
            return buildEmptyState(now)
        }

        val zoneId = ZoneId.systemDefault()
        val hasActive = calculateCurrentBehavior(behaviors)
        val activityMap = activities.associateBy { it.id }
        val sortedBehaviors = buildTimelineBehaviors(behaviors)
        val allCellsRaw = buildMomentBehaviors(sortedBehaviors, activityMap, tagsByBehaviorId, currentTimeMs, zoneId)

        // 预计算所有 Cell 的日期，避免重复转换
        val cellDateMap = allCellsRaw.associateWith { cell ->
            cell.startEpochMs?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
                ?: if (cell.status == BehaviorNature.PENDING) today else null
        }

        val todayBehaviorIds = sortedBehaviors
            .filter { b ->
                if (b.status == BehaviorNature.PENDING || b.startTime <= 0L) false
                else Instant.ofEpochMilli(b.startTime).atZone(zoneId).toLocalDate() == today
            }
            .mapTo(mutableSetOf()) { it.id }

        val todayCells = mutableListOf<GridCellUiState>()
        val pendingCells = mutableListOf<GridCellUiState>()
        val nonTodayCells = mutableListOf<GridCellUiState>()
        
        // 分离 datedCells 用于后续 List 和 Grid 构建
        val datedCellsByDate = mutableMapOf<LocalDate, MutableList<GridCellUiState>>()

        for (cell in allCellsRaw) {
            val date = cellDateMap[cell]
            if (cell.behaviorId != null && date != null) {
                datedCellsByDate.getOrPut(date) { mutableListOf() }.add(cell)
            }

            when {
                cell.status == BehaviorNature.PENDING -> pendingCells.add(cell)
                cell.behaviorId != null && cell.behaviorId in todayBehaviorIds -> todayCells.add(cell)
                cell.behaviorId != null && cell.status != BehaviorNature.PENDING -> nonTodayCells.add(cell)
            }
        }
        val momentCells = (todayCells + pendingCells + nonTodayCells).toPersistentList()

        val addCell = buildAddCell(todayCells, today, now)
        val gridSections = buildGridSections(datedCellsByDate, sortedBehaviors, today, addCell, now, gridColumns, zoneId)
        val items = buildListItems(datedCellsByDate, today)

        val lastBehaviorEndTime = calculateLastBehaviorEndTime(behaviors, zoneId)

        return HomeUiState(
            items = items.toPersistentList(),
            gridSections = gridSections.toPersistentList(),
            momentCells = momentCells,
            isLoading = false,
            selectedTimeHour = now.hour,
            hasActiveBehavior = hasActive,
            lastBehaviorEndTime = lastBehaviorEndTime,
        )
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String {
        val datePart = "${date.monthValue}月${date.dayOfMonth}日"
        val days = ChronoUnit.DAYS.between(date, today)
        return when (days) {
            0L -> "今天 $datePart"
            1L -> "昨天 $datePart"
            else -> datePart
        }
    }

    private fun buildListItems(
        datedCellsByDate: Map<LocalDate, List<GridCellUiState>>,
        today: LocalDate,
    ): List<HomeListItem> {
        val result = mutableListOf<HomeListItem>()
        datedCellsByDate.keys.sorted().forEach { date ->
            result.add(HomeListItem.DayDivider(date = date, label = dayLabel(date, today)))
            datedCellsByDate[date]!!.forEach { cell -> result.add(HomeListItem.CellItem(cell)) }
        }
        return result
    }

    private fun buildGridSections(
        datedCellsByDate: Map<LocalDate, List<GridCellUiState>>,
        sortedBehaviors: List<Behavior>,
        today: LocalDate,
        todayAddCell: GridCellUiState,
        now: LocalTime,
        gridColumns: Int,
        zoneId: ZoneId,
    ): List<GridDaySection> {
        val sections = mutableListOf<GridDaySection>()
        datedCellsByDate.keys.sortedDescending().forEach { date ->
            val cells = datedCellsByDate[date]!!.sortedByDescending { it.startEpochMs ?: Long.MAX_VALUE }
            val cellsForSection = if (date == today) cells + todayAddCell else cells
            val dateBehaviors = sortedBehaviors.filter { b ->
                if (b.status == BehaviorNature.PENDING) date == today
                else b.startTime > 0L && Instant.ofEpochMilli(b.startTime).atZone(zoneId).toLocalDate() == date
            }.sortedByDescending { if (it.status == BehaviorNature.PENDING) Long.MAX_VALUE else it.startTime }

            val isTodaySection = date == today
            val rowsTime = if (isTodaySection) now else dateBehaviors.firstOrNull()?.let {
                if (it.status == BehaviorNature.PENDING) now
                else Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalTime()
            } ?: LocalTime.MIDNIGHT
            val (rows, _) = buildGridRows(
                allCells = cellsForSection,
                sortedBehaviors = dateBehaviors,
                now = rowsTime,
                gridColumns = gridColumns,
                isCurrentDay = isTodaySection,
                zoneId = zoneId,
            )
            sections.add(GridDaySection(date = date, label = dayLabel(date, today), rows = rows.toPersistentList()))
        }
        return sections
    }

    private fun buildEmptyState(now: LocalTime): HomeUiState {
        val addCell = GridCellUiState(
            behaviorId = null,
            activityIconKey = null,
            activityName = null,
            tags = persistentListOf(),
            status = null,
            isCurrent = false,
            isAddPlaceholder = true,
            formattedDuration = "",
            platinumStrength = 0f,
        )
        val row = GridRowUiState(
            rowId = "empty-row",
            startTime = now,
            isCurrentRow = true,
            isLocked = false,
            cells = persistentListOf(addCell),
        )
        return HomeUiState(
            gridSections = persistentListOf(
                GridDaySection(
                    date = LocalDate.now(),
                    label = "今天",
                    rows = persistentListOf(row),
                )
            ),
            momentCells = persistentListOf(),
            isLoading = false,
            selectedTimeHour = now.hour,
            hasActiveBehavior = false,
        )
    }

    private fun buildTimelineBehaviors(behaviors: List<Behavior>): List<Behavior> {
        val nonPending = behaviors
            .filter { it.status != BehaviorNature.PENDING }
            .sortedBy { it.startTime }
        val pending = behaviors
            .filter { it.status == BehaviorNature.PENDING }
            .sortedBy { it.sequence }
        return nonPending + pending
    }

    private fun buildMomentBehaviors(
        sortedBehaviors: List<Behavior>,
        activityMap: Map<Long, Activity>,
        tagsByBehaviorId: Map<Long, List<Tag>>,
        currentTimeMs: Long,
        zoneId: ZoneId,
    ): List<GridCellUiState> {
        return sortedBehaviors.map { behavior ->
            val activity = activityMap[behavior.activityId]
            val tags = tagsByBehaviorId[behavior.id].orEmpty()
            val isActive = behavior.status == BehaviorNature.ACTIVE
            val isPending = behavior.status == BehaviorNature.PENDING
            // PENDING 的 startTime 在数据库里是 0L，用 Instant.ofEpochMilli(0) 在 UTC+N 时区下
            // 会被解析成 08:00 之类的幽灵值（中国时区 UTC+8 → 1970-01-01 08:00）。
            // 因此这里直接置 null，避免详情弹窗里出现伪造的开始时间。
            val startLocal = if (isPending || behavior.startTime <= 0L) {
                null
            } else {
                Instant.ofEpochMilli(behavior.startTime)
                    .atZone(zoneId)
                    .toLocalDateTime()
            }
            val endLocal = behavior.endTime?.let {
                Instant.ofEpochMilli(it)
                    .atZone(zoneId)
                    .toLocalDateTime()
            }

            val isPlatinum = behavior.wasPlanned && behavior.status == BehaviorNature.COMPLETED
            val platinumStrength = behavior.achievementLevel?.let { it / 100f } ?: 0f
            val duration = if (isActive && behavior.startTime > 0) {
                currentTimeMs - behavior.startTime
            } else {
                behavior.actualDuration ?: 0L
            }

            GridCellUiState(
                behaviorId = behavior.id,
                activityIconKey = activity?.iconKey,
                activityName = activity?.name,
                tags = tags.map { TagUiState(id = it.id, name = it.name, color = it.color, isActive = !it.isArchived) }.toPersistentList(),
                status = behavior.status,
                isCurrent = isActive,
                wasPlanned = behavior.wasPlanned,
                achievementLevel = behavior.achievementLevel,
                estimatedDuration = behavior.estimatedDuration,
                actualDuration = behavior.actualDuration,
                durationMs = if (isActive && behavior.startTime > 0) {
                    currentTimeMs - behavior.startTime
                } else null,
                startTime = startLocal,
                endTime = endLocal,
                startEpochMs = if (isPending || behavior.startTime <= 0L) null else behavior.startTime,
                endEpochMs = behavior.endTime,
                note = behavior.note,
                pomodoroCount = behavior.pomodoroCount,
                formattedDuration = formatGridDurationHours(duration),
                platinumStrength = platinumStrength,
            )
        }
    }

    private fun buildAddCell(cells: List<GridCellUiState>, today: LocalDate, now: LocalTime): GridCellUiState {
        val nowDateTime = today.atTime(now)
        val lastEnd = cells.lastOrNull()?.endTime
        val idleStart = lastEnd ?: nowDateTime
        val idleEnd = nowDateTime

        return GridCellUiState(
            behaviorId = null,
            activityIconKey = null,
            activityName = null,
            tags = persistentListOf(),
            status = null,
            isCurrent = false,
            isAddPlaceholder = true,
            startTime = idleStart,
            endTime = idleEnd,
            formattedDuration = "",
            platinumStrength = 0f,
        )
    }

    private fun buildGridRows(
        allCells: List<GridCellUiState>,
        sortedBehaviors: List<Behavior>,
        now: LocalTime,
        gridColumns: Int = DEFAULT_GRID_COLUMNS,
        isCurrentDay: Boolean = true,
        zoneId: ZoneId,
    ): Pair<List<GridRowUiState>, String?> {
        val rows = mutableListOf<GridRowUiState>()
        var currentRowId: String? = null

        allCells.chunked(gridColumns).forEachIndexed { rowIndex, rowCells ->
            val rowId = ROW_ID_FORMAT.format(rowIndex, rowCells.firstOrNull()?.behaviorId ?: "add")
            val hasCurrentInRow = isCurrentDay && rowCells.any { it.isCurrent }
            if (hasCurrentInRow) currentRowId = rowId

            val timeForRow = if (rowIndex < allCells.size / gridColumns) {
                val behavior = sortedBehaviors.getOrNull(rowIndex * gridColumns)
                if (behavior != null
                    && behavior.status != BehaviorNature.PENDING
                    && behavior.startTime > 0L
                ) {
                    Instant.ofEpochMilli(behavior.startTime)
                        .atZone(zoneId)
                        .toLocalTime()
                } else {
                    now
                }
            } else {
                now
            }

            val paddedCells = rowCells.toMutableList()
            while (paddedCells.size < gridColumns) {
                paddedCells.add(
                    GridCellUiState(
                        behaviorId = null,
                        activityIconKey = null,
                        activityName = null,
                        tags = persistentListOf(),
                        status = null,
                        isCurrent = false,
                        formattedDuration = "",
                        platinumStrength = 0f,
                    )
                )
            }

            rows.add(
                GridRowUiState(
                    rowId = rowId,
                    startTime = timeForRow,
                    isCurrentRow = hasCurrentInRow,
                    isLocked = false,
                    cells = paddedCells.toPersistentList(),
                )
            )
        }

        return rows to currentRowId
    }

    private fun calculateCurrentBehavior(behaviors: List<Behavior>): Boolean {
        return behaviors.any { it.status == BehaviorNature.ACTIVE }
    }

    private fun calculateLastBehaviorEndTime(behaviors: List<Behavior>, zoneId: ZoneId): LocalDateTime? {
        return behaviors
            .filter { it.endTime != null }
            .maxByOrNull { it.endTime ?: 0 }
            ?.let {
                Instant.ofEpochMilli(it.endTime!!)
                    .atZone(zoneId)
                    .toLocalDateTime()
            }
    }
}
