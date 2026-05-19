package com.nltimer.core.tools.library

import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.ActivityRepository
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
class BatchDeleteActivitiesTool @Inject constructor(
    private val activityManagementRepository: ActivityManagementRepository,
    private val activityRepository: ActivityRepository,
    private val toolConfig: ToolConfig,
) : ToolDefinition {

    override val name: String = "batchDeleteActivities"
    override val description: String = "批量删除活动（含关联行为记录和标签绑定），返回已删除和未找到的 ID 列表"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.FULL

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "ids",
            description = "要删除的活动 ID 列表，最多 $MAX_BATCH_SIZE 条",
            type = ParameterType.ARRAY,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val rawIds = args["ids"] as? List<*>
        if (rawIds.isNullOrEmpty()) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("ids 不能为空"),
            )
        }
        if (rawIds.size > toolConfig.maxBatchSize) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("单次最多 ${toolConfig.maxBatchSize} 条，当前 ${rawIds.size} 条"),
            )
        }

        val deleted = JSONArray()
        val notFound = JSONArray()

        for (rawId in rawIds) {
            val id = (rawId as? Number)?.toLong()
            if (id == null) {
                notFound.put(JSONObject().put("id", rawId).put("reason", "id 无效"))
                continue
            }
            val existing = activityRepository.getById(id)
            if (existing == null) {
                notFound.put(JSONObject().put("id", id).put("reason", "活动不存在"))
                continue
            }
            runCatching {
                activityManagementRepository.deleteActivity(id)
                deleted.put(id)
            }.onFailure {
                notFound.put(JSONObject().put("id", id).put("reason", it.message ?: "删除失败"))
            }
        }

        val result = JSONObject()
            .put("deleted", deleted)
            .put("notFound", notFound)

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
              "deleted": [1, 2, 3],
              "notFound": [{"id": 99, "reason": "活动不存在"}]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "ids 不能为空",
                scenario = "传入空列表",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "单次最多 20 条，当前 25 条",
                scenario = "超出批量上限",
            ),
        ),
        usageExamples = listOf(
            """batchDeleteActivities(ids=[1, 2, 3])""",
            """batchDeleteActivities(ids=[42, 99])""",
        ),
    )

    private companion object {
        const val MAX_BATCH_SIZE = 20
    }
}
