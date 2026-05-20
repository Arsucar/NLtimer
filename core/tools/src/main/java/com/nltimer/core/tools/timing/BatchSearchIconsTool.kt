package com.nltimer.core.tools.timing

import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
import com.nltimer.core.tools.ParameterType
import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolConfig
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolDocumentation
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolParameter
import com.nltimer.core.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BatchSearchIconsTool @Inject constructor(
    private val missLog: IconSearchMissLog,
    private val toolConfig: ToolConfig,
) : ToolDefinition {

    override val name: String = "batchSearchIcons"
    override val description: String =
        "批量搜索图标：传入多个关键词，一次返回每个关键词的最佳匹配结果。适合批量创建活动/标签时一次性获取所有图标。"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.NONE
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "queries",
            description = "搜索关键词列表（中英文均可），如 [\"code\",\"review\",\"sleep\"]",
            type = ParameterType.ARRAY,
            required = true,
        ),
        ToolParameter(
            name = "library",
            description = "图标库过滤：hi / mi / emoji；默认返回所有",
            type = ParameterType.STRING,
            required = false,
        ),
        ToolParameter(
            name = "limitPerQuery",
            description = "每个关键词返回数量上限，默认 3",
            type = ParameterType.NUMBER,
            required = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val queries = (args["queries"] as? List<String>)
            ?: return ToolResult.Error(name, ToolError.ValidationError("queries 必须是字符串数组"))

        if (queries.isEmpty()) {
            return ToolResult.Error(name, ToolError.ValidationError("queries 不能为空"))
        }
        if (queries.size > toolConfig.maxBatchSize) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("单次最多 ${toolConfig.maxBatchSize} 个关键词，当前 ${queries.size} 个"),
            )
        }

        val library = (args["library"] as? String)?.lowercase()?.trim()
        val limitPerQuery = ((args["limitPerQuery"] as? Number)?.toInt() ?: 3).coerceIn(1, 20)

        val results = JSONArray()
        val allMisses = mutableListOf<String>()

        for (query in queries) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) continue

            val matches = IconSearchEngine.searchWithFallback(trimmed, library, limitPerQuery)
            val item = JSONObject()
            item.put("query", trimmed)

            if (matches.isEmpty()) {
                missLog.record(trimmed, library)
                allMisses.add(trimmed)
                item.put("matches", JSONArray())
                item.put("iconKey", "⁉")
            } else {
                val matchesArr = JSONArray()
                matches.forEach { match ->
                    matchesArr.put(IconSearchEngine.toJsonObject(match))
                }
                item.put("matches", matchesArr)
                item.put("iconKey", matches.first().iconKey)
            }

            results.put(item)
        }

        val result = JSONObject().apply {
            put("results", results)
            if (allMisses.isNotEmpty()) {
                put("misses", JSONArray(allMisses))
                put("placeholderIconKey", "⁉")
            }
        }

        return ToolResult.Success(name, result.toString())
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "results": [
                {
                  "query": "code",
                  "matches": [
                    {"iconKey":"hi:Code","name":"Code","library":"hi","keywords":"code,programming,代码,编程"},
                    {"iconKey":"mi:filled:Code","name":"Code","library":"mi","keywords":"code,programming,developer,代码,编程,开发"}
                  ],
                  "iconKey": "hi:Code"
                },
                {
                  "query": "sleep",
                  "matches": [
                    {"iconKey":"hi:Moon","name":"Moon","library":"hi","keywords":"moon,night,月亮,夜晚"},
                    {"iconKey":"😴","name":"sleeping","library":"emoji","keywords":"睡觉,困,sleep,zzz"}
                  ],
                  "iconKey": "hi:Moon"
                }
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("VALIDATION_ERROR", "queries 必须是字符串数组", "传入非数组"),
            ErrorExample("VALIDATION_ERROR", "queries 不能为空", "传入空数组"),
        ),
        usageExamples = listOf(
            """batchSearchIcons(queries=["code","review","sleep","exercise"])""",
            """batchSearchIcons(queries=["工作","学习","娱乐"], library="hi")""",
        ),
    )
}
