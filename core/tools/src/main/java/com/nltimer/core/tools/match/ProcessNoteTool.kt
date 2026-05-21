package com.nltimer.core.tools.match

import com.nltimer.core.data.repository.ActivityRepository
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
import kotlinx.coroutines.flow.first

@Singleton
class ProcessNoteTool @Inject constructor(
    private val noteMatcher: NoteMatcher,
    private val applyNoteDirectivesUseCase: ApplyNoteDirectivesUseCase,
    private val activityRepository: ActivityRepository,
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name: String = "processNote"
    override val description: String =
        "解析用户备注文本，自动匹配或创建活动和标签。支持 @活动名、#标签名 指令和关键词匹配。"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.WRITE
    override val returnType: KClass<*> = Map::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "note",
            description = "用户输入的备注文本，支持 @活动名 和 #标签名 指令",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = 500),
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val note = args["note"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("note 必须是字符串"))

        val activities = activityRepository.getAllActive().first()
        val tags = tagRepository.getAllActive().first()

        val parsed = NoteDirectiveParser.parse(note)
        val directive = applyNoteDirectivesUseCase(parsed.directives, activities, tags)
        val scan = noteMatcher.scan(parsed.cleanedNote, activities, tags)

        val finalActivityId = directive.lastActivityId ?: scan.activityId
        val finalTagIds = (directive.addedTagIds + scan.tagIds).toList()

        return ToolResult.Success(
            name = name,
            data = mapOf(
                "activityId" to finalActivityId,
                "tagIds" to finalTagIds,
                "cleanedNote" to parsed.cleanedNote,
                "createdActivities" to directive.createdActivityNames,
                "createdTags" to directive.createdTagNames,
                "matchedActivities" to directive.matchedActivityNames,
                "matchedTags" to directive.matchedTagNames,
            ),
        )
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "activityId": 1,
              "tagIds": [2, 3],
              "cleanedNote": "开会",
              "createdActivities": [],
              "createdTags": [],
              "matchedActivities": ["工作"],
              "matchedTags": ["会议"]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "note 必须是字符串",
                scenario = "note 参数缺失或非字符串",
            ),
        ),
        usageExamples = listOf(
            """processNote(note="@工作 开会 #会议")""",
            """processNote(note="下午在写代码")""",
        ),
    )
}
