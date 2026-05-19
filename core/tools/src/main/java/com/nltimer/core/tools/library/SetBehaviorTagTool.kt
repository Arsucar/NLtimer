package com.nltimer.core.tools.library

import com.nltimer.core.data.database.dao.BehaviorDao
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
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class SetBehaviorTagTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val behaviorDao: BehaviorDao,
) : ToolDefinition {

    override val name: String = "setBehaviorTag"
    override val description: String =
        "管理行为记录的标签关联；支持追加(add)、移除(remove)、替换(replace)三种模式，返回操作后的完整标签列表"
    override val category: ToolCategory = ToolCategory.ACTIVITIES
    override val accessLevel: AccessLevel = AccessLevel.FULL

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "behaviorId",
            description = "行为记录 ID",
            type = ParameterType.NUMBER,
            required = true,
            constraints = ParameterConstraint(minValue = 1),
        ),
        ToolParameter(
            name = "tagIds",
            description = "标签 ID 列表",
            type = ParameterType.ARRAY,
            required = true,
        ),
        ToolParameter(
            name = "mode",
            description = "操作模式：add（追加）、remove（移除）、replace（替换全部）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(enum = listOf("add", "remove", "replace")),
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val behaviorId = (args["behaviorId"] as? Number)?.toLong()
        if (behaviorId == null) {
            return ToolResult.Error(name, ToolError.ValidationError("behaviorId 必填"))
        }

        @Suppress("UNCHECKED_CAST")
        val tagIds = (args["tagIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }
        if (tagIds.isNullOrEmpty()) {
            return ToolResult.Error(name, ToolError.ValidationError("tagIds 不能为空"))
        }

        val mode = args["mode"] as? String
        if (mode == null || mode !in listOf("add", "remove", "replace")) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("mode 必须为 add、remove 或 replace"),
            )
        }

        val entity = behaviorDao.getById(behaviorId)
        if (entity == null) {
            return ToolResult.Error(name, ToolError.NotFound("行为记录 $behaviorId 不存在"))
        }

        return runCatching {
            when (mode) {
                "add" -> {
                    val currentTagIds = behaviorDao.getTagsForBehaviorSync(behaviorId)
                        .map { it.id }
                        .toSet()
                    val merged = (currentTagIds + tagIds).distinct()
                    behaviorRepository.updateTagsForBehavior(behaviorId, merged)
                }
                "remove" -> {
                    behaviorDao.removeTagCrossRefs(behaviorId, tagIds)
                }
                "replace" -> {
                    behaviorRepository.updateTagsForBehavior(behaviorId, tagIds)
                }
            }

            val finalTags = behaviorDao.getTagsForBehaviorSync(behaviorId)
            val tagsArray = JSONArray()
            for (tag in finalTags) {
                tagsArray.put(
                    JSONObject()
                        .put("id", tag.id)
                        .put("name", tag.name)
                        .put("color", tag.color)
                        .put("iconKey", tag.iconKey)
                        .put("category", tag.category)
                )
            }

            val result = JSONObject()
                .put("behaviorId", behaviorId)
                .put("tags", tagsArray)

            ToolResult.Success(name, result.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "操作失败"))
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
              "behaviorId": 42,
              "tags": [
                {"id": 1, "name": "工作", "color": null, "iconKey": null, "category": null},
                {"id": 3, "name": "重要", "color": 4278190335, "iconKey": "02", "category": "优先级"}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "behaviorId 必填",
                scenario = "未传入 behaviorId",
            ),
            ErrorExample(
                code = "NOT_FOUND",
                message = "行为记录 99 不存在",
                scenario = "传入不存在的 behaviorId",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "mode 必须为 add、remove 或 replace",
                scenario = "传入无效的 mode 值",
            ),
        ),
        usageExamples = listOf(
            """setBehaviorTag(behaviorId=42, tagIds=[1, 3], mode="add")""",
            """setBehaviorTag(behaviorId=42, tagIds=[1], mode="remove")""",
            """setBehaviorTag(behaviorId=42, tagIds=[1, 2, 3], mode="replace")""",
        ),
    )
}
