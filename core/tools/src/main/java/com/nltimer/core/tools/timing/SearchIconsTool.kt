package com.nltimer.core.tools.timing

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
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class SearchIconsTool @Inject constructor(
    private val missLog: IconSearchMissLog,
) : ToolDefinition {

    override val name: String = "searchIcons"
    override val description: String = "搜索可用图标，支持中英文关键词，返回 iconKey 列表供 createActivity/createTag/bulkUpdateActivities 使用。搜不到时会自动尝试同义词。"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.NONE
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "query",
            description = "搜索关键词（中英文均可，如 睡觉/sleep/时间/time/工作/work）",
            type = ParameterType.STRING,
            required = true,
        ),
        ToolParameter(
            name = "library",
            description = "图标库过滤：hi (HugeIcons) / mi (Material Icons) / emoji；默认返回所有",
            type = ParameterType.STRING,
            required = false,
            constraints = null,
        ),
        ToolParameter(
            name = "limit",
            description = "返回数量上限，默认 20，最大 50",
            type = ParameterType.NUMBER,
            required = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val query = (args["query"] as? String)?.trim()
            ?: return ToolResult.Error(name, ToolError.ValidationError("query 必填"))
        val library = (args["library"] as? String)?.lowercase()?.trim()
        val limit = ((args["limit"] as? Number)?.toInt() ?: 20).coerceIn(1, 50)

        val results = IconSearchEngine.searchWithFallback(query, library, limit)

        if (results.isEmpty()) {
            missLog.record(query, library)
            return ToolResult.Success(name, """{"matches":[],"missLogged":true,"placeholderIconKey":"⁉","placeholderNote":"未找到匹配图标，已记录此需求（累计未满足${missLog.count()}条）。建议先使用 ⁉ 占位，后续版本更新图标库后可批量替换","suggestion":"请尝试其他关键词或缩短查询"}""")
        }

        val matchesArr = JSONArray()
        results.forEach { match ->
            matchesArr.put(IconSearchEngine.toJsonObject(match))
        }

        val result = JSONObject().apply {
            put("query", query)
            put("totalMatches", results.size)
            put("returned", results.size)
            put("matches", matchesArr)
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
              "query": "sleep",
              "totalMatches": 3,
              "returned": 3,
              "matches": [
                {"iconKey":"hi:Moon","name":"Moon","library":"hi","keywords":"moon,night,月亮,夜晚,睡觉,sleep"},
                {"iconKey":"mi:filled:NightsStay","name":"NightsStay","library":"mi","keywords":"night,dark"},
                {"iconKey":"😴","name":"sleeping","library":"emoji","keywords":"睡觉,困,sleep,zzz"}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("VALIDATION_ERROR", "query 必填", "未传搜索关键词"),
        ),
        usageExamples = listOf(
            """searchIcons(query="睡觉")""",
            """searchIcons(query="work", library="hi")""",
            """searchIcons(query="时间", limit=10)""",
        ),
    )
}
