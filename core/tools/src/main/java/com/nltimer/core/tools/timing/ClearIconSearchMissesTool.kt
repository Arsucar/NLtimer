package com.nltimer.core.tools.timing

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

@Singleton
class ClearIconSearchMissesTool @Inject constructor(
    private val missLog: IconSearchMissLog,
) : ToolDefinition {

    override val name: String = "clearIconSearchMisses"
    override val description: String = "清空图标搜索未命中记录"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.WRITE
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val count = missLog.count()
            missLog.clear()
            ToolResult.Success(name, """{"cleared":true,"wasCount":$count}""")
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "清除失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """{"cleared":true,"wasCount":5}""",
        errorExamples = emptyList(),
        usageExamples = listOf("clearIconSearchMisses()"),
    )
}
