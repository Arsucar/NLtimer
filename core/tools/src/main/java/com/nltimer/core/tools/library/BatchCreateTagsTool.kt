package com.nltimer.core.tools.library

import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
import com.nltimer.core.tools.ParameterConstraint
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
class BatchCreateTagsTool @Inject constructor(
    private val tagRepository: TagRepository,
    private val toolConfig: ToolConfig,
) : ToolDefinition {

    override val name: String = "batchCreateTags"
    override val description: String =
        "批量创建标签；传入标签列表，自动去重并创建不存在的标签，返回 created/skipped 结果"
    override val category: ToolCategory = ToolCategory.TAG
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "tags",
            description = "标签列表，每个元素含 name(必填)、category(可选,默认'预制菜')、iconKey(可选,默认'#')、color(可选)",
            type = ParameterType.ARRAY,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val rawList = args["tags"] as? List<Map<String, Any?>>
        if (rawList.isNullOrEmpty()) {
            return ToolResult.Error(
                this.name,
                ToolError.ValidationError("tags 不能为空"),
            )
        }
        if (rawList.size > toolConfig.maxBatchSize) {
            return ToolResult.Error(
                this.name,
                ToolError.ValidationError("单次最多 ${toolConfig.maxBatchSize} 条，当前 ${rawList.size} 条"),
            )
        }

        val created = JSONArray()
        val skipped = JSONArray()

        for (item in rawList) {
            val tagName = (item["name"] as? String)?.trim().orEmpty()
            if (tagName.isEmpty()) {
                skipped.put(JSONObject().put("name", "").put("reason", "name 不能为空"))
                continue
            }
            if (tagName.length > MAX_NAME_LENGTH) {
                skipped.put(JSONObject().put("name", tagName).put("reason", "名称超过 $MAX_NAME_LENGTH 字"))
                continue
            }

            if (tagRepository.getByName(tagName) != null) {
                skipped.put(JSONObject().put("name", tagName).put("reason", "标签已存在"))
                continue
            }

            val category = (item["category"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_CATEGORY
            val iconKey = (item["iconKey"] as? String)?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ICON_KEY
            val color = (item["color"] as? Number)?.toLong() ?: RandomMonetColor.next()

            val newId = tagRepository.insert(
                Tag(
                    id = 0L,
                    name = tagName,
                    color = color,
                    iconKey = iconKey,
                    category = category,
                    priority = 0,
                    usageCount = 0,
                    sortOrder = 0,
                    keywords = null,
                    isArchived = false,
                ),
            )
            created.put(JSONObject().put("id", newId).put("name", tagName))
        }

        val result = JSONObject()
            .put("created", created)
            .put("skipped", skipped)

        return ToolResult.Success(this.name, result.toString())
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "created": [{"id": 14, "name": "重要"}, {"id": 15, "name": "紧急"}],
              "skipped": [{"name": "吃饭", "reason": "标签已存在"}]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "tags 不能为空",
                scenario = "传入空列表",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "单次最多 20 条，当前 25 条",
                scenario = "超出批量上限",
            ),
        ),
        usageExamples = listOf(
            """batchCreateTags(tags=[{"name":"重要"},{"name":"紧急"}])""",
            """batchCreateTags(tags=[{"name":"工作","category":"分类","iconKey":"💼"}])""",
        ),
    )

    private companion object {
        const val MAX_BATCH_SIZE = 20
        const val MAX_NAME_LENGTH = 50
        const val DEFAULT_CATEGORY = "预制菜"
        const val DEFAULT_ICON_KEY = "#"
    }
}
