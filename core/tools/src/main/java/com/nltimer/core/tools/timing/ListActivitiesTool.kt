package com.nltimer.core.tools.timing

import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
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

@Singleton
class ListActivitiesTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "listActivities"
    override val description: String = "列出所有未归档活动，含分组名和关联标签，供选择计时目标"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val activities = activityRepository.getAllActive().first()
            val groups = activityRepository.getAllGroups().first()
            val groupMap = groups.associateBy { it.id }
            val arr = JSONArray()
            for (a in activities) {
                val tags = tagRepository.getByActivityId(a.id).first()
                val tagsArr = JSONArray()
                tags.forEach { t ->
                    tagsArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                    })
                }
                val groupName = a.groupId?.let { groupMap[it]?.name }
                arr.put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("iconKey", a.iconKey ?: JSONObject.NULL)
                    put("keywords", a.keywords ?: JSONObject.NULL)
                    put("groupName", groupName ?: JSONObject.NULL)
                    put("usageCount", a.usageCount)
                    put("tags", tagsArr)
                })
            }
            ToolResult.Success(name, arr.toString())
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "查询活动列表失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            [
              {
                "id": 1, "name": "编程", "iconKey": "code", "keywords": "编程,coding",
                "groupName": "工作", "usageCount": 12,
                "tags": [{"id": 3, "name": "后端"}, {"id": 5, "name": "重点"}]
              },
              {
                "id": 2, "name": "阅读", "iconKey": "book", "keywords": null,
                "groupName": null, "usageCount": 5, "tags": []
              }
            ]
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "INTERNAL_ERROR",
                message = "查询活动列表失败",
                scenario = "数据库读取异常",
            ),
        ),
        usageExamples = listOf(
            "listActivities()",
        ),
    )
}
