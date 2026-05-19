package com.nltimer.core.tools.library

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.repository.ActivityRepository
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
import kotlinx.coroutines.flow.first

/**
 * 工具：创建活动
 *
 * 默认行为：
 * - 未指定 groupName → 落到 "预制菜" 分组（不存在则自动创建）
 * - 未指定 iconKey → 留 null，由 UI 渲染兜底
 * - 未指定 color → 工具内随机莫奈中和色
 */
@Singleton
class CreateActivityTool @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val categoryRepository: CategoryRepository,
) : ToolDefinition {

    override val name: String = "createActivity"
    override val description: String =
        "创建活动；可指定分组名（默认'预制菜'）、图标 key（emoji 或 mi:style:name 前缀）、颜色（缺省自动生成莫奈色）"
    override val category: ToolCategory = ToolCategory.ACTIVITY
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "name",
            description = "活动名（不能为空，最长 50 字）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = MAX_NAME_LENGTH),
        ),
        ToolParameter(
            name = "groupName",
            description = "活动分组名，缺省 '预制菜'，不存在则自动创建",
            type = ParameterType.STRING,
            required = false,
            default = DEFAULT_GROUP_NAME,
        ),
        ToolParameter(
            name = "iconKey",
            description = "图标 key：emoji 字符直接传（如 😮‍💨），或 Material 图标用 'mi:filled:Code' 等前缀",
            type = ParameterType.STRING,
            required = false,
        ),
        ToolParameter(
            name = "color",
            description = "ARGB 颜色（Long，例如 0xFF6750A4=4284513444）；缺省自动生成莫奈中和色",
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
        val groupName = (args["groupName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_GROUP_NAME
        val iconKey = (args["iconKey"] as? String)?.takeIf { it.isNotBlank() }
        val color = (args["color"] as? Number)?.toLong() ?: RandomMonetColor.next()

        return runCatching {
            if (activityRepository.getByName(name) != null) {
                return@runCatching ToolResult.Error(
                    this.name,
                    ToolError.ValidationError("活动已存在: $name"),
                )
            }

            val groupId = ensureActivityGroupId(groupName)
            val newId = activityRepository.insert(
                Activity(
                    id = 0L,
                    name = name,
                    iconKey = iconKey,
                    keywords = null,
                    groupId = groupId,
                    isPreset = false,
                    isArchived = false,
                    color = color,
                    usageCount = 0,
                ),
            )
            ToolResult.Success(this.name, newId)
        }.getOrElse { e ->
            ToolResult.Error(
                this.name,
                ToolError.InternalError(e.message ?: "创建活动失败"),
            )
        }
    }

    private suspend fun ensureActivityGroupId(groupName: String): Long {
        val groups = activityRepository.getAllGroups().first()
        groups.firstOrNull { it.name == groupName }?.let { return it.id }
        categoryRepository.addActivityCategory(groupName)
        return activityRepository.getAllGroups().first()
            .firstOrNull { it.name == groupName }?.id
            ?: error("创建分组后仍未找到: $groupName")
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = "42  // 新 Activity 的 id",
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "活动已存在: 阅读",
                scenario = "重名创建",
            ),
        ),
        usageExamples = listOf(
            "createActivity(name=\"看小说\")",
            "createActivity(name=\"追番\", groupName=\"娱乐\", iconKey=\"📺\")",
        ),
    )

    private companion object {
        const val MAX_NAME_LENGTH = 50
        const val DEFAULT_GROUP_NAME = "预制菜"
    }
}
