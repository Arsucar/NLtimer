package com.nltimer.core.tools.timing

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 时间解析与格式化共享工具
 *
 * 供 timing 包下各 ToolDefinition 实现类共用，
 * 避免每个工具重复维护 ISO 8601 解析 / 格式化逻辑。
 */
object TimeUtils {

    /** 将 ISO 8601 字符串解析为 epoch 毫秒；支持带时区和不带时区两种格式 */
    fun parseIsoToMillis(s: String): Long? {
        return try {
            OffsetDateTime.parse(s).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(s)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /** 将 epoch 毫秒格式化为 ISO 8601 带时区字符串 */
    fun formatIso(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /** 获取今天 00:00:00 本地时区的 epoch 毫秒 */
    fun todayStartMillis(): Long {
        val today = LocalDate.now()
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** 获取今天 23:59:59 本地时区的 epoch 毫秒 */
    fun todayEndMillis(): Long {
        val today = LocalDate.now()
        return today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** 将 YYYY-MM-DD 解析为当天 00:00:00 的 epoch 毫秒 */
    fun dateStartMillis(dateStr: String): Long? {
        return try {
            LocalDate.parse(dateStr)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** 将 YYYY-MM-DD 解析为当天 23:59:59 的 epoch 毫秒 */
    fun dateEndMillis(dateStr: String): Long? {
        return try {
            LocalDate.parse(dateStr)
                .atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
