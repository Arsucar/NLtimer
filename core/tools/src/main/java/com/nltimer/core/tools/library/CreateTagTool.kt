package com.nltimer.core.tools.library

import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.TagRepository
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

/**
 * 工具：创建标签
 *
 * 默认行为：
 * - 未指定 category → "预制菜"
 * - 未指定 iconKey → "#"
 * - 未指定 color → 工具内随机莫奈中和色
 */
@Singleton
class CreateTagTool @Inject constructor(
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "createTag"
    override val description: String =
        "创建标签；可指定分类名（默认'预制菜'）、图标 key（默认'#'）、颜色（缺省自动生成莫奈色）"
    override val category: ToolCategory = ToolCategory.ACTIVITIES
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "name",
            description = "标签名（不能为空，最长 50 字）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = MAX_NAME_LENGTH),
        ),
        ToolParameter(
            name = "category",
            description = "标签分类名，缺省 '预制菜'",
            type = ParameterType.STRING,
            required = false,
            default = DEFAULT_CATEGORY,
        ),
        ToolParameter(
            name = "iconKey",
            description = "图标 key：缺省 '#'；可传 emoji 字符或 'mi:filled:Tag' 等前缀",
            type = ParameterType.STRING,
            required = false,
            default = DEFAULT_ICON_KEY,
        ),
        ToolParameter(
            name = "color",
            description = "ARGB 颜色（Long）；缺省自动生成莫奈中和色",
            type = ParameterType.NUMBER,
            required = false,
        ),
    )

    override val returnType: KClass<*> = Long::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val name = (args["name"] as? String)?.trim().orEmpty()
        if (name.isEmpty()) {
            return ToolResult.Error(
                this.name,
                ToolError.ValidationError("name 不能为空"),
            )
        }
        val category = (args["category"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_CATEGORY
        val iconKey = (args["iconKey"] as? String)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ICON_KEY
        val color = (args["color"] as? Number)?.toLong() ?: RandomMonetColor.next()

        return runCatching {
            if (tagRepository.getByName(name) != null) {
                return@runCatching ToolResult.Error(
                    this.name,
                    ToolError.ValidationError("标签已存在: $name"),
                )
            }
            val newId = tagRepository.insert(
                Tag(
                    id = 0L,
                    name = name,
                    color = color,
                    iconKey = iconKey,
                    category = category,
                    priority = 0,
                    usageCount = 0,
                    sortOrder = 0,
                    keywords = null,
                    isArchived = false,
                ),
            )
            ToolResult.Success(this.name, newId)
        }.getOrElse { e ->
            ToolResult.Error(
                this.name,
                ToolError.InternalError(e.message ?: "创建标签失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "17  // 新 Tag 的 id",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "标签已存在: 吃饭",
                scenario = "重名创建",
            ),
        ),
        usageExamples = listOf(
            "createTag(name=\"吃饭\")",
            "createTag(name=\"睡觉\", category=\"生活\", iconKey=\"😴\")",
        ),
    )

    private companion object {
        const val MAX_NAME_LENGTH = 50
        const val DEFAULT_CATEGORY = "预制菜"
        const val DEFAULT_ICON_KEY = "#"
    }
}
