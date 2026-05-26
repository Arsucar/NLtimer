package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
import com.nltimer.core.tools.ParameterConstraint
import com.nltimer.core.tools.ParameterType
import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolDocumentation
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolParameter
import com.nltimer.core.tools.ToolResult
import com.nltimer.core.tools.match.KeywordMatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first

/**
 * 工具：开始一段计时（写入新的 ACTIVE Behavior）
 *
 * 入参：
 * - activityId (可选，Number, >= 1) —— 关联的活动 id
 * - tagName (可选，String) —— 标签名，用于查找关联的活动
 * - note (可选，String, maxLength=500) —— 备注
 *
 * activityId 和 tagName 二选一：
 * - 传 activityId：直接使用该活动
 * - 传 tagName：查找标签关联的活动，如果标签未关联活动则报错
 *
 * 标签分组语义：
 * - 简单事件分组内的标签 = 具体事件（如"小睡"、"睡觉"），可直接用于开始行为
 * - 标记类分组（如"重要"、"紧急"）= 属性标记，不用于开始行为
 * - 当用户说"开始XX"时，优先在简单事件分组中查找标签
 *
 * 业务约束：
 * - activityId 或 tagName 必须提供一个
 * - activityId 对应活动必须存在
 * - tagName 对应标签必须存在且关联了活动
 * - 当前不能已有 ACTIVE 行为（避免双开计时；如需切换应先调用结束工具）
 *
 * 返回新 Behavior 的 id (Long)
 */
@Singleton
class StartBehaviorTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val behaviorRepository: BehaviorRepository,
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "startBehavior"
    override val description: String = "为指定活动开始一段计时（写入 ACTIVE 状态的 Behavior）。可通过 activityId 或 tagName 指定活动。标签分组语义：简单事件分组内的标签（如'小睡'、'睡觉'）= 具体事件，可直接用于开始行为；标记类分组（如'重要'、'紧急'）= 属性标记，不用于开始行为。"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "activityId",
            description = "活动 id（来自 listActivities 返回的元素之一）。与 tagName 二选一",
            type = ParameterType.NUMBER,
            required = false,
            constraints = ParameterConstraint(minValue = 1),
        ),
        ToolParameter(
            name = "tagName",
            description = "标签名，用于查找关联的活动。与 activityId 二选一。标签分组定义了标签的性质：简单事件分组内的标签（如'小睡'、'睡觉'）= 具体事件，可直接用于开始行为；标记类分组（如'重要'、'紧急'）= 属性标记，不用于开始行为",
            type = ParameterType.STRING,
            required = false,
        ),
        ToolParameter(
            name = "note",
            description = "可选备注，最长 500 字",
            type = ParameterType.STRING,
            required = false,
            constraints = ParameterConstraint(maxLength = MAX_NOTE_LENGTH),
        ),
    )

    override val returnType: KClass<*> = Long::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val activityIdArg = (args["activityId"] as? Number)?.toLong()
        val tagNameArg = args["tagName"] as? String
        val note = args["note"] as? String

        // 校验：activityId 和 tagName 必须提供一个
        if (activityIdArg == null && tagNameArg.isNullOrBlank()) {
            return ToolResult.Error(
                name = name,
                error = ToolError.ValidationError("activityId 和 tagName 必须提供一个"),
            )
        }

        // 校验：不能同时提供
        if (activityIdArg != null && !tagNameArg.isNullOrBlank()) {
            return ToolResult.Error(
                name = name,
                error = ToolError.ValidationError("activityId 和 tagName 不能同时提供，请只传一个"),
            )
        }

        return runCatching {
            val resolvedActivityId = if (activityIdArg != null) {
                activityIdArg
            } else {
                val allTags = tagRepository.getAllActive().first()
                val tagName = requireNotNull(tagNameArg) { "tagNameArg should not be null when activityIdArg is null" }
                val tag = KeywordMatcher.matchTag(tagName, allTags)
                    ?: return@runCatching ToolResult.Error(
                        name = name,
                        error = ToolError.NotFound("未找到标签（名称或关键词匹配）: $tagNameArg"),
                    )
                val activityIds = tagRepository.getActivityIdsForTag(tag.id)
                if (activityIds.isEmpty()) {
                    return@runCatching ToolResult.Error(
                        name = name,
                        error = ToolError.ValidationError(
                            "标签「$tagNameArg」未关联任何活动，请先关联。" +
                                "标签分组定义了标签的性质：简单事件分组内的标签（如'小睡'、'睡觉'）可直接用于开始行为"
                        ),
                    )
                }
                activityIds.first()
            }

            val activity = activityRepository.getById(resolvedActivityId)
                ?: return@runCatching ToolResult.Error(
                    name = name,
                    error = ToolError.NotFound("活动不存在: id=$resolvedActivityId"),
                )

            val current = behaviorRepository.getCurrentBehavior().first()
            if (current != null) {
                return@runCatching ToolResult.Error(
                    name = name,
                    error = ToolError.ValidationError(
                        "当前已有正在进行的行为 (id=${current.id})，请先结束",
                    ),
                )
            }

            val behavior = Behavior(
                id = 0L,
                activityId = activity.id,
                startTime = System.currentTimeMillis(),
                endTime = null,
                status = BehaviorNature.ACTIVE,
                note = note,
                pomodoroCount = 0,
                sequence = 0,
                estimatedDuration = null,
                actualDuration = null,
                achievementLevel = null,
                wasPlanned = false,
            )
            val newId = behaviorRepository.insert(behavior, emptyList())
            ToolResult.Success(name, newId)
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "开始计时失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "42  // 新 Behavior 的 id",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "activityId 和 tagName 必须提供一个",
                scenario = "未传入任何标识符",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "activityId 和 tagName 不能同时提供，请只传一个",
                scenario = "同时传入两个标识符",
            ),
            ErrorExample(
                code = "NOT_FOUND",
                message = "未找到标签（名称或关键词匹配）: 小睡",
                scenario = "传入的标签名在数据库中不存在（名称和关键词均未匹配）",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "标签「小睡」未关联任何活动，请先关联",
                scenario = "标签存在但未关联活动",
            ),
            ErrorExample(
                code = "NOT_FOUND",
                message = "活动不存在: id=999",
                scenario = "传入的 activityId 在数据库中不存在",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "当前已有正在进行的行为 (id=41)，请先结束",
                scenario = "已有 ACTIVE 行为时不允许再次启动",
            ),
        ),
        usageExamples = listOf(
            "startBehavior(activityId=1)",
            "startBehavior(activityId=2, note=\"番茄钟第一阶段\")",
            "startBehavior(tagName=\"小睡\")",
            "startBehavior(tagName=\"睡觉\", note=\"晚安\")",
        ),
    )

    private companion object {
        const val MAX_NOTE_LENGTH = 500
    }
}
