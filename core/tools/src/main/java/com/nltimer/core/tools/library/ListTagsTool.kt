package com.nltimer.core.tools.library

import com.nltimer.core.data.repository.TagRepository
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
import kotlinx.coroutines.flow.first

/**
 * 工具：列出标签
 *
 * 默认返回未归档标签；当用户显式要求"包含归档"时传 includeArchived=true。
 */
@Singleton
class ListTagsTool @Inject constructor(
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "listTags"
    override val description: String = "列出标签；默认仅未归档，可选包含归档"
    override val category: ToolCategory = ToolCategory.TAG
    override val accessLevel: AccessLevel = AccessLevel.READ

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "includeArchived",
            description = "是否包含已归档标签；默认 false 仅返回未归档",
            type = ParameterType.BOOLEAN,
            required = false,
            default = false,
        ),
    )

    override val returnType: KClass<*> = List::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val includeArchived = (args["includeArchived"] as? Boolean) ?: false
        return runCatching {
            val tags = if (includeArchived) {
                tagRepository.getAll().first()
            } else {
                tagRepository.getAllActive().first()
            }
            ToolResult.Success(name, tags)
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "查询标签列表失败"),
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
              { "id": 1, "name": "专注", "category": "工作", "isArchived": false },
              { "id": 2, "name": "午休", "category": "休息", "isArchived": false }
            ]
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "INTERNAL_ERROR",
                message = "查询标签列表失败",
                scenario = "数据库读取异常",
            ),
        ),
        usageExamples = listOf(
            "listTags()                          // 默认仅未归档",
            "listTags(includeArchived=true)      // 包含归档标签",
        ),
    )
}
