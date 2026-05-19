package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.ActivityRepository
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
import kotlinx.coroutines.flow.first
import org.json.JSONObject

@Singleton
class UpdateBehaviorTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val activityRepository: ActivityRepository,
) : ToolDefinition {

    override val name: String = "updateBehavior"
    override val description: String = "修改已有行为的备注、起止时间或标签"
    override val category: ToolCategory = ToolCategory.BEHAVIOR
    override val accessLevel: AccessLevel = AccessLevel.WRITE
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "id",
            description = "行为 id",
            type = ParameterType.NUMBER,
            required = true,
            constraints = ParameterConstraint(minValue = 1),
        ),
        ToolParameter(
            name = "note",
            description = "新备注（可选）",
            type = ParameterType.STRING,
            required = false,
            constraints = ParameterConstraint(maxLength = 500),
        ),
        ToolParameter(
            name = "startTime",
            description = "新开始时间 ISO 8601（可选）",
            type = ParameterType.STRING,
            required = false,
        ),
        ToolParameter(
            name = "endTime",
            description = "新结束时间 ISO 8601（可选，传 null 清除）",
            type = ParameterType.STRING,
            required = false,
        ),
        ToolParameter(
            name = "tagIds",
            description = "替换的标签 id 列表（可选，传入则整体替换）",
            type = ParameterType.ARRAY,
            required = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val id = (args["id"] as? Number)?.toLong()
            ?: return ToolResult.Error(name, ToolError.ValidationError("id 必填"))

        return runCatching {
            val existing = behaviorRepository.getBehaviorWithDetails(id)
                ?: return@runCatching ToolResult.Error(name, ToolError.NotFound("行为不存在: id=$id"))

            val b = existing.behavior
            val note = args["note"] as? String
            val startStr = args["startTime"] as? String
            val endStr = args["endTime"] as? String
            val tagIdsRaw = args["tagIds"]

            if (note != null) {
                behaviorRepository.setNote(id, note)
            }
            if (startStr != null) {
                val startMs = TimeUtils.parseIsoToMillis(startStr)
                    ?: return@runCatching ToolResult.Error(name, ToolError.ValidationError("startTime 格式错误: $startStr"))
                behaviorRepository.setStartTime(id, startMs)
            }
            if (args.containsKey("endTime")) {
                if (endStr != null) {
                    val endMs = TimeUtils.parseIsoToMillis(endStr)
                        ?: return@runCatching ToolResult.Error(name, ToolError.ValidationError("endTime 格式错误: $endStr"))
                    behaviorRepository.setEndTime(id, endMs)
                } else if (b.status == BehaviorNature.COMPLETED) {
                    behaviorRepository.setEndTime(id, System.currentTimeMillis())
                }
            }
            if (tagIdsRaw != null) {
                val tagIds = parseLongList(tagIdsRaw)
                behaviorRepository.updateTagsForBehavior(id, tagIds)
            }

            val updated = behaviorRepository.getBehaviorWithDetails(id)
            val resultObj = JSONObject().apply {
                put("id", id)
                put("updated", true)
                if (updated != null) {
                    put("activityName", updated.activity.name)
                    put("startTime", TimeUtils.formatIso(updated.behavior.startTime))
                    put("endTime", updated.behavior.endTime?.let { TimeUtils.formatIso(it) } ?: JSONObject.NULL)
                    put("note", updated.behavior.note ?: JSONObject.NULL)
                    put("status", updated.behavior.status.name)
                }
            }
            ToolResult.Success(name, resultObj.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "更新行为失败"))
        }
    }

    private fun parseLongList(value: Any?): List<Long> = when (value) {
        is List<*> -> value.mapNotNull { (it as? Number)?.toLong() }
        is Array<*> -> value.mapNotNull { (it as? Number)?.toLong() }
        else -> emptyList()
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """{"id":42,"updated":true,"activityName":"本职工作","startTime":"2026-05-19T09:00:00+08:00","endTime":"2026-05-19T12:30:00+08:00","note":"需求评审（已修改）","status":"COMPLETED"}""",
        errorExamples = listOf(
            ErrorExample("NOT_FOUND", "行为不存在: id=999", "id 无效"),
            ErrorExample("VALIDATION_ERROR", "startTime 格式错误: abc", "时间格式非法"),
        ),
        usageExamples = listOf(
            """updateBehavior(id=42, note="补充备注")""",
            """updateBehavior(id=42, startTime="2026-05-19T10:00:00+08:00", endTime="2026-05-19T14:00:00+08:00")""",
            """updateBehavior(id=42, tagIds=[1,3,5])""",
        ),
    )
}
