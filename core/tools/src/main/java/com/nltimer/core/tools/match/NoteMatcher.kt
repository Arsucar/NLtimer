package com.nltimer.core.tools.match

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 单次备注扫描的结果。
 *
 * @property activityId 至多 1 个；命中多项时按候选在 note 中的**最早出现位置**取首者
 * @property tagIds 全部命中标签 ID 集合；无命中为空集
 */
data class NoteScanResult(
    val activityId: Long?,
    val tagIds: Set<Long>,
)

/**
 * 备注反向扫描器 —— 在用户输入的备注里寻找哪些活动 / 标签的名字或 keywords token "整体出现"。
 *
 * 与同 package 下两个工具的方向区别：
 * - [SearchActivitiesAndTagsTool]：候选 name / keyword **包含** query（"找相关项"）
 * - [SelectActivitiesAndTagsTool]：候选 name / keyword **完全等于** query（"精确选择"）
 * - 本类：**note 包含** 候选（"备注扫描字典"，方向相反）
 *
 * 候选规则（与用户最终需求严格对齐）：
 * - `keywords` 非空：使用全部 `parseKeywords` 拆分出的 token，单字 token 也参与
 *   （用户主动设置 keywords 即视为完整意图，不再 ≥2 字保护）
 * - `keywords` 空：回退到 `name`，且仅当 `name.length ≥ 2` 才参与（防止单字 name 污染长备注）
 *
 * 选中策略：
 * - 活动：单选；命中多项取 `note.indexOf(候选)` 最小者
 * - 标签：多选；全部命中均纳入
 * - 已归档项一律不参与
 * - 大小写不敏感（兼容英文活动 / 标签）
 *
 * 性能：2000 条 contains 在 JVM 上单次扫描约 1-3ms，调用方可直接在主线程使用。
 */
@Singleton
class NoteMatcher @Inject constructor() {

    fun scan(note: String, activities: List<Activity>, tags: List<Tag>): NoteScanResult {
        if (note.isBlank()) return NoteScanResult(null, emptySet())
        val noteLower = note.lowercase()

        val activityId = activities
            .asSequence()
            .filter { !it.isArchived }
            .mapNotNull { a ->
                earliestHitPos(noteLower, a.keywords, a.name)?.let { pos -> a.id to pos }
            }
            .minByOrNull { it.second }
            ?.first

        val tagIds = tags
            .asSequence()
            .filter { !it.isArchived }
            .filter { earliestHitPos(noteLower, it.keywords, it.name) != null }
            .map { it.id }
            .toSet()

        return NoteScanResult(activityId, tagIds)
    }

    /**
     * 该项任一候选在 note 中的最早出现位置；都不命中返回 null。
     * 复用 [parseKeywords] 同款拆分语义（逗号 / 分号 / 空白）。
     */
    private fun earliestHitPos(noteLower: String, keywords: String?, name: String): Int? {
        val tokens = parseKeywords(keywords)
        val candidates: List<String> = if (tokens.isNotEmpty()) {
            tokens
        } else if (name.length >= 2) {
            listOf(name)
        } else {
            return null
        }

        return candidates
            .mapNotNull { c -> noteLower.indexOf(c.lowercase()).takeIf { it >= 0 } }
            .minOrNull()
    }
}
