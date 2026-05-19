package com.nltimer.core.tools.library

import com.nltimer.core.data.repository.CategoryRepository
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
 * 工具：创建活动分类（ActivityGroup）
 *
 * 直接在 ActivityGroup 表插入一条记录，sortOrder 自动递增。
 */
@Singleton
class CreateActivityCategoryTool @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ToolDefinition {

    override val name: String = "createActivityCategory"
    override val description: String = "创建活动分类（写入 ActivityGroup 表，sortOrder 自增）"
    override val category: ToolCategory = ToolCategory.CATEGORY
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "name",
            description = "分类名（不能为空，最长 50 字）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = MAX_NAME_LENGTH),
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val name = (args["name"] as? String)?.trim().orEmpty()
        if (name.isEmpty()) {
            return ToolResult.Error(
                this.name,
                ToolError.ValidationError("name 不能为空"),
            )
        }
        return runCatching {
            categoryRepository.addActivityCategory(name)
            ToolResult.Success(this.name, "已创建活动分类: $name")
        }.getOrElse { e ->
            ToolResult.Error(
                this.name,
                ToolError.InternalError(e.message ?: "创建活动分类失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "\"已创建活动分类: 运动\"",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "name 不能为空",
                scenario = "传入空字符串",
            ),
        ),
        usageExamples = listOf(
            "createActivityCategory(name=\"运动\")",
        ),
    )

    private companion object {
        const val MAX_NAME_LENGTH = 50
    }
}
