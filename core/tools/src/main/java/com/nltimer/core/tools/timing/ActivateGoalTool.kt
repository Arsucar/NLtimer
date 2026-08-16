package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.BehaviorNature
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
import kotlinx.coroutines.flow.first
import org.json.JSONObject

@Singleton
class ActivateGoalTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "activateGoal"
    override val description: String = "将 PENDING 目标激活为 ACTIVE（开始计时），自动结束当前已有 ACTIVE 行为"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "id",
            description = "目标（PENDING Behavior）id",
            type = ParameterType.NUMBER,
            required = true,
            constraints = ParameterConstraint(minValue = 1),
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val id = (args["id"] as? Number)?.toLong()
            ?: return ToolResult.Error(name, ToolError.ValidationError("id 必填"))

        return runCatching {
            val goal = behaviorRepository.getBehaviorWithDetails(id)
                ?: return@runCatching ToolResult.Error(name, ToolError.NotFound("目标不存在: id=$id"))

            if (goal.behavior.status != BehaviorNature.PENDING) {
                return@runCatching ToolResult.Error(
                    name,
                    ToolError.ValidationError("该行为不是 PENDING 状态，当前状态: ${goal.behavior.status}"),
                )
            }

            val current = behaviorRepository.getCurrentBehavior().first()
            if (current != null) {
                behaviorRepository.endCurrentBehavior(System.currentTimeMillis())
            }

            behaviorRepository.setStartTime(id, System.currentTimeMillis())
            behaviorRepository.setStatus(id, BehaviorNature.ACTIVE.key)

            val resultObj = JSONObject().apply {
                put("id", id)
                put("activityId", goal.behavior.activityId)
                put("activityName", goal.activity.name)
                put("status", "ACTIVE")
                put("startTime", TimeUtils.formatIso(System.currentTimeMillis()))
                if (current != null) {
                    put("endedPreviousId", current.id)
                }
            }
            ToolResult.Success(name, resultObj.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "激活目标失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """{"id":73,"activityId":4,"activityName":"主动学习","status":"ACTIVE","startTime":"2026-05-19T14:00:00+08:00","endedPreviousId":42}""",
        errorExamples = listOf(
            ErrorExample("NOT_FOUND", "目标不存在: id=999", "id 无效"),
            ErrorExample("VALIDATION_ERROR", "该行为不是 PENDING 状态", "对非 PENDING 行为调用"),
        ),
        usageExamples = listOf("activateGoal(id=73)"),
    )
}
