package com.nltimer.core.tools.library

import com.nltimer.core.data.repository.DataExportImportRepository
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ExportDataTool @Inject constructor(
    private val dataExportImportRepository: DataExportImportRepository,
) : ToolDefinition {

    override val name: String = "exportData"
    override val description: String = "按日期范围导出行为、活动、标签数据为 JSON 格式"
    override val category: ToolCategory = ToolCategory.DATA
    override val accessLevel: AccessLevel = AccessLevel.READ

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "startDate",
            description = "开始时间（epoch 毫秒）",
            type = ParameterType.DATE_TIME,
            required = true,
        ),
        ToolParameter(
            name = "endDate",
            description = "结束时间（epoch 毫秒）",
            type = ParameterType.DATE_TIME,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val startMs = (args["startDate"] as? Number)?.toLong()
        if (startMs == null) {
            return ToolResult.Error(name, ToolError.ValidationError("startDate 必填"))
        }
        val endMs = (args["endDate"] as? Number)?.toLong()
        if (endMs == null) {
            return ToolResult.Error(name, ToolError.ValidationError("endDate 必填"))
        }
        if (startMs >= endMs) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("startDate 必须小于 endDate"),
            )
        }

        return runCatching {
            val exportData = dataExportImportRepository.exportByDateRange(startMs, endMs)
            val json = Json { prettyPrint = true; encodeDefaults = false }
            ToolResult.Success(name, json.encodeToString(exportData))
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "导出失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "version": 1,
              "exportedAt": 1716000000000,
              "activities": [...],
              "activityGroups": [...],
              "tags": [...],
              "tagCategories": [...]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "startDate 必填",
                scenario = "未传入 startDate",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "startDate 必须小于 endDate",
                scenario = "开始时间不小于结束时间",
            ),
        ),
        usageExamples = listOf(
            """exportData(startDate=1716163200000, endDate=1716249600000)""",
        ),
    )
}
