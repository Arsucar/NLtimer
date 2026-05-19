package com.nltimer.core.tools.library

import com.nltimer.core.data.model.Activity
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

/**
 * 工具：批量创建活动
 *
 * 一次调用创建多条活动，自动去重，返回 created / skipped 列表。
 * 分组不存在时自动创建（复用 ensureActivityGroupId 逻辑）。
 */
@Singleton
class BatchCreateActivitiesTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val categoryRepository: CategoryRepository,
    private val toolConfig: ToolConfig,
) : ToolDefinition {

    override val name: String = "batchCreateActivities"
    override val description: String =
        "批量创建活动；传入活动列表，自动去重并创建不存在的活动，返回 created/skipped 结果"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "activities",
            description = "活动列表，每个元素含 name(必填)、groupName(可选,默认'预制菜')、iconKey(可选)、color(可选)",
            type = ParameterType.ARRAY,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val rawList = args["activities"] as? List<Map<String, Any?>>
        if (rawList.isNullOrEmpty()) {
            return ToolResult.Error(
                this.name,
                ToolError.ValidationError("activities 不能为空"),
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
            val actName = (item["name"] as? String)?.trim().orEmpty()
            if (actName.isEmpty()) {
                skipped.put(
                    JSONObject().put("name", "").put("reason", "name 不能为空")
                )
                continue
            }

            // 查重
            if (activityRepository.getByName(actName) != null) {
                skipped.put(
                    JSONObject().put("name", actName).put("reason", "活动已存在")
                )
                continue
            }

            // 确保分组存在
            val groupName = (item["groupName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_GROUP_NAME
            val groupId = ensureActivityGroupId(groupName)

            // 颜色
            val color = (item["color"] as? Number)?.toLong() ?: RandomMonetColor.next()

            // 图标
            val iconKey = (item["iconKey"] as? String)?.takeIf { it.isNotBlank() }

            // 插入
            val newId = activityRepository.insert(
                Activity(
                    id = 0L,
                    name = actName,
                    iconKey = iconKey,
                    keywords = null,
                    groupId = groupId,
                    isPreset = false,
                    isArchived = false,
                    color = color,
                    usageCount = 0,
                ),
            )
            created.put(
                JSONObject().put("id", newId).put("name", actName)
            )
        }

        val result = JSONObject()
            .put("created", created)
            .put("skipped", skipped)

        return ToolResult.Success(this.name, result.toString())
    }

    /**
     * 确保分组存在，不存在则创建并返回 id
     *
     * 逻辑与 [CreateActivityTool.ensureActivityGroupId] 一致，
     * 因 Kotlin abstract class 不支持跨工具共享 private 方法，故在此重复。
     */
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
              "created": [
                {"id": 14, "name": "阅读"},
                {"id": 15, "name": "跑步"}
              ],
              "skipped": [
                {"name": "编程", "reason": "活动已存在"}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "activities 不能为空",
                scenario = "传入空列表",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "单次最多 20 条，当前 25 条",
                scenario = "超出批量上限",
            ),
        ),
        usageExamples = listOf(
            """batchCreateActivities(activities=[{"name":"阅读"},{"name":"跑步"}])""",
            """batchCreateActivities(activities=[{"name":"看小说","groupName":"娱乐","iconKey":"📺"}])""",
        ),
    )

    private companion object {
        const val MAX_BATCH_SIZE = 20
        const val DEFAULT_GROUP_NAME = "预制菜"
    }
}
