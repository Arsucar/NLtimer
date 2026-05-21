package com.nltimer.core.data

import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.designsystem.theme.TimeLabelFormat
import com.nltimer.core.designsystem.theme.TimeLabelStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPrefsImplTest {

    @Test
    fun `DialogGridConfig default values`() {
        val config = DialogGridConfig()
        assertEquals(ChipDisplayMode.Filled, config.activityDisplayMode)
        assertEquals(GridLayoutMode.Horizontal, config.activityLayoutMode)
        assertEquals(2, config.activityColumnLines)
        assertEquals(2, config.activityHorizontalLines)
        assertTrue(config.activityUseColorForText)
        assertEquals(ChipDisplayMode.Filled, config.tagDisplayMode)
        assertEquals(GridLayoutMode.Horizontal, config.tagLayoutMode)
        assertEquals(2, config.tagColumnLines)
        assertEquals(2, config.tagHorizontalLines)
        assertTrue(config.tagUseColorForText)
        assertTrue(config.showBehaviorNature)
        assertEquals(PathDrawMode.StartToEnd, config.pathDrawMode)
        assertEquals(false, config.autoMatchNote)
    }

    @Test
    fun `TimeLabelConfig default values`() {
        val config = TimeLabelConfig()
        assertTrue(config.visible)
        assertEquals(TimeLabelStyle.PILL, config.style)
        assertEquals(TimeLabelFormat.HH_MM, config.format)
    }

    @Test
    fun `TimeLabelConfig serialization roundtrip visible pill hh_mm`() {
        val config = TimeLabelConfig(visible = true, style = TimeLabelStyle.PILL, format = TimeLabelFormat.HH_MM)
        val serialized = "${config.visible}|${config.style.name}|${config.format.name}"
        val parts = serialized.split("|")
        assertEquals(3, parts.size)
        assertEquals("true", parts[0])
        assertEquals("PILL", parts[1])
        assertEquals("HH_MM", parts[2])
    }

    @Test
    fun `TimeLabelConfig serialization roundtrip hidden plain h_mm`() {
        val config = TimeLabelConfig(visible = false, style = TimeLabelStyle.PLAIN, format = TimeLabelFormat.H_MM)
        val serialized = "${config.visible}|${config.style.name}|${config.format.name}"
        val parts = serialized.split("|")
        assertEquals("false", parts[0])
        assertEquals("PLAIN", parts[1])
        assertEquals("H_MM", parts[2])
    }

    @Test
    fun `DialogGridConfig copy preserves values`() {
        val config = DialogGridConfig(
            activityDisplayMode = ChipDisplayMode.Underline,
            activityLayoutMode = GridLayoutMode.Vertical,
            activityColumnLines = 3,
            activityHorizontalLines = 4,
            activityUseColorForText = false,
            tagUseColorForText = false,
            tagDisplayMode = ChipDisplayMode.Underline,
            tagLayoutMode = GridLayoutMode.Vertical,
            tagColumnLines = 3,
            tagHorizontalLines = 4,
            showBehaviorNature = false,
            pathDrawMode = PathDrawMode.BothSidesToMiddle,
            autoMatchNote = true,
        )
        assertEquals(ChipDisplayMode.Underline, config.activityDisplayMode)
        assertEquals(GridLayoutMode.Vertical, config.activityLayoutMode)
        assertEquals(3, config.activityColumnLines)
        assertEquals(4, config.activityHorizontalLines)
        assertEquals(false, config.activityUseColorForText)
        assertEquals(PathDrawMode.BothSidesToMiddle, config.pathDrawMode)
        assertEquals(false, config.showBehaviorNature)
        assertEquals(true, config.autoMatchNote)
    }

    @Test
    fun `tag categories roundtrip serialization`() {
        val categories = setOf("工作", "学习", "生活")
        val serialized = categories.joinToString(",")
        val deserialized = if (serialized.isBlank()) emptySet() else serialized.split(",").toSet()
        assertEquals(categories, deserialized)
    }

    @Test
    fun `empty tag categories serialization`() {
        val categories = emptySet<String>()
        val serialized = categories.joinToString(",")
        val deserialized = if (serialized.isBlank()) emptySet() else serialized.split(",").toSet()
        assertEquals(categories, deserialized)
    }

    @Test
    fun `single tag category serialization`() {
        val categories = setOf("工作")
        val serialized = categories.joinToString(",")
        val deserialized = if (serialized.isBlank()) emptySet() else serialized.split(",").toSet()
        assertEquals(categories, deserialized)
    }
}
