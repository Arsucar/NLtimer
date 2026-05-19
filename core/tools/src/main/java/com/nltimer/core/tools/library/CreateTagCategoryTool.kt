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
 * 工具：声明标签分类（占位语义）
 *
 * Tag.category 是字符串字段，没有独立 TagCategory 表。
 * 该工具只做查重 — 待首个 tag 写入该 category 后，从 distinct 自然出现。
 *
 * 返回：
 * - 名字未存在 → "已声明标签分类: X（待首个标签写入后生效）"
 * - 名字已存在 → "标签分类已存在: X"（仍按 Success 返回，避免 LLM 误判）
 */
@Singleton
class CreateTagCategoryTool @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ToolDefinition {

    override val name: String = "createTagCategory"
    override val description: String = "声明标签分类名（占位语义，未写物理表；待首个标签写入后通过 distinct 体现）"
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
            val declared = categoryRepository.addTagCategoryStub(name)
            val msg = if (declared) {
                "已声明标签分类: $name（待首个标签写入后生效）"
            } else {
                "标签分类已存在: $name"
            }
            ToolResult.Success(this.name, msg)
        }.getOrElse { e ->
            ToolResult.Error(
                this.name,
                ToolError.InternalError(e.message ?: "声明标签分类失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "\"已声明标签分类: 学习（待首个标签写入后生效）\"",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "name 不能为空",
                scenario = "传入空字符串",
            ),
        ),
        usageExamples = listOf(
            "createTagCategory(name=\"学习\")",
        ),
    )

    private companion object {
        const val MAX_NAME_LENGTH = 50
    }
}
