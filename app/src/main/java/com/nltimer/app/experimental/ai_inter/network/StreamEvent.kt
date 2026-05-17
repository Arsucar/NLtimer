package com.nltimer.app.experimental.ai_inter.network

/**
 * SSE 流事件，把 OpenAI 流式响应里"普通 token / reasoning / 工具调用"三类信息分开。
 */
sealed class StreamEvent {
    /** 模型给用户看的正文 token */
    data class Content(val text: String) : StreamEvent()

    /** 模型推理过程（reasoning_content / reasoning），UI 中默认折叠 */
    data class Reasoning(val text: String) : StreamEvent()

    /**
     * 工具调用的增量片段。OpenAI 协议会按 index 分多段下发：
     * - 第一段携带 id、function.name、可能携带 arguments 起始片段；
     * - 后续段只携带 arguments 的后续片段。
     * 收方按 index 拼接 [argumentsChunk]，得到最终 JSON 参数串。
     */
    data class ToolCallDelta(
        val index: Int,
        val id: String?,
        val name: String?,
        val argumentsChunk: String,
    ) : StreamEvent()
}
