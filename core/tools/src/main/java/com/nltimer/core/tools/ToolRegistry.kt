package com.nltimer.core.tools

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 工具注册表 —— 全局单例，统一管理所有 [ToolDefinition]
 *
 * 设计要点：
 * - 通过 Hilt Multibinding 收集所有 `@IntoSet` 注册的工具，构造时一次性注入
 * - 同时支持运行时动态 [register] / [registerAll]，满足热插拔场景
 * - [executeTool] 提供超时保护、参数校验、错误归一化（不外抛任何异常）
 */
@Singleton
class ToolRegistry @Inject constructor(
    initialTools: @JvmSuppressWildcards Set<ToolDefinition>,
) {

    private val tools: MutableMap<String, ToolDefinition> = ConcurrentHashMap()

    init {
        initialTools.forEach(::register)
    }

    /** 注册单个工具，重名时覆盖并打印 warning */
    fun register(tool: ToolDefinition) {
        if (tools.containsKey(tool.name)) {
            Log.w(TAG, "Tool ${tool.name} is already registered, overwriting...")
        }
        tools[tool.name] = tool
        Log.i(TAG, "Tool registered: ${tool.name}")
    }

    /** 批量注册 */
    fun registerAll(toolList: Collection<ToolDefinition>) {
        toolList.forEach(::register)
    }

    /** 获取指定工具，未注册返回 null */
    fun getTool(name: String): ToolDefinition? = tools[name]

    /** 获取所有已注册工具的快照 */
    fun getAllTools(): List<ToolDefinition> = tools.values.toList()

    /** 按业务分类筛选 */
    fun getToolsByCategory(category: ToolCategory): List<ToolDefinition> =
        tools.values.filter { it.category == category }

    /**
     * 获取调用方有权限调用的工具列表
     *
     * 规则：工具自身 [ToolDefinition.accessLevel] <= 调用方 [accessLevel] 时可用
     */
    fun getAvailableTools(accessLevel: AccessLevel): List<ToolDefinition> =
        tools.values.filter { it.accessLevel.ordinal <= accessLevel.ordinal }

    /**
     * 执行指定工具
     *
     * @param toolName 工具名
     * @param args 参数集合（参数名 -> 值）
     * @param timeoutMillis 超时时间，默认 [DEFAULT_TIMEOUT_MS]
     * @return 统一封装的 [ToolResult]；任何内部异常均会被归一化为 [ToolResult.Error]
     */
    suspend fun executeTool(
        toolName: String,
        args: Map<String, Any?>,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
    ): ToolResult {
        val tool = getTool(toolName) ?: return ToolResult.Error(
            name = toolName,
            error = ToolError.NotFound("Tool not found: $toolName"),
        )

        return try {
            validateParameters(tool, args)
            withTimeoutOrNull(timeoutMillis) { tool.execute(args) }
                ?: ToolResult.Error(
                    name = toolName,
                    error = ToolError.TimeoutError(
                        "Tool execution timeout after ${timeoutMillis}ms",
                    ),
                )
        } catch (e: IllegalArgumentException) {
            ToolResult.Error(
                name = toolName,
                error = ToolError.ValidationError(e.message ?: "Invalid arguments"),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution failed: $toolName", e)
            ToolResult.Error(
                name = toolName,
                error = ToolError.InternalError(e.message ?: "Unknown error"),
            )
        }
    }

    /**
     * 参数验证
     *
     * 抛 [IllegalArgumentException] 时由 [executeTool] 自动转换为 [ToolError.ValidationError]
     * 暴露为 internal 便于单元测试与子类复用
     */
    internal fun validateParameters(tool: ToolDefinition, args: Map<String, Any?>) {
        for (param in tool.parameters) {
            val present = args.containsKey(param.name) && args[param.name] != null
            if (param.required && !present) {
                throw IllegalArgumentException("Missing required parameter: ${param.name}")
            }
            val value = args[param.name] ?: continue
            val constraints = param.constraints ?: continue
            when (value) {
                is String -> validateStringConstraints(param.name, value, constraints)
                is Number -> validateNumberConstraints(param.name, value, constraints)
                else -> Unit // 其他类型暂不内置校验，工具自行处理
            }
        }
    }

    private fun validateStringConstraints(
        name: String,
        value: String,
        constraints: ParameterConstraint,
    ) {
        constraints.minLength?.let {
            require(value.length >= it) { "$name is too short (min: $it)" }
        }
        constraints.maxLength?.let {
            require(value.length <= it) { "$name is too long (max: $it)" }
        }
        constraints.pattern?.let {
            require(value.matches(Regex(it))) { "$name does not match pattern: $it" }
        }
        constraints.enum?.let {
            require(value in it) { "$name must be one of $it" }
        }
    }

    private fun validateNumberConstraints(
        name: String,
        value: Number,
        constraints: ParameterConstraint,
    ) {
        constraints.minValue?.let {
            require(value.toDouble() >= it.toDouble()) { "$name is too small (min: $it)" }
        }
        constraints.maxValue?.let {
            require(value.toDouble() <= it.toDouble()) { "$name is too large (max: $it)" }
        }
    }

    companion object {
        private const val TAG = "ToolRegistry"

        /** 工具默认执行超时（毫秒） */
        const val DEFAULT_TIMEOUT_MS: Long = 10_000L
    }
}
