package com.nltimer.core.tools.timing

import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
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
import org.json.JSONObject

@Singleton
class ReorderGoalsTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "reorderGoals"
    override val description: String = "重排 PENDING 目标执行顺序，传入目标 id 列表（按新顺序排列）"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "orderedIds",
            description = "按新顺序排列的目标 id 列表",
            type = ParameterType.ARRAY,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val rawIds = args["orderedIds"]
        val ids = when (rawIds) {
            is List<*> -> rawIds.mapNotNull { (it as? Number)?.toLong() }
            is Array<*> -> rawIds.mapNotNull { (it as? Number)?.toLong() }
            else -> return ToolResult.Error(name, ToolError.ValidationError("orderedIds 必须是 id 列表"))
        }

        if (ids.isEmpty()) {
            return ToolResult.Error(name, ToolError.ValidationError("orderedIds 不能为空"))
        }

        return runCatching {
            behaviorRepository.reorderGoals(ids)
            val resultObj = JSONObject().apply {
                put("reordered", true)
                put("count", ids.size)
                put("orderedIds", org.json.JSONArray().apply { ids.forEach { put(it) } })
            }
            ToolResult.Success(name, resultObj.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "重排目标失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """{"reordered":true,"count":3,"orderedIds":[75,73,74]}""",
        errorExamples = listOf(
            ErrorExample("VALIDATION_ERROR", "orderedIds 不能为空", "传入空数组"),
        ),
        usageExamples = listOf(
            """reorderGoals(orderedIds=[75,73,74])""",
        ),
    )
}
