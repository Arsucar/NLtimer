package com.nltimer.app.experimental.ai_inter.chat.markdown

private val INLINE_LATEX_REGEX = Regex("\\\\\\((.+?)\\\\\\)")
private val BLOCK_LATEX_REGEX = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)
private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```|`[^`\n]*`", RegexOption.DOT_MATCHES_ALL)

internal fun preProcess(content: String): String {
    val codeBlocks = mutableListOf<IntRange>()
    CODE_BLOCK_REGEX.findAll(content).forEach { codeBlocks.add(it.range) }
    fun isInCodeBlock(pos: Int) = codeBlocks.any { pos in it }

    var result = INLINE_LATEX_REGEX.replace(content) { m ->
        if (isInCodeBlock(m.range.first)) m.value else "$" + m.groupValues[1] + "$"
    }
    result = BLOCK_LATEX_REGEX.replace(result) { m ->
        if (isInCodeBlock(m.range.first)) m.value else "$$" + m.groupValues[1] + "$$"
    }
    return result
}
