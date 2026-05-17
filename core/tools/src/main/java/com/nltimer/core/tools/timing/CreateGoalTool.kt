package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorRepository
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * 工具：创建 PENDING 目标
 *
 * Goal 不是独立实体 —— 在 Behavior 表里以 PENDING 状态出现。
 * 不走 [com.nltimer.core.data.usecase.AddBehaviorUseCase] 因为它不收 estimatedDuration 参数；
 * PENDING 路径在 UseCase 内也只是直插，无额外约束，绕过安全。
 */
@Singleton
class CreateGoalTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "createGoal"
    override val description: String = "创建 PENDING 目标（不立即开始计时），可指定预计用时（分钟）与标签"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "activityId",
            description = "活动 id",
            type = ParameterType.NUMBER,
            required = true,
            constraints = ParameterConstraint(minValue = 1),
        ),
        ToolParameter(
            name = "tagIds",
            description = "标签 id 列表（可空）",
            type = ParameterType.ARRAY,
            required = false,
        ),
        ToolParameter(
            name = "estimatedDurationMinutes",
            description = "预计用时（分钟）",
            type = ParameterType.NUMBER,
            required = false,
            constraints = ParameterConstraint(minValue = 1),
        ),
        ToolParameter(
            name = "note",
            description = "备注，最长 500 字",
            type = ParameterType.STRING,
            required = false,
            constraints = ParameterConstraint(maxLength = MAX_NOTE_LENGTH),
        ),
    )

    override val returnType: KClass<*> = Long::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val activityId = (args["activityId"] as? Number)?.toLong()
            ?: return ToolResult.Error(name, ToolError.ValidationError("activityId 必填"))
        val tagIds = parseLongList(args["tagIds"])
        val durationMinutes = (args["estimatedDurationMinutes"] as? Number)?.toLong()
        val note = (args["note"] as? String)?.takeIf { it.isNotBlank() }

        return runCatching {
            activityRepository.getById(activityId)
                ?: return@runCatching ToolResult.Error(
                    name,
                    ToolError.NotFound("活动不存在: id=$activityId"),
                )

            val sequence = behaviorRepository.getMaxSequence() + 1
            val newId = behaviorRepository.insert(
                Behavior(
                    id = 0L,
                    activityId = activityId,
                    startTime = 0L,
                    endTime = null,
                    status = BehaviorNature.PENDING,
                    note = note,
                    pomodoroCount = 0,
                    sequence = sequence,
                    estimatedDuration = durationMinutes?.let { it * MILLIS_PER_MINUTE },
                    actualDuration = null,
                    achievementLevel = null,
                    wasPlanned = true,
                ),
                tagIds,
            )
            ToolResult.Success(name, newId)
        }.getOrElse { e ->
            ToolResult.Error(
                name,
                ToolError.InternalError(e.message ?: "创建目标失败"),
            )
        }
    }

    private fun parseLongList(value: Any?): List<Long> = when (value) {
        is List<*> -> value.mapNotNull { (it as? Number)?.toLong() }
        is Array<*> -> value.mapNotNull { (it as? Number)?.toLong() }
        else -> emptyList()
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "73  // 新 PENDING Behavior 的 id",
        errorExamples = listOf(
            ErrorExample(
                code = "NOT_FOUND",
                message = "活动不存在: id=999",
                scenario = "传入的 activityId 在数据库中不存在",
            ),
        ),
        usageExamples = listOf(
            "createGoal(activityId=4, tagIds=[5], estimatedDurationMinutes=10, note=\"先去食堂吃饭，再散步\")",
        ),
    )

    private companion object {
        const val MAX_NOTE_LENGTH = 500
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
