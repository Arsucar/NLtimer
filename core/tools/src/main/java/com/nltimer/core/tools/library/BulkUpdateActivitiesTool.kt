package com.nltimer.core.tools.library

import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.CategoryRepository
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
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BulkUpdateActivitiesTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val categoryRepository: CategoryRepository,
    private val toolConfig: ToolConfig,
) : ToolDefinition {

    override val name: String = "bulkUpdateActivities"
    override val description: String =
        "批量修改活动属性（分组、图标、颜色、名称），每个活动可独立指定不同字段，仅更新传入字段"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "updates",
            description = "更新列表，每个元素含 id(必填)、groupId(可选)、groupName(可选)、iconKey(可选)、color(可选)、name(可选)，最多 $MAX_BATCH_SIZE 条。groupName 不存在时自动创建",
            type = ParameterType.ARRAY,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val rawList = args["updates"] as? List<Map<String, Any?>>
        if (rawList.isNullOrEmpty()) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("updates 不能为空"),
            )
        }
        if (rawList.size > toolConfig.maxBatchSize) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("单次最多 ${toolConfig.maxBatchSize} 条，当前 ${rawList.size} 条"),
            )
        }

        val updated = JSONArray()
        val notFound = JSONArray()

        for (item in rawList) {
            val id = (item["id"] as? Number)?.toLong()
            if (id == null) {
                notFound.put(
                    JSONObject()
                        .put("id", item["id"] ?: JSONObject.NULL)
                        .put("reason", "id 无效")
                )
                continue
            }

            val existing = activityRepository.getById(id)
            if (existing == null) {
                notFound.put(JSONObject().put("id", id).put("reason", "活动不存在"))
                continue
            }

            // 解析 groupId：优先使用 groupId，其次使用 groupName
            val resolvedGroupId = when {
                item.containsKey("groupId") -> (item["groupId"] as? Number)?.toLong()
                item.containsKey("groupName") -> {
                    val groupName = (item["groupName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                    if (groupName != null) ensureActivityGroupId(groupName) else existing.groupId
                }
                else -> existing.groupId
            }

            val merged = existing.copy(
                name = (item["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: existing.name,
                iconKey = if (item.containsKey("iconKey")) (item["iconKey"] as? String) else existing.iconKey,
                color = if (item.containsKey("color")) (item["color"] as? Number)?.toLong() else existing.color,
                groupId = resolvedGroupId,
            )

            runCatching {
                activityRepository.update(merged)
                updated.put(id)
            }.onFailure {
                notFound.put(JSONObject().put("id", id).put("reason", it.message ?: "更新失败"))
            }
        }

        val result = JSONObject()
            .put("updated", updated)
            .put("notFound", notFound)

        return ToolResult.Success(name, result.toString())
    }

    private suspend fun ensureActivityGroupId(groupName: String): Long {
        val groups = activityRepository.getAllGroups().first()
        groups.firstOrNull { it.name == groupName }?.let { return it.id }
        categoryRepository.addActivityCategory(groupName)
        return activityRepository.getAllGroups().first()
            .firstOrNull { it.name == groupName }?.id
            ?: error("创建分组后仍未找到: $groupName")
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "updated": [1, 2],
              "notFound": [{"id": 99, "reason": "活动不存在"}]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "updates 不能为空",
                scenario = "传入空列表",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "单次最多 20 条，当前 25 条",
                scenario = "超出批量上限",
            ),
        ),
        usageExamples = listOf(
            """bulkUpdateActivities(updates=[{"id":1,"color":4278190335},{"id":2,"name":"新名称"}])""",
            """bulkUpdateActivities(updates=[{"id":3,"groupId":5,"iconKey":"01"}])""",
            """bulkUpdateActivities(updates=[{"id":1,"groupName":"生活"}])""",
        ),
    )

    private companion object {
        const val MAX_BATCH_SIZE = 20
    }
}
