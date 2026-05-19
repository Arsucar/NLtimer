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

@Singleton
class DeleteGoalTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "deleteGoal"
    override val description: String = "取消（删除）一个 PENDING 目标"
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

    override val returnType: KClass<*> = Long::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val id = (args["id"] as? Number)?.toLong()
            ?: return ToolResult.Error(name, ToolError.ValidationError("id 必填"))

        return runCatching {
            val goal = behaviorRepository.getBehaviorWithDetails(id)
                ?: return@runCatching ToolResult.Error(name, ToolError.NotFound("目标不存在: id=$id"))

            if (goal.behavior.status != BehaviorNature.PENDING) {
                return@runCatching ToolResult.Error(
                    name,
                    ToolError.ValidationError("只能取消 PENDING 目标，当前状态: ${goal.behavior.status}"),
                )
            }

            behaviorRepository.delete(id)
            ToolResult.Success(name, id)
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "取消目标失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "73  // 被删除的目标 id",
        errorExamples = listOf(
            ErrorExample("NOT_FOUND", "目标不存在: id=999", "id 无效"),
            ErrorExample("VALIDATION_ERROR", "只能取消 PENDING 目标", "对非 PENDING 行为调用"),
        ),
        usageExamples = listOf("deleteGoal(id=73)"),
    )
}
