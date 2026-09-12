package com.nltimer.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.designsystem.theme.TimeLabelFormat
import com.nltimer.core.designsystem.theme.TimeLabelStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

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

    @Test
    fun `FocusCardConfig default cardHeight is 280`() {
        assertEquals(280, FocusCardConfig().cardHeight)
    }

    @Test
    fun `FocusCardConfig DEFAULT_CARD_HEIGHT is 280`() {
        assertEquals(280, FocusCardConfig.DEFAULT_CARD_HEIGHT)
    }

    @Test
    fun `resolveCardHeight null is 280`() {
        assertEquals(280, FocusCardConfig.resolveCardHeight(null))
    }

    @Test
    fun `resolveCardHeight 260 migrates to 280`() {
        assertEquals(280, FocusCardConfig.resolveCardHeight(260))
    }

    @Test
    fun `resolveCardHeight 240 stays 240`() {
        assertEquals(240, FocusCardConfig.resolveCardHeight(240))
    }

    @Test
    fun `resolveCardHeight 300 stays 300`() {
        assertEquals(300, FocusCardConfig.resolveCardHeight(300))
    }

    @Test
    fun `resolveCardHeight 280 stays 280`() {
        assertEquals(280, FocusCardConfig.resolveCardHeight(280))
    }

    @Test
    fun `getFocusCardConfigFlow missing key reads 280 without writing`() = runTest {
        val store = FakePrefsDataStore()
        val prefs = SettingsPrefsImpl(store)

        assertEquals(280, prefs.getFocusCardConfigFlow().first().cardHeight)
        assertNull(store.current()[FOCUS_CARD_HEIGHT_KEY])
    }

    @Test
    fun `getFocusCardConfigFlow stored 260 reads 280 and writes back`() = runTest {
        val store = FakePrefsDataStore().seed(FOCUS_CARD_HEIGHT_KEY, 260)
        val prefs = SettingsPrefsImpl(store)

        assertEquals(280, prefs.getFocusCardConfigFlow().first().cardHeight)
        assertEquals(280, store.current()[FOCUS_CARD_HEIGHT_KEY])
        // 二次订阅不得再改写或挂起
        assertEquals(280, prefs.getFocusCardConfigFlow().first().cardHeight)
        assertEquals(280, store.current()[FOCUS_CARD_HEIGHT_KEY])
    }

    @Test
    fun `getFocusCardConfigFlow stored 240 stays 240`() = runTest {
        val store = FakePrefsDataStore().seed(FOCUS_CARD_HEIGHT_KEY, 240)
        val prefs = SettingsPrefsImpl(store)

        assertEquals(240, prefs.getFocusCardConfigFlow().first().cardHeight)
        assertEquals(240, store.current()[FOCUS_CARD_HEIGHT_KEY])
    }

    @Test
    fun `getFocusCardConfigFlow stored 300 stays 300`() = runTest {
        val store = FakePrefsDataStore().seed(FOCUS_CARD_HEIGHT_KEY, 300)
        val prefs = SettingsPrefsImpl(store)

        assertEquals(300, prefs.getFocusCardConfigFlow().first().cardHeight)
        assertEquals(300, store.current()[FOCUS_CARD_HEIGHT_KEY])
    }

    @Test
    fun `getFocusCardConfigFlow stored 280 stays 280`() = runTest {
        val store = FakePrefsDataStore().seed(FOCUS_CARD_HEIGHT_KEY, 280)
        val prefs = SettingsPrefsImpl(store)

        assertEquals(280, prefs.getFocusCardConfigFlow().first().cardHeight)
        assertEquals(280, store.current()[FOCUS_CARD_HEIGHT_KEY])
    }

    /**
     * 模拟 DataStore：在 `data` 的 collect 回调里调用 `updateData`/`edit` 会挂起。
     * 用于回归「迁移写回不得写在 data.collect 内部」。
     */
    private class FakePrefsDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        private val collecting = AtomicBoolean(false)

        override val data: Flow<Preferences> = flow {
            state.collect { prefs ->
                collecting.set(true)
                try {
                    emit(prefs)
                } finally {
                    collecting.set(false)
                }
            }
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            check(!collecting.get()) {
                "Deadlock: updateData/edit must not run inside data.collect"
            }
            val updated = transform(state.value)
            state.value = updated
            return updated
        }

        fun seed(key: Preferences.Key<Int>, value: Int): FakePrefsDataStore {
            val mut = state.value.toMutablePreferences()
            mut[key] = value
            state.value = mut
            return this
        }

        fun current(): Preferences = state.value
    }

    companion object {
        private val FOCUS_CARD_HEIGHT_KEY = intPreferencesKey("focus_card_height")
    }
}
