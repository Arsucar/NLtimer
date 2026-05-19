package com.nltimer.core.tools.library

import com.nltimer.core.data.repository.CategoryRepository
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
class BatchCreateTagCategoriesTool @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val toolConfig: ToolConfig,
) : ToolDefinition {

    override val name: String = "batchCreateTagCategories"
    override val description: String =
        "批量声明标签分类；传入分类名列表，自动去重，返回 created/skipped 结果"
    override val category: ToolCategory = ToolCategory.CATEGORY
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "names",
            description = "分类名列表，最多 $MAX_BATCH_SIZE 条",
            type = ParameterType.ARRAY,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val rawList = args["names"] as? List<*>
        if (rawList.isNullOrEmpty()) {
            return ToolResult.Error(
                this.name,
                ToolError.ValidationError("names 不能为空"),
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

        for (raw in rawList) {
            val name = (raw as? String)?.trim().orEmpty()
            if (name.isEmpty()) {
                skipped.put(JSONObject().put("name", "").put("reason", "name 不能为空"))
                continue
            }
            if (name.length > MAX_NAME_LENGTH) {
                skipped.put(JSONObject().put("name", name).put("reason", "名称超过 $MAX_NAME_LENGTH 字"))
                continue
            }

            runCatching {
                val isNew = categoryRepository.addTagCategoryStub(name)
                if (isNew) {
                    created.put(name)
                } else {
                    skipped.put(JSONObject().put("name", name).put("reason", "标签分类已存在"))
                }
            }.onFailure {
                skipped.put(JSONObject().put("name", name).put("reason", it.message ?: "声明失败"))
            }
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
              "created": ["优先级", "场景"],
              "skipped": [{"name": "生活", "reason": "标签分类已存在"}]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "names 不能为空",
                scenario = "传入空列表",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "单次最多 20 条，当前 25 条",
                scenario = "超出批量上限",
            ),
        ),
        usageExamples = listOf(
            """batchCreateTagCategories(names=["优先级","场景","来源"])""",
        ),
    )

    private companion object {
        const val MAX_BATCH_SIZE = 20
        const val MAX_NAME_LENGTH = 50
    }
}
