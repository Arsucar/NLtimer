package com.nltimer.core.tools.match

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.usecase.AddActivityUseCase
import com.nltimer.core.data.usecase.AddTagUseCase
import javax.inject.Inject

/**
 * 应用 @/# 主动指令：
 * - 命中现有非归档同名活动/标签 → 仅返回其 id
 * - 未命中 → 调 [AddActivityUseCase] / [AddTagUseCase] 静默默认创建
 * - 多个 @：按顺序处理，lastActivityId 为最后一个候选
 * - 多个 #：全部 id union 到 addedTagIds
 * - 同批次中相同 name 仅创建一次
 *
 * 设计依据：docs/superpowers/specs/2026-05-14-note-directive-design.md §4
 */
class ApplyNoteDirectivesUseCase @Inject constructor(
    private val addActivityUseCase: AddActivityUseCase,
    private val addTagUseCase: AddTagUseCase,
) {
    data class Outcome(
        val lastActivityId: Long?,
        val addedTagIds: Set<Long>,
        val createdActivityNames: List<String>,
        val createdTagNames: List<String>,
        val matchedActivityNames: List<String>,
        val matchedTagNames: List<String>,
    ) {
        companion object {
            val Empty = Outcome(null, emptySet(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    suspend operator fun invoke(
        directives: List<NoteDirectiveParser.Directive>,
        existingActivities: List<Activity>,
        existingTags: List<Tag>,
        selectedActivityId: Long? = null,
    ): Outcome {
        if (directives.isEmpty()) return Outcome.Empty

        val batchActivities = mutableMapOf<String, Long>()
        val batchTags = mutableMapOf<String, Long>()
        val activityIdsInOrder = mutableListOf<Long>()
        val tagIdsCollected = linkedSetOf<Long>()
        val createdActivityNames = mutableListOf<String>()
        val createdTagNames = mutableListOf<String>()
        val matchedActivityNames = mutableListOf<String>()
        val matchedTagNames = mutableListOf<String>()

        for (d in directives) {
            val key = d.name.lowercase()
            when (d.symbol) {
                '@' -> {
                    val cached = batchActivities[key]
                    if (cached != null) {
                        activityIdsInOrder += cached
                        continue
                    }
                    val existing = existingActivities.firstOrNull {
                        !it.isArchived && it.name.equals(d.name, ignoreCase = true)
                    }
                    val id = if (existing != null) {
                        matchedActivityNames += existing.name
                        existing.id
                    } else {
                        val newId = safeCall {
                            addActivityUseCase(d.name, null, null, null, null, emptyList())
                        } ?: continue
                        createdActivityNames += d.name
                        newId
                    }
                    batchActivities[key] = id
                    activityIdsInOrder += id
                }
                '#' -> {
                    val cached = batchTags[key]
                    if (cached != null) {
                        tagIdsCollected += cached
                        continue
                    }
                    val existing = existingTags.firstOrNull {
                        !it.isArchived && it.name.equals(d.name, ignoreCase = true)
                    }
                    val id = if (existing != null) {
                        matchedTagNames += existing.name
                        existing.id
                    } else {
                        val newId = safeCall {
                            addTagUseCase(d.name, null, null, 0, null, null, selectedActivityId)
                        } ?: continue
                        createdTagNames += d.name
                        newId
                    }
                    batchTags[key] = id
                    tagIdsCollected += id
                }
            }
        }

        return Outcome(
            lastActivityId = activityIdsInOrder.lastOrNull(),
            addedTagIds = tagIdsCollected.toSet(),
            createdActivityNames = createdActivityNames,
            createdTagNames = createdTagNames,
            matchedActivityNames = matchedActivityNames,
            matchedTagNames = matchedTagNames,
        )
    }
}

/**
 * 协程安全的 try-call：捕获普通异常并返回 null，但保留 [kotlinx.coroutines.CancellationException]
 * 让协程能正确响应取消。把 `runCatching` 在协程中的反模式封装在一处。
 */
private inline fun <T> safeCall(block: () -> T): T? = runCatching(block).getOrElse { e ->
    if (e is kotlinx.coroutines.CancellationException) throw e
    null
}
