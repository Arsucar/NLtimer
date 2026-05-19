package com.nltimer.core.tools.timing

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
 * 工具：按 id 删除 Behavior（含 ACTIVE / PENDING / COMPLETED 任意状态）
 *
 * 主要给冲突回报后"覆盖旧记录"路径用：LLM 拿到 conflicts 列表里的 id，
 * 逐个 deleteBehavior，然后重发 recordBehavior。
 *
 * 因为是潜在不可逆操作，accessLevel = FULL。
 */
@Singleton
class DeleteBehaviorTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "deleteBehavior"
    override val description: String = "按 id 删除行为记录；常用于冲突覆盖前先删旧记录"
    override val category: ToolCategory = ToolCategory.BEHAVIOR
    override val accessLevel: AccessLevel = AccessLevel.FULL

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "id",
            description = "Behavior id",
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
            behaviorRepository.delete(id)
            ToolResult.Success(name, id)
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "删除失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "42  // 被删除的 Behavior id",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "id 必填",
                scenario = "未传入 id 或类型错误",
            ),
        ),
        usageExamples = listOf(
            "deleteBehavior(id=42)",
        ),
    )
}
