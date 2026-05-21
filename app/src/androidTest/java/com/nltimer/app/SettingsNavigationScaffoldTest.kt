package com.nltimer.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsNavigationScaffoldTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsPopup_showsNavigationItems() {
        composeTestRule.onNodeWithText("设置").performClick()

        composeTestRule.onNodeWithText("主题配置").assertExists()
        composeTestRule.onNodeWithText("分类管理").assertExists()
        composeTestRule.onNodeWithText("数据管理").assertExists()
    }

    @Test
    fun themeSettingsRoute_showsBackButton() {
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.onNodeWithText("主题配置").performClick()

        composeTestRule.onNodeWithText("主题配置").assertExists()
        composeTestRule.onNodeWithContentDescription("返回").assertExists()
    }
}
