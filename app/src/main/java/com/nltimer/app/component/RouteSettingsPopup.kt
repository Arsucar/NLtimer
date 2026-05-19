package com.nltimer.app.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavHostController
import com.nltimer.app.navigation.NLtimerRoutes
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledCorner

private const val POPUP_WIDTH_RATIO = 0.45f

internal data class RouteSettingsNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

internal val routeSettingsNavItems = listOf(
    RouteSettingsNavItem(NLtimerRoutes.CATEGORIES, "分类管理", Icons.Default.Category),
    RouteSettingsNavItem(NLtimerRoutes.MANAGEMENT_ACTIVITIES, "活动管理", Icons.AutoMirrored.Filled.List),
    RouteSettingsNavItem(NLtimerRoutes.TAG_MANAGEMENT, "标签管理", Icons.AutoMirrored.Filled.Label),
    RouteSettingsNavItem(NLtimerRoutes.SETTINGS, "设置", Icons.Default.Settings),
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun RouteSettingsPopup(
    currentRoute: String?,
    onDismiss: () -> Unit,
    onHomeLayoutChange: (HomeLayout) -> Unit,
    onShowTimeSideBarChange: (Boolean) -> Unit,
    navController: NavHostController,
    popupOffsetY: Int = -100,
    initialShowLayoutOptions: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val popupWidth = screenWidth * POPUP_WIDTH_RATIO
    val currentLayout = LocalTheme.current.homeLayout
    var showLayoutOptions by remember(initialShowLayoutOptions) { mutableStateOf(initialShowLayoutOptions) }

    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(x = 16, y = popupOffsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = modifier
                .width(popupWidth)
                .wrapContentHeight(),
            shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                if (currentRoute == NLtimerRoutes.HOME) {
                    PopupItem(
                        icon = Icons.Default.Dashboard,
                        label = if (showLayoutOptions) "返回设置" else "更改布局",
                        onClick = { showLayoutOptions = !showLayoutOptions },
                    )

                    if (showLayoutOptions) {
                        HomeLayout.entries.forEach { layout ->
                            val isSelected = currentLayout == layout
                            PopupItem(
                                icon = if (isSelected) Icons.Default.Search else Icons.Default.Dashboard,
                                label = when (layout) {
                                    HomeLayout.GRID -> "网格时间"
                                    HomeLayout.TIMELINE_REVERSE -> "时间轴(反)"
                                    HomeLayout.LOG -> "行为日志"
                                    HomeLayout.MOMENT -> "瞬间"
                                    HomeLayout.TEXT_LIST -> "纯文字列表"
                                },
                                onClick = {
                                    onHomeLayoutChange(layout)
                                    onDismiss()
                                },
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (!showLayoutOptions && currentLayout == HomeLayout.GRID) {
                        PopupSwitchItem(
                            icon = Icons.Default.Timelapse,
                            label = "侧边时间轴",
                            checked = LocalTheme.current.showTimeSideBar,
                            onCheckedChange = {
                                onShowTimeSideBarChange(it)
                            },
                        )
                    }

                    if (!showLayoutOptions) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                if (!showLayoutOptions) {
                    routeSettingsNavItems.forEach { item ->
                        PopupItem(
                            icon = item.icon,
                            label = item.label,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.padding(end = 12.dp),
            tint = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
        )
    }
}

@Composable
private fun PopupSwitchItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = styledAlpha(0.5f)) else Color.Transparent,
                shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL)),
            )
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.padding(end = 12.dp),
            tint = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            modifier = Modifier.weight(1f),
        )
    }
}
