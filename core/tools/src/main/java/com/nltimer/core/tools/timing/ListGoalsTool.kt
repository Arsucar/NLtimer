package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.BehaviorNature
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
class ListGoalsTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "listGoals"
    override val description: String = "列出所有 PENDING 目标（按 sequence 排序），含活动名、预估时长、标签"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val pending = behaviorRepository.getPendingBehaviors().first()
                .sortedBy { it.sequence }
            val arr = JSONArray()
            for (b in pending) {
                val details = behaviorRepository.getBehaviorWithDetails(b.id)
                val a = details?.activity
                val tags = details?.tags ?: emptyList()
                val tagsArr = JSONArray()
                tags.forEach { t -> tagsArr.put(JSONObject().apply { put("id", t.id); put("name", t.name) }) }
                val estimatedMinutes = b.estimatedDuration?.let { (it / 60_000).toInt() }
                arr.put(JSONObject().apply {
                    put("id", b.id)
                    put("activityId", b.activityId)
                    put("activityName", a?.name ?: JSONObject.NULL)
                    put("iconKey", a?.iconKey ?: JSONObject.NULL)
                    put("sequence", b.sequence)
                    put("estimatedDurationMinutes", estimatedMinutes ?: JSONObject.NULL)
                    put("note", b.note ?: JSONObject.NULL)
                    put("tags", tagsArr)
                })
            }
            ToolResult.Success(name, arr.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "获取目标列表失败"))
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
              {"id":73,"activityId":4,"activityName":"主动学习","iconKey":"📖","sequence":1,"estimatedDurationMinutes":30,"note":"复习笔记","tags":[{"id":5,"name":"重点"}]},
              {"id":74,"activityId":7,"activityName":"本职工作","iconKey":"💼","sequence":2,"estimatedDurationMinutes":120,"note":null,"tags":[]}
            ]
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("INTERNAL_ERROR", "获取目标列表失败", "数据库读取异常"),
        ),
        usageExamples = listOf("listGoals()"),
    )
}
