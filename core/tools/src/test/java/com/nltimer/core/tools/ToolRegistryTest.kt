package com.nltimer.core.tools

import com.nltimer.core.tools.event.ToolEventBus
import kotlin.reflect.KClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ToolRegistry] 单元测试
 *
 * 覆盖：
 * - 构造期注入工具
 * - 分类 / 权限筛选
 * - 未知工具 / 必填缺失 / 字符串约束 / 超时 / 内部异常
 * - 重名覆盖
 */
class ToolRegistryTest {

    private fun fakeTool(
        name: String,
        category: ToolCategory = ToolCategory.TIMING,
        accessLevel: AccessLevel = AccessLevel.READ,
        parameters: List<ToolParameter> = emptyList(),
        block: suspend (Map<String, Any?>) -> ToolResult = { ToolResult.Success(name, "ok") },
    ): ToolDefinition {
        val toolName = name
        val toolCategory = category
        val toolAccess = accessLevel
        val toolParams = parameters
        return object : ToolDefinition {
            override val name: String = toolName
            override val description: String = "fake $toolName"
            override val category: ToolCategory = toolCategory
            override val accessLevel: AccessLevel = toolAccess
            override val parameters: List<ToolParameter> = toolParams
            override val returnType: KClass<*> = String::class
            override suspend fun execute(args: Map<String, Any?>): ToolResult = block(args)
            override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
                name = toolName,
                description = "fake $toolName",
                category = toolCategory,
                accessLevel = toolAccess,
                parameters = toolParams,
                returnExample = "\"ok\"",
                errorExamples = emptyList(),
                usageExamples = emptyList(),
            )
        }
    }

    @Test
    fun `registers tools provided via constructor`() {
        val registry = ToolRegistry(setOf(fakeTool("foo")), ToolEventBus())

        assertNotNull(registry.getTool("foo"))
        assertNull(registry.getTool("bar"))
        assertEquals(1, registry.getAllTools().size)
    }

    @Test
    fun `getToolsByCategory filters correctly`() {
        val registry = ToolRegistry(
            setOf(
                fakeTool("a", ToolCategory.TIMING),
                fakeTool("b", ToolCategory.STATISTICS),
            ),
            ToolEventBus(),
        )

        val timing = registry.getToolsByCategory(ToolCategory.TIMING)
        assertEquals(1, timing.size)
        assertEquals("a", timing.first().name)
    }

    @Test
    fun `getAvailableTools returns tools at or below caller access level`() {
        val registry = ToolRegistry(
            setOf(
                fakeTool("r", accessLevel = AccessLevel.READ),
                fakeTool("w", accessLevel = AccessLevel.WRITE),
                fakeTool("f", accessLevel = AccessLevel.FULL),
            ),
            ToolEventBus(),
        )

        val available = registry.getAvailableTools(AccessLevel.WRITE).map { it.name }.toSet()
        assertEquals(setOf("r", "w"), available)
    }

    @Test
    fun `executeTool returns NotFound for unknown tool`() = runTest {
        val registry = ToolRegistry(emptySet(), ToolEventBus())

        val result = registry.executeTool("unknown", emptyMap())

        assertTrue(result is ToolResult.Error)
        assertTrue((result as ToolResult.Error).error is ToolError.NotFound)
    }

    @Test
    fun `executeTool returns ValidationError when required parameter missing`() = runTest {
        val tool = fakeTool(
            name = "needsId",
            parameters = listOf(
                ToolParameter(
                    name = "id",
                    description = "id",
                    type = ParameterType.STRING,
                    required = true,
                ),
            ),
        )
        val registry = ToolRegistry(setOf(tool), ToolEventBus())

        val result = registry.executeTool("needsId", emptyMap())

        assertTrue(result is ToolResult.Error)
        val error = (result as ToolResult.Error).error
        assertTrue(error is ToolError.ValidationError)
        assertTrue((error as ToolError.ValidationError).message.contains("id"))
    }

    @Test
    fun `executeTool enforces string minLength constraint`() = runTest {
        val tool = fakeTool(
            name = "minLen",
            parameters = listOf(
                ToolParameter(
                    name = "code",
                    description = "code",
                    type = ParameterType.STRING,
                    required = true,
                    constraints = ParameterConstraint(minLength = 3),
                ),
            ),
        )
        val registry = ToolRegistry(setOf(tool), ToolEventBus())

        val result = registry.executeTool("minLen", mapOf("code" to "ab"))

        assertTrue(result is ToolResult.Error)
        assertTrue((result as ToolResult.Error).error is ToolError.ValidationError)
    }

    @Test
    fun `executeTool wraps timeouts as TimeoutError`() = runTest {
        val tool = fakeTool("slow") {
            delay(60_000)
            ToolResult.Success("slow", "should not reach")
        }
        val registry = ToolRegistry(setOf(tool), ToolEventBus())

        val result = registry.executeTool("slow", emptyMap(), timeoutMillis = 50)

        assertTrue(result is ToolResult.Error)
        assertTrue((result as ToolResult.Error).error is ToolError.TimeoutError)
    }

    @Test
    fun `executeTool wraps unexpected exceptions as InternalError`() = runTest {
        val tool = fakeTool("boom") { throw IllegalStateException("kaboom") }
        val registry = ToolRegistry(setOf(tool), ToolEventBus())

        val result = registry.executeTool("boom", emptyMap())

        assertTrue(result is ToolResult.Error)
        val error = (result as ToolResult.Error).error
        assertTrue(error is ToolError.InternalError)
        assertEquals("kaboom", (error as ToolError.InternalError).message)
    }

    @Test
    fun `register overwrites existing tool with same name`() {
        val v1 = fakeTool("dup", category = ToolCategory.TIMING)
        val v2 = fakeTool("dup", category = ToolCategory.STATISTICS)
        val registry = ToolRegistry(setOf(v1), ToolEventBus())

        registry.register(v2)

        assertEquals(1, registry.getAllTools().size)
        assertEquals(ToolCategory.STATISTICS, registry.getTool("dup")?.category)
    }
}
