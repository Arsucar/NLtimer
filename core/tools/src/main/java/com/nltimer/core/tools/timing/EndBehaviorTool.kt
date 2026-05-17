package com.nltimer.core.tools.timing

import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
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

/**
 * 工具：结束当前正在进行（ACTIVE）的计时
 *
 * 业务约束：
 * - 必须存在 ACTIVE 行为，否则返回 ValidationError
 *
 * 返回结束的 Behavior id（Long）
 */
@Singleton
class EndBehaviorTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "endBehavior"
    override val description: String = "结束当前正在进行的计时（ACTIVE → COMPLETED）"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.WRITE
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = Long::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val current = behaviorRepository.getCurrentBehavior().first()
                ?: return@runCatching ToolResult.Error(
                    name = name,
                    error = ToolError.ValidationError("当前没有正在进行的行为"),
                )
            behaviorRepository.endCurrentBehavior(System.currentTimeMillis())
            ToolResult.Success(name, current.id)
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "结束计时失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "42  // 被结束的 Behavior id",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "当前没有正在进行的行为",
                scenario = "没有 ACTIVE 行为时调用",
            ),
        ),
        usageExamples = listOf(
            "endBehavior()  // 无参",
        ),
    )
}
