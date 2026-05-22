package com.nltimer.core.tools.match

/**
 * 备注主动指令解析器：在备注里识别 `@name` / `#name` 形式的“主动声明”。
 *
 * 与同 package 下 [NoteMatcher]（反向扫描“备注包含哪些已存在的活动/标签”）的关系：
 * - 本类做“用户主动声明” → 上游 UseCase 决定是否创建并选中
 * - 两者由 ViewModel 编排串联，互不干扰
 *
 * 设计依据：`docs/superpowers/specs/2026-05-14-note-directive-design.md`
 */
object NoteDirectiveParser {

    private const val MAX_NAME_LENGTH = 32

    data class Directive(
        val symbol: Char,
        val name: String,
        val range: IntRange,
    )

    data class ParseResult(
        val directives: List<Directive>,
        val cleanedNote: String,
    )

    fun parse(note: String): ParseResult {
        if (note.isEmpty()) return ParseResult(emptyList(), note)
        val directives = mutableListOf<Directive>()
        val cleaned = StringBuilder(note.length)
        var i = 0
        while (i < note.length) {
            val ch = note[i]
            if (ch == '@' || ch == '#') {
                val escaped = i > 0 && (note[i - 1] == '!' || note[i - 1] == '！')
                val nameEnd = scanNameEnd(note, i + 1)
                val rawName = note.substring(i + 1, nameEnd)
                if (escaped) {
                    cleaned.append(ch)
                    cleaned.append(rawName)
                    i = nameEnd
                    continue
                }
                if (rawName.isEmpty()) {
                    cleaned.append(ch)
                    i++
                    continue
                }
                val name = rawName.take(MAX_NAME_LENGTH)
                directives += Directive(ch, name, i..(nameEnd - 1))
                cleaned.append(rawName)
                i = nameEnd
            } else {
                cleaned.append(ch)
                i++
            }
        }
        return ParseResult(directives, cleaned.toString())
    }

    /** 从 [start] 开始向后扫描，返回 name 结束位置（开区间右端） */
    private fun scanNameEnd(note: String, start: Int): Int {
        var j = start
        while (j < note.length) {
            val c = note[j]
            if (isBoundary(c)) break
            j++
        }
        return j
    }

    private fun isBoundary(c: Char): Boolean {
        return c.isWhitespace() || c == '@' || c == '#' ||
            c in ",.!?:;'\"()[]{}<>/\\|" ||
            c in "，。、！？：；\u201C\u201D\u2018\u2019「」《》（）【】〈〉"
    }
}
