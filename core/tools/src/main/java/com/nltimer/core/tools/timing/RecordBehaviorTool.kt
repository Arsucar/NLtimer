package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.usecase.AddBehaviorUseCase
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * 工具：补录已结束行为（COMPLETED）
 *
 * 通过 [AddBehaviorUseCase] 写入，复用其冲突检测与时间吸附。
 * 检测到冲突时不直接返回错误文案，而是回报结构化 JSON：
 *
 * ```
 * {
 *   "code": "CONFLICT",
 *   "conflicts": [
 *     {"id":42, "activityId":3, "activityName":"工作",
 *      "startTime":"2026-05-17T10:00:00+08:00",
 *      "endTime":"2026-05-17T13:30:00+08:00"}
 *   ]
 * }
 * ```
 *
 * 调用方（LLM）据此向用户复述冲突区间并询问"覆盖 / 取消 / 调整时间"。
 */
@Singleton
class RecordBehaviorTool @Inject constructor(
    private val addBehaviorUseCase: AddBehaviorUseCase,
    private val behaviorRepository: BehaviorRepository,
    private val activityRepository: ActivityRepository,
) : ToolDefinition {

    override val name: String = "recordBehavior"
    override val description: String =
        "补录已结束行为；起止时间用 ISO 8601。冲突时返回 JSON 含 conflicts 数组，由 LLM 转述给用户选择"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "activityId",
            description = "活动 id",
            type = ParameterType.NUMBER,
            required = true,
            constraints = ParameterConstraint(minValue = 1),
        ),
        ToolParameter(
            name = "startTime",
            description = "开始时间 ISO 8601（如 2026-05-17T10:00:00+08:00；无时区时按本机时区）",
            type = ParameterType.STRING,
            required = true,
        ),
        ToolParameter(
            name = "endTime",
            description = "结束时间 ISO 8601",
            type = ParameterType.STRING,
            required = true,
        ),
        ToolParameter(
            name = "tagIds",
            description = "标签 id 列表（可空）",
            type = ParameterType.ARRAY,
            required = false,
        ),
        ToolParameter(
            name = "note",
            description = "备注，最长 500 字",
            type = ParameterType.STRING,
            required = false,
            constraints = ParameterConstraint(maxLength = MAX_NOTE_LENGTH),
        ),
    )

    override val returnType: KClass<*> = Long::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val activityId = (args["activityId"] as? Number)?.toLong()
            ?: return ToolResult.Error(name, ToolError.ValidationError("activityId 必填"))
        val startStr = args["startTime"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("startTime 必填 (ISO 8601)"))
        val endStr = args["endTime"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("endTime 必填 (ISO 8601)"))
        val tagIds = parseLongList(args["tagIds"])
        val note = (args["note"] as? String)?.takeIf { it.isNotBlank() }

        val startMs = TimeUtils.parseIsoToMillis(startStr)
            ?: return ToolResult.Error(
                name,
                ToolError.ValidationError("startTime 格式错误: $startStr"),
            )
        val endMs = TimeUtils.parseIsoToMillis(endStr)
            ?: return ToolResult.Error(
                name,
                ToolError.ValidationError("endTime 格式错误: $endStr"),
            )
        if (endMs <= startMs) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("endTime 必须晚于 startTime"),
            )
        }

        return when (val r = addBehaviorUseCase(
            activityId = activityId,
            tagIds = tagIds,
            startTime = startMs,
            endTime = endMs,
            status = BehaviorNature.COMPLETED,
            note = note,
        )) {
            is AddBehaviorUseCase.Result.Success -> ToolResult.Success(name, r.behaviorId)
            is AddBehaviorUseCase.Result.ValidationError ->
                ToolResult.Error(name, ToolError.ValidationError(r.message))
            is AddBehaviorUseCase.Result.Conflict ->
                conflictResult(startMs, endMs)
        }
    }

    private suspend fun conflictResult(start: Long, end: Long): ToolResult {
        val overlapping = behaviorRepository.getBehaviorsOverlappingRange(start, end).first()
            .filter { it.status != BehaviorNature.PENDING && it.startTime > 0L }
        // 先把 suspend 查询全做完，再拼 JSON
        val items = overlapping.map { b ->
            val activityName = activityRepository.getById(b.activityId)?.name ?: "(未知活动)"
            val endStr = b.endTime?.let { TimeUtils.formatIso(it) } ?: "(未结束)"
            ConflictItem(
                id = b.id,
                activityId = b.activityId,
                activityName = activityName,
                startTimeIso = TimeUtils.formatIso(b.startTime),
                endTimeIso = endStr,
            )
        }
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("activityId", item.activityId)
            obj.put("activityName", item.activityName)
            obj.put("startTime", item.startTimeIso)
            obj.put("endTime", item.endTimeIso)
            arr.put(obj)
        }
        val payload = JSONObject()
        payload.put("code", "CONFLICT")
        payload.put("conflicts", arr)
        return ToolResult.Error(
            name = name,
            error = ToolError.ValidationError(payload.toString()),
        )
    }

    private data class ConflictItem(
        val id: Long,
        val activityId: Long,
        val activityName: String,
        val startTimeIso: String,
        val endTimeIso: String,
    )

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
        returnExample = "42  // 新 Behavior 的 id",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "{\"code\":\"CONFLICT\",\"conflicts\":[...]}",
                scenario = "时间区间与现有行为冲突；LLM 解析 JSON 后向用户复述",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "endTime 必须晚于 startTime",
                scenario = "时间区间非法",
            ),
        ),
        usageExamples = listOf(
            "recordBehavior(activityId=3, startTime=\"2026-05-17T10:00:00+08:00\", endTime=\"2026-05-17T14:00:00+08:00\", tagIds=[7])",
        ),
    )

    private companion object {
        const val MAX_NOTE_LENGTH = 500
    }
}
