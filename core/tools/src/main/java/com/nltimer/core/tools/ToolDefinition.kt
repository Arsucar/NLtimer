package com.nltimer.core.tools

import kotlin.reflect.KClass

/**
 * 工具基础接口 —— 所有业务工具均实现此接口
 *
 * 设计目标：
 * - UI 层、业务层、AI Agent 均通过统一入口调用同一套工具
 * - 工具元信息（参数 / 约束 / 权限 / 文档）随实现一起声明，便于后续生成 LLM Function Calling Schema
 */
interface ToolDefinition {

    /** 工具唯一标识符（建议英文小驼峰），如 "queryActiveSession" */
    val name: String

    /** 工具中文描述，给开发者与 AI Agent 阅读 */
    val description: String

    /** 工具业务分类 */
    val category: ToolCategory

    /** 工具需要的访问权限级别 */
    val accessLevel: AccessLevel

    /** 参数声明列表 */
    val parameters: List<ToolParameter>

    /** 返回值类型，用于反射或文档生成 */
    val returnType: KClass<*>

    /**
     * 执行工具
     *
     * @param args 参数集合（参数名 -> 参数值），未传的可选参数无需出现在 map 中
     * @return 执行结果（统一封装为 [ToolResult]，不抛异常）
     */
    suspend fun execute(args: Map<String, Any?>): ToolResult

    /** 获取完整文档（用于 AI Agent 描述工具能力，或开发者查阅用法） */
    fun getDocumentation(): ToolDocumentation
}

/** 工具参数定义 */
data class ToolParameter(
    val name: String,
    val description: String,
    val type: ParameterType,
    val required: Boolean = false,
    val default: Any? = null,
    val constraints: ParameterConstraint? = null,
)

/** 参数约束条件，所有字段均可选；ToolRegistry 会按字段类型自动校验对应约束 */
data class ParameterConstraint(
    /** 字符串最小长度（含） */
    val minLength: Int? = null,
    /** 字符串最大长度（含） */
    val maxLength: Int? = null,
    /** 正则表达式（针对字符串） */
    val pattern: String? = null,
    /** 数值最小值（含） */
    val minValue: Number? = null,
    /** 数值最大值（含） */
    val maxValue: Number? = null,
    /** 枚举可选值（针对字符串） */
    val enum: List<String>? = null,
)

/** 参数原始类型 */
enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    ARRAY,
    OBJECT,
    DATE_TIME,
    DURATION,
}

/** 工具业务分类 */
enum class ToolCategory {
    /** 计时控制：开始/结束/补录/查询正在进行的会话 */
    TIMING,

    /** 行为管理：删除行为、标签关联 */
    BEHAVIOR,

    /** 活动管理：创建/删除/更新活动 */
    ACTIVITY,

    /** 标签管理：创建/列出标签 */
    TAG,

    /** 分类管理：活动分类、标签分类 */
    CATEGORY,

    /** 数据管理：导入/导出 */
    DATA,

    /** 搜索匹配：活动与标签搜索 */
    SEARCH,

    /** 统计数据 */
    STATISTICS,
}

/**
 * 访问权限等级
 *
 * 数值越大权限越大；[ToolRegistry.getAvailableTools] 通过 ordinal 比较过滤可调用工具
 */
enum class AccessLevel {
    /** 不访问任何本地数据，纯计算 */
    NONE,

    /** 仅读取 */
    READ,

    /** 可读可写 */
    WRITE,

    /** 全权限（含敏感操作，例如删除） */
    FULL,
}

/** 工具文档 */
data class ToolDocumentation(
    val name: String,
    val description: String,
    val category: ToolCategory,
    val accessLevel: AccessLevel,
    val parameters: List<ToolParameter>,
    /** JSON 格式返回示例 */
    val returnExample: String,
    /** 错误示例集合 */
    val errorExamples: List<ErrorExample>,
    /** 调用示例（伪代码） */
    val usageExamples: List<String>,
)

/** 错误示例 */
data class ErrorExample(
    val code: String,
    val message: String,
    val scenario: String,
)
