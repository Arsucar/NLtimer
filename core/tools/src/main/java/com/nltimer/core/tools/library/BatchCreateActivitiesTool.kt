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
import com.nltimer.core.tools.timing.IconSearchEngine
import com.nltimer.core.tools.timing.IconSearchMissLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BatchCreateActivitiesTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val categoryRepository: CategoryRepository,
    private val toolConfig: ToolConfig,
    private val iconSearchMissLog: IconSearchMissLog,
) : ToolDefinition {

    override val name: String = "batchCreateActivities"
    override val description: String =
        "批量创建活动；传入活动列表，自动去重并创建不存在的活动，返回 created/skipped 结果。autoIcon=true 时自动为每个活动匹配图标。"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "activities",
            description = "活动列表，每个元素含 name(必填)、groupName(可选,默认'预制菜')、iconKey(可选)、color(可选)",
            type = ParameterType.ARRAY,
            required = true,
        ),
        ToolParameter(
            name = "autoIcon",
            description = "是否自动为未指定 iconKey 的活动匹配图标（默认 true）。自动匹配基于活动名称搜索图标库，无需手动调 searchIcons",
            type = ParameterType.BOOLEAN,
            required = false,
            default = true,
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

        val autoIcon = (args["autoIcon"] as? Boolean) ?: true

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

            if (activityRepository.getByName(actName) != null) {
                skipped.put(
                    JSONObject().put("name", actName).put("reason", "活动已存在")
                )
                continue
            }

            val groupName = (item["groupName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_GROUP_NAME
            val groupId = ensureActivityGroupId(groupName)

            val color = (item["color"] as? Number)?.toLong() ?: RandomMonetColor.next()

            val iconResult = resolveIconKey(item["iconKey"] as? String, actName, autoIcon)

            val newId = activityRepository.insert(
                Activity(
                    id = 0L,
                    name = actName,
                    iconKey = iconResult.iconKey,
                    keywords = null,
                    groupId = groupId,
                    isPreset = false,
                    isArchived = false,
                    color = color,
                    usageCount = 0,
                ),
            )
            val createdObj = JSONObject()
                .put("id", newId)
                .put("name", actName)
                .put("iconKey", iconResult.iconKey ?: "⁉")
                .put("iconMatched", iconResult.iconKey != null)
            if (iconResult.fallbackLibrary != null) {
                createdObj.put("fallbackLibrary", iconResult.fallbackLibrary)
                createdObj.put("fallbackNote", "hi库无匹配，使用${iconResult.fallbackLibrary}库")
            }
            created.put(createdObj)
        }

        val result = JSONObject()
            .put("created", created)
            .put("skipped", skipped)

        return ToolResult.Success(this.name, result.toString())
    }

    private data class IconResult(
        val iconKey: String?,
        val fallbackLibrary: String?,
    )

    private suspend fun resolveIconKey(
        explicitIconKey: String?,
        activityName: String,
        autoIcon: Boolean,
    ): IconResult {
        explicitIconKey?.takeIf { it.isNotBlank() }?.let { return IconResult(it, null) }
        if (!autoIcon) return IconResult(null, null)

        val queries = listOf(activityName) + inferRelatedKeywords(activityName)
        val result = IconSearchEngine.searchMultipleQueries(queries, limit = 1)
        // PRD: 使用 mi/emoji 时记录到 icon_search_miss 表
        if (result.fallbackLibrary != null) {
            iconSearchMissLog.record(activityName, result.fallbackLibrary)
        }
        return IconResult(
            iconKey = result.match?.iconKey,
            fallbackLibrary = result.fallbackLibrary,
        )
    }

    /**
     * 推断活动的相关关键词，用于多关键词图标搜索。
     */
    private fun inferRelatedKeywords(activityName: String): List<String> {
        val keywordMap = mapOf(
            "阅读" to listOf("看书", "读书", "book", "read"),
            "跑步" to listOf("慢跑", "run", "jogging", "运动"),
            "编程" to listOf("代码", "开发", "code", "programming"),
            "学习" to listOf("阅读", "书", "study", "book"),
            "工作" to listOf("办公", "公文包", "work", "briefcase"),
            "休息" to listOf("放松", "relax", "spa"),
            "冥想" to listOf("瑜伽", "meditation", "yoga", "放松"),
            "购物" to listOf("买东西", "shopping", "store"),
            "散步" to listOf("走路", "walk"),
            "做饭" to listOf("烹饪", "cooking", "食物", "餐厅"),
            "运动" to listOf("健身", "fitness", "exercise"),
            "看电影" to listOf("电影", "movie", "film"),
            "听音乐" to listOf("音乐", "music", "耳机"),
            "写代码" to listOf("编程", "开发", "code", "programming"),
            "开会" to listOf("会议", "meeting", "讨论"),
            "午睡" to listOf("小睡", "睡觉", "nap", "sleep"),
            "洗澡" to listOf("淋浴", "shower", "bath"),
            "整理" to listOf("收拾", "clean", "tidy"),
            "写日记" to listOf("日记", "journal", "diary"),
        )
        return keywordMap[activityName] ?: emptyList()
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
              "created": [
                {"id": 14, "name": "阅读", "iconKey": "hi:BookOpen01", "iconMatched": true},
                {"id": 15, "name": "跑步", "iconKey": "🏃", "iconMatched": true, "fallbackLibrary": "emoji", "fallbackNote": "hi库无匹配，使用emoji库"}
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
            """batchCreateActivities(activities=[{"name":"看小说","groupName":"娱乐","iconKey":"📺"}], autoIcon=false)""",
        ),
    )

    private companion object {
        const val DEFAULT_GROUP_NAME = "预制菜"
    }
}
