package com.nltimer.core.tools.timing

import com.nltimer.core.data.repository.ActivityRepository
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

/**
 * 工具：列出当前所有未归档（active）的活动
 *
 * 业务场景：
 * - UI 弹出"开始计时"选择器前，先拉一份可选活动列表
 * - AI Agent："我想开始记录某项活动"前，需要知道有哪些可选项
 *
 * 返回 List<Activity>，由调用方按需序列化或绑定到 UI
 */
@Singleton
class ListActivitiesTool @Inject constructor(
    private val activityRepository: ActivityRepository,
) : ToolDefinition {

    override val name: String = "listActivities"
    override val description: String = "列出当前所有未归档的活动，供开始计时时挑选"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val parameters: List<ToolParameter> = emptyList()
    override val returnType: KClass<*> = List::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val list = activityRepository.getAllActive().first()
            ToolResult.Success(name, list)
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
              { "id": 1, "name": "编程", "iconKey": "code", "keywords": "编程,coding", "usageCount": 12, "isArchived": false },
              { "id": 2, "name": "阅读", "iconKey": "book", "keywords": null, "usageCount": 5, "isArchived": false }
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
            "listActivities()  // 无参",
        ),
    )
}
