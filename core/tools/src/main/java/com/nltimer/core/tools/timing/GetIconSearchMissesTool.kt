package com.nltimer.core.tools.timing

import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolDocumentation
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolParameter
import com.nltimer.core.tools.ToolResult
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class GetIconSearchMissesTool @Inject constructor(
    private val missLog: IconSearchMissLog,
) : ToolDefinition {

    override val name: String = "getIconSearchMisses"
    override val description: String = "查看用户搜索图标但未找到结果的记录，用于补充图标库"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = String::class

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val misses = missLog.getAll()
            val arr = JSONArray()
            for (m in misses) {
                arr.put(JSONObject().apply {
                    put("id", m.id)
                    put("query", m.query)
                    put("library", m.library)
                    put("timestamp", dateFormat.format(m.timestamp))
                })
            }
            val result = JSONObject().apply {
                put("totalMisses", misses.size)
                put("misses", arr)
            }
            ToolResult.Success(name, result.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "获取未满足图标需求失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "totalMisses": 3,
              "misses": [
                {"id":1,"query":"swimming","library":"hi","timestamp":"2026-05-20 14:30:00"},
                {"id":2,"query":"游泳","library":"all","timestamp":"2026-05-20 14:30:15"}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("INTERNAL_ERROR", "获取未满足图标需求失败", "内部错误"),
        ),
        usageExamples = listOf("getIconSearchMisses()"),
    )
}
