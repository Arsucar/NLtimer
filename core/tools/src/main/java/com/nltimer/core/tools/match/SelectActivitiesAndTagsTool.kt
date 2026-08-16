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
 * 工具：**选择** 模式 —— token 级精确相等（exact equality）匹配活动 / 标签
 *
 * 适用场景：
 * - 候选选择：用户输入"计划" → 命中名为"计划"的活动 + keywords 含"计划"的标签"计划目标"，
 *   不会被"今天 / 明天 / 后天"这种 substring 模糊匹配误中
 * - AI Agent 想精确解析用户提到的某个具体实体
 *
 * 匹配策略（与 [SearchActivitiesAndTagsTool] 唯一差异在 matcher 行为）：
 * - 默认对每一项优先匹配 `keywords` 字段（按 `,` `;` `空白` 拆分多个候选）
 * - 当某项 `keywords` 为空 / null 时，回退到匹配 `name` 字段
 * - useRegex=false（默认）：候选必须 **完全等于** query（受 caseSensitive 控制）
 * - useRegex=true：用 [Regex.matches]，候选必须被正则 **完整覆盖**；解析失败返回 ValidationError
 * - 默认排除已归档项（includeArchived=true 才纳入）
 *
 * 与 [SearchActivitiesAndTagsTool] 的差异：
 * - 本工具是 token-equality（"完全相等"语义），适合"用户从候选项中选中目标"
 * - Search 工具是 substring（"包含"语义），适合"找相关项"
 */
@Singleton
class SelectActivitiesAndTagsTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "selectActivitiesAndTags"
    override val description: String =
        "选择（token 精确相等）：输入「计划」只命中名为计划的活动与 keywords 含计划的标签；不会被今天/明天误中"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "query",
            description = "查询文本（必须与候选完全相等；正则模式下必须 matches 整段）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = MAX_QUERY_LENGTH),
        ),
        ToolParameter(
            name = "useRegex",
            description = "true 时把 query 当作正则；用 matches 完整匹配；解析失败返回 ValidationError",
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
            buildExactMatcher(query, useRegex, caseSensitive)
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
                put("mode", "select")
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

    /** 构造精确匹配器；正则模式使用 matches（要求完整覆盖整段候选） */
    @Throws(IllegalArgumentException::class)
    private fun buildExactMatcher(
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
            FieldMatcher { candidate -> regex.matches(candidate) }
        } else {
            if (caseSensitive) {
                FieldMatcher { candidate -> candidate == query }
            } else {
                val needle = query.lowercase()
                FieldMatcher { candidate -> candidate.lowercase() == needle }
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
              "query": "计划",
              "useRegex": false,
              "mode": "select",
              "activities": [
                { "id": 4, "name": "计划", "matchedField": "name" }
              ],
              "tags": [
                { "id": 1, "name": "计划目标", "matchedField": "keywords" }
              ]
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
            "selectActivitiesAndTags(query=\"计划\")",
            "selectActivitiesAndTags(query=\"^运动\\\$\", useRegex=true)",
            "selectActivitiesAndTags(query=\"work\", scope=\"tags\", caseSensitive=true)",
        ),
    )
}
