package com.nltimer.core.tools.match

import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.TagRepository
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * 工具：**搜索** 模式 —— substring（子串）匹配活动 / 标签
 *
 * 适用场景：
 * - 搜索框：输入"天" → 命中 "今天" / "明天" / "后天"（模糊找相关）
 * - AI Agent 想模糊定位用户提到的实体
 *
 * 匹配策略：
 * - 默认对每一项优先匹配 `keywords` 字段（按 `,` `;` `空白` 拆分多个候选）
 * - 当某项 `keywords` 为空 / null 时，回退到匹配 `name` 字段
 * - useRegex=false（默认）：候选含 query 子串即命中（受 caseSensitive 控制）
 * - useRegex=true：用 [Regex.containsMatchIn]，找到任意位置子串即可；解析失败返回 ValidationError
 * - 默认排除已归档项（includeArchived=true 才纳入）
 *
 * 与 [SelectActivitiesAndTagsTool] 的差异：
 * - 本工具是 substring（"包含"语义），适合"找相关项"
 * - Select 工具是 token-equality（"完全相等"语义），适合"用户从候选项中选中目标"
 */
@Singleton
class SearchActivitiesAndTagsTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "searchActivitiesAndTags"
    override val description: String =
        "搜索（substring 子串）：输入「天」会同时命中今天 / 明天 / 后天；适合搜索框，不适合候选选择"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "query",
            description = "查询文本（substring 或 regex，取决于 useRegex）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = MAX_QUERY_LENGTH),
        ),
        ToolParameter(
            name = "useRegex",
            description = "true 时把 query 当作正则；用 containsMatchIn 子串扫描；解析失败返回 ValidationError",
            type = ParameterType.BOOLEAN,
            required = false,
            default = false,
        ),
        ToolParameter(
            name = "caseSensitive",
            description = "是否区分大小写",
            type = ParameterType.BOOLEAN,
            required = false,
            default = false,
        ),
        ToolParameter(
            name = "scope",
            description = "搜索范围：activities / tags / both",
            type = ParameterType.STRING,
            required = false,
            default = SCOPE_BOTH,
            constraints = ParameterConstraint(
                enum = listOf(SCOPE_ACTIVITIES, SCOPE_TAGS, SCOPE_BOTH),
            ),
        ),
        ToolParameter(
            name = "includeArchived",
            description = "是否包含已归档项；默认 false 排除",
            type = ParameterType.BOOLEAN,
            required = false,
            default = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val query = args["query"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("query 必须是字符串"))
        val useRegex = (args["useRegex"] as? Boolean) ?: false
        val caseSensitive = (args["caseSensitive"] as? Boolean) ?: false
        val scope = (args["scope"] as? String) ?: SCOPE_BOTH
        val includeArchived = (args["includeArchived"] as? Boolean) ?: false

        val matcher: FieldMatcher = try {
            buildSubstringMatcher(query, useRegex, caseSensitive)
        } catch (e: IllegalArgumentException) {
            return ToolResult.Error(name, ToolError.ValidationError(e.message ?: "正则解析失败"))
        }

        return runCatching {
            val matchedActivities = if (scope == SCOPE_TAGS) {
                emptyList()
            } else {
                val source = if (includeArchived) {
                    activityRepository.getAll().first()
                } else {
                    activityRepository.getAllActive().first()
                }
                source.mapNotNull { activity ->
                    matchItem(matcher, activity.keywords, activity.name)?.let { field ->
                        JSONObject().apply {
                            put("id", activity.id)
                            put("name", activity.name)
                            put("matchedField", field)
                        }
                    }
                }
            }

            val matchedTags = if (scope == SCOPE_ACTIVITIES) {
                emptyList()
            } else {
                val source = if (includeArchived) {
                    tagRepository.getAll().first()
                } else {
                    tagRepository.getAllActive().first()
                }
                source.mapNotNull { tag ->
                    matchItem(matcher, tag.keywords, tag.name)?.let { field ->
                        JSONObject().apply {
                            put("id", tag.id)
                            put("name", tag.name)
                            put("matchedField", field)
                        }
                    }
                }
            }

            val result = JSONObject().apply {
                put("query", query)
                put("useRegex", useRegex)
                put("mode", "search")
                put("activities", JSONArray(matchedActivities))
                put("tags", JSONArray(matchedTags))
            }
            ToolResult.Success(
                name = name,
                data = result.toString(),
            )
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "匹配失败"))
        }
    }

    /** 构造子串匹配器；正则模式使用 containsMatchIn（任意位置子串） */
    @Throws(IllegalArgumentException::class)
    private fun buildSubstringMatcher(
        query: String,
        useRegex: Boolean,
        caseSensitive: Boolean,
    ): FieldMatcher {
        return if (useRegex) {
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = try {
                Regex(query, options)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("正则解析失败: ${e.message}")
            }
            FieldMatcher { candidate -> regex.containsMatchIn(candidate) }
        } else {
            val needle = if (caseSensitive) query else query.lowercase()
            FieldMatcher { candidate ->
                val haystack = if (caseSensitive) candidate else candidate.lowercase()
                needle in haystack
            }
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
              "query": "天",
              "useRegex": false,
              "mode": "search",
              "activities": [
                { "id": 1, "name": "今天", "matchedField": "name" },
                { "id": 2, "name": "明天", "matchedField": "name" },
                { "id": 3, "name": "后天", "matchedField": "name" }
              ],
              "tags": []
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "正则解析失败: ...",
                scenario = "useRegex=true 但 query 是非法正则",
            ),
        ),
        usageExamples = listOf(
            "searchActivitiesAndTags(query=\"跑\")",
            "searchActivitiesAndTags(query=\"^运动\", useRegex=true)",
            "searchActivitiesAndTags(query=\"work\", scope=\"tags\", caseSensitive=true)",
        ),
    )
}
