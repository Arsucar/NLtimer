package com.nltimer.core.data.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** HH:mm 格式的时间格式化器，供各视图共享使用。 */
val hhmmFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 将毫秒格式化为"X时X分X秒"的可读字符串。
 */
fun formatDuration(ms: Long): String {
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    val seconds = (ms % 60000) / 1000
    return buildString {
        if (hours > 0) append("${hours}时")
        if (minutes > 0 || hours > 0) append("${minutes}分")
        if (hours == 0L) append("${seconds}秒")
    }
}

/**
 * 将分钟数格式化为"X小时X分钟"的可读字符串。
 */
fun formatDurationMinutes(minutes: Long): String {
    if (minutes == 0L) return "0 分钟"
    val hours = minutes / 60
    val mins = minutes % 60
    return buildString {
        if (hours > 0) append("${hours} 小时 ")
        if (mins > 0 || hours == 0L) append("${mins} 分钟")
    }.trim()
}

val hhmmssFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
val mmddFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")
val yyyyMMddHHmmFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
val exportTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

fun Long.epochToLocalTime(): LocalTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime()

fun Long.epochToLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.epochToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalTime.toEpochMillisToday(): Long =
    LocalDate.now().atTime(this).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillis(): Long =
    plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.atTimeToEpochMillis(time: LocalTime): Long =
    atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatDurationCompact(ms: Long): String {
    val totalMinutes = ms / 60000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}.${m}h" else "${m}m"
}

fun formatDurationCompactHm(ms: Long): String {
    val totalMinutes = ms / 60000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

fun formatEpochTimeRange(startMs: Long, endMs: Long?): String {
    val start = startMs.epochToLocalTime().format(hhmmFormatter)
    val end = endMs?.let { it.epochToLocalTime().format(hhmmFormatter) }
    return if (end != null) "$start - $end" else start
}

fun formatGridDurationHours(ms: Long): String {
    if (ms <= 0L) return ""
    val totalMinutes = (ms / 60_000).coerceAtLeast(1)
    if (totalMinutes < 60) return "${totalMinutes}m"
    val tenths = ((ms * 10) / 3_600_000).coerceAtLeast(1)
    val hours = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0L) {
        "${hours}h"
    } else {
        "${hours}.${fraction}h"
    }
}

fun formatTimestamp(timestamp: Long): String =
    yyyyMMddHHmmFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())

fun formatExportTimestamp(): String =
    exportTimestampFormatter.format(LocalDateTime.now())
