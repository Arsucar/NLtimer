package com.nltimer.core.tools.library

import com.nltimer.core.data.model.ExportData
import com.nltimer.core.data.repository.DataExportImportRepository
import com.nltimer.core.data.repository.ImportMode
import com.nltimer.core.data.repository.ImportResult
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
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Singleton
class ImportDataTool @Inject constructor(
    private val dataExportImportRepository: DataExportImportRepository,
) : ToolDefinition {

    override val name: String = "importData"
    override val description: String = "导入 JSON 格式数据，支持 SMART（合并）和 OVERWRITE（覆盖）两种模式"
    override val category: ToolCategory = ToolCategory.ACTIVITIES
    override val accessLevel: AccessLevel = AccessLevel.WRITE

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "data",
            description = "JSON 字符串，格式同 exportData 输出",
            type = ParameterType.STRING,
            required = true,
        ),
        ToolParameter(
            name = "mode",
            description = "导入模式：SMART（合并，跳过已存在）或 OVERWRITE（清空后覆盖）",
            type = ParameterType.STRING,
            required = true,
            constraints = ParameterConstraint(enum = listOf("SMART", "OVERWRITE")),
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val dataStr = args["data"] as? String
        if (dataStr.isNullOrBlank()) {
            return ToolResult.Error(name, ToolError.ValidationError("data 必填"))
        }
        val modeStr = args["mode"] as? String
        if (modeStr == null || modeStr !in listOf("SMART", "OVERWRITE")) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("mode 必须为 SMART 或 OVERWRITE"),
            )
        }

        return runCatching {
            val json = Json { ignoreUnknownKeys = true }
            val exportData = json.decodeFromString<ExportData>(dataStr)
            val mode = ImportMode.valueOf(modeStr)
            val importResult = dataExportImportRepository.importAll(exportData, mode)

            when (importResult) {
                is ImportResult.Success -> {
                    val result = JSONObject()
                        .put("activityGroupsImported", importResult.activityGroupsImported)
                        .put("activitiesImported", importResult.activitiesImported)
                        .put("tagsImported", importResult.tagsImported)
                        .put("tagCategoriesImported", importResult.tagCategoriesImported)
                    ToolResult.Success(name, result.toString())
                }
                is ImportResult.Error -> {
                    ToolResult.Error(name, ToolError.InternalError(importResult.message))
                }
            }
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "导入失败"))
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
              "activityGroupsImported": 3,
              "activitiesImported": 10,
              "tagsImported": 5,
              "tagCategoriesImported": 2
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "data 必填",
                scenario = "未传入数据",
            ),
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "mode 必须为 SMART 或 OVERWRITE",
                scenario = "传入无效的 mode 值",
            ),
            ErrorExample(
                code = "INTERNAL_ERROR",
                message = "Unexpected JSON token at offset 0",
                scenario = "JSON 格式错误",
            ),
        ),
        usageExamples = listOf(
            """importData(data='{"activities":[...],"tags":[...]}', mode="SMART")""",
            """importData(data=exportedJson, mode="OVERWRITE")""",
        ),
    )
}
