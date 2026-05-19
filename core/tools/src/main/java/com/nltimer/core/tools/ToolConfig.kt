package com.nltimer.core.tools

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 工具运行时配置，由 app 层写入、core/tools 层消费。
 *
 * 目前仅包含 maxBatchSize，后续可扩展更多可配置参数。
 */
@Singleton
class ToolConfig @Inject constructor() {
    /** 批量工具单次最大条数，范围 1–200，默认 20 */
    @Volatile
    var maxBatchSize: Int = 20
}
