package com.nltimer.core.data.util

import com.nltimer.core.data.model.BehaviorEventValue

/**
 * 事件字段值展示格式化（R4/R5 各查看端共用）
 * EAV 行 → 人类可读文本；数值去小数点展示整数；文本去空白
 */
private fun trimNumberText(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/** 单个字段值的展示文本；无值（文本空 + 数值空）返回 null */
fun BehaviorEventValue.displayText(): String? {
    val text = valueText?.ifBlank { null }
    val number = valueNumber
    return when {
        text != null -> text
        number != null -> trimNumberText(number)
        else -> null
    }
}

/** 字段值列表摘要：截取前 [limit] 个原始值行再提取有效文本，以 " · " 连接（空列表/全空返回 ""） */
fun List<BehaviorEventValue>.summarizeEventValues(limit: Int = 3): String =
    take(limit).mapNotNull { it.displayText() }.joinToString(" · ")
