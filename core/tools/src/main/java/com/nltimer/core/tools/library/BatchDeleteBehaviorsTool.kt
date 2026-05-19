package com.nltimer.core.tools.library

import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
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
class BatchDeleteBehaviorsTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val behaviorDao: BehaviorDao,
) : ToolDefinition {

    override val name: String = "batchDeleteBehaviors"
    override val description: String = "批量删除行为记录，同时清理标签关联，返回已删除和未找到的 ID 列表"
    override val category: ToolCategory = ToolCategory.ACTIVITIES
    override val accessLevel: AccessLevel = AccessLevel.FULL

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "ids",
            description = "要删除的行为 ID 列表，最多 $MAX_BATCH_SIZE 条",
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
        if (rawIds.size > MAX_BATCH_SIZE) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("单次最多 $MAX_BATCH_SIZE 条，当前 ${rawIds.size} 条"),
            )
        }

        val deleted = JSONArray()
        val notFound = JSONArray()

        for (rawId in rawIds) {
            val id = (rawId as? Number)?.toLong()
            if (id == null) {
                notFound.put(rawId)
                continue
            }
            val entity = behaviorDao.getById(id)
            if (entity == null) {
                notFound.put(id)
                continue
            }
            runCatching {
                behaviorDao.deleteTagsForBehavior(id)
                behaviorRepository.delete(id)
            }.onSuccess {
                deleted.put(id)
            }.onFailure {
                notFound.put(id)
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
              "notFound": [99]
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
            """batchDeleteBehaviors(ids=[1, 2, 3])""",
            """batchDeleteBehaviors(ids=[42, 99])""",
        ),
    )

    private companion object {
        const val MAX_BATCH_SIZE = 20
    }
}
