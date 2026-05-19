package com.nltimer.core.tools.timing

import com.nltimer.core.data.repository.BehaviorRepository
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
class QueryCurrentBehaviorTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "queryCurrentBehavior"
    override val description: String = "查询当前正在进行的行为记录，含活动名/标签/已持续时间；没有则返回 null"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val current = behaviorRepository.getCurrentBehavior().first()
            if (current == null) {
                ToolResult.Success(name, null)
            } else {
                val details = behaviorRepository.getBehaviorWithDetails(current.id)
                if (details == null) {
                    ToolResult.Success(name, null)
                } else {
                    val b = details.behavior
                    val a = details.activity
                    val now = System.currentTimeMillis()
                    val durationMinutes = ((now - b.startTime) / 60_000).toInt().coerceAtLeast(0)
                    val tagsArr = JSONArray()
                    details.tags.forEach { tag -> tagsArr.put(tag.name) }
                    val obj = JSONObject().apply {
                        put("id", b.id)
                        put("activityId", a.id)
                        put("activityName", a.name)
                        put("iconKey", a.iconKey ?: JSONObject.NULL)
                        put("startTime", TimeUtils.formatIso(b.startTime))
                        put("durationMinutes", durationMinutes)
                        put("status", b.status.name)
                        put("note", b.note ?: JSONObject.NULL)
                        put("tags", tagsArr)
                    }
                    ToolResult.Success(name, obj.toString())
                }
            }
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "查询当前行为失败"),
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
            {
              "id": 42,
              "activityId": 7,
              "activityName": "本职工作",
              "iconKey": "💼",
              "startTime": "2026-05-19T14:00:00+08:00",
              "durationMinutes": 30,
              "status": "ACTIVE",
              "note": "需求评审",
              "tags": ["后端", "重点"]
            }
            // 或 null（无正在进行的行为）
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "INTERNAL_ERROR",
                message = "查询当前行为失败",
                scenario = "数据库读取异常",
            ),
        ),
        usageExamples = listOf(
            "queryCurrentBehavior()",
        ),
    )
}
