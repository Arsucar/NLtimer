package com.nltimer.core.tools.timing

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
class GetBehaviorDetailTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "getBehaviorDetail"
    override val description: String = "查询单个行为的完整详情：活动名、标签、时长、预估vs实际、成就等级"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "id",
            description = "行为 id",
            type = ParameterType.NUMBER,
            required = true,
            constraints = ParameterConstraint(minValue = 1),
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val id = (args["id"] as? Number)?.toLong()
            ?: return ToolResult.Error(name, ToolError.ValidationError("id 必填"))

        return runCatching {
            val details = behaviorRepository.getBehaviorWithDetails(id)
                ?: return@runCatching ToolResult.Error(name, ToolError.NotFound("行为不存在: id=$id"))

            val b = details.behavior
            val a = details.activity
            val now = System.currentTimeMillis()
            val end = b.endTime ?: now
            val durationMs = end - b.startTime
            val durationMinutes = (durationMs / 60_000).toInt().coerceAtLeast(0)
            val estimatedMinutes = b.estimatedDuration?.let { (it / 60_000).toInt() }
            val actualMinutes = b.actualDuration?.let { (it / 60_000).toInt() }

            val tagsArr = JSONArray()
            details.tags.forEach { t ->
                tagsArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("category", t.category ?: JSONObject.NULL)
                })
            }

            val obj = JSONObject().apply {
                put("id", b.id)
                put("activityId", a.id)
                put("activityName", a.name)
                put("iconKey", a.iconKey ?: JSONObject.NULL)
                put("startTime", TimeUtils.formatIso(b.startTime))
                put("endTime", b.endTime?.let { TimeUtils.formatIso(it) } ?: JSONObject.NULL)
                put("durationMinutes", durationMinutes)
                put("status", b.status.name)
                put("note", b.note ?: JSONObject.NULL)
                put("wasPlanned", b.wasPlanned)
                put("estimatedDurationMinutes", estimatedMinutes ?: JSONObject.NULL)
                put("actualDurationMinutes", actualMinutes ?: JSONObject.NULL)
                put("achievementLevel", b.achievementLevel ?: JSONObject.NULL)
                put("tags", tagsArr)
            }
            ToolResult.Success(name, obj.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "获取行为详情失败"))
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
              "id": 42, "activityId": 7, "activityName": "本职工作", "iconKey": "💼",
              "startTime": "2026-05-19T09:00:00+08:00", "endTime": "2026-05-19T12:30:00+08:00",
              "durationMinutes": 210, "status": "COMPLETED", "note": "需求评审",
              "wasPlanned": true, "estimatedDurationMinutes": 240, "actualDurationMinutes": 210,
              "achievementLevel": 4,
              "tags": [{"id": 3, "name": "后端", "category": "技术"}, {"id": 5, "name": "重点", "category": null}]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("NOT_FOUND", "行为不存在: id=999", "id 无效"),
        ),
        usageExamples = listOf("getBehaviorDetail(id=42)"),
    )
}
