package com.nltimer.feature.ai.navigation

object AiRoutes {
    const val AI_INTER = "ai_inter"
    const val AI_PROVIDER_CONFIG = "ai_provider_config"
    const val AI_TOOLS_LIST = "ai_tools_list"
    const val AI_CALL_LOGS = "ai_call_logs"
    const val AI_PROMPT_CONFIG = "ai_prompt_config"
    const val AI_TEST_CHAT = "ai_test_chat"
    const val AI_ASSISTANT_CHAT = "ai_assistant_chat"
    const val AI_CALL_LOG_DETAIL = "ai_call_log_detail"
    const val AI_CALL_LOG_DETAIL_PATTERN = "ai_call_log_detail/{logId}"

    fun aiCallLogDetail(logId: Long) = "ai_call_log_detail/$logId"
}
