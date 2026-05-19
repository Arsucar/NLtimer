package com.nltimer.core.data.repository

import com.nltimer.core.data.model.ExportData

enum class ImportMode {
    SMART,
    OVERWRITE,
}

sealed class ImportResult {
    data class Success(
        val activityGroupsImported: Int = 0,
        val activitiesImported: Int = 0,
        val tagsImported: Int = 0,
        val tagCategoriesImported: Int = 0,
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}

interface DataExportImportRepository {
    suspend fun exportAll(): ExportData
    suspend fun exportActivities(): ExportData
    suspend fun exportTags(): ExportData
    suspend fun exportCategories(): ExportData

    suspend fun importAll(data: ExportData, mode: ImportMode): ImportResult
    suspend fun importActivities(data: ExportData, mode: ImportMode): ImportResult
    suspend fun importTags(data: ExportData, mode: ImportMode): ImportResult
    suspend fun importCategories(data: ExportData, mode: ImportMode): ImportResult

    suspend fun exportByDateRange(startMs: Long, endMs: Long): ExportData
}
