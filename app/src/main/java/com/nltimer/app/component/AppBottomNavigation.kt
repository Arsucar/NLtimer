package com.nltimer.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.nltimer.app.navigation.NLtimerRoutes
import com.nltimer.core.designsystem.component.DragMenuState
import com.nltimer.core.designsystem.component.DraggableMenuAnchor
import com.nltimer.core.designsystem.component.LocalNavBarWidth
import com.nltimer.core.designsystem.component.rememberDragMenuState
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledCorner
import kotlin.math.roundToInt

internal data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

internal val navItems = listOf(
    NavItem(NLtimerRoutes.HOME, "主页", Icons.Default.Home),
    NavItem(NLtimerRoutes.STATS, "统计", Icons.Default.BarChart),
    NavItem(NLtimerRoutes.SETTINGS, "设置", Icons.Default.Settings),
)

private val SettingsDragMenuWidth = 180.dp
private val SettingsDragMenuGap = 12.dp

@Composable
fun AppBottomNavigation(
    navController: NavHostController,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(
        initialValue = navController.currentBackStackEntry,
    )
    val currentDestination = currentBackStackEntry?.destination

    NavigationBar(modifier = modifier) {
        navItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            if (item.route == NLtimerRoutes.SETTINGS) {
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier.clickable(onClick = onSettingsClick),
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text(item.label) },
                    selected = selected,
                    onClick = onSettingsClick,
                )
            } else {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(item.label) },
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppFloatingBottomBar(
    navController: NavHostController,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(
        initialValue = navController.currentBackStackEntry,
    )
    val currentDestination = currentBackStackEntry?.destination

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        ) {
            navItems.filter { it.route != NLtimerRoutes.SETTINGS }.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                FloatingToolbarTab(
                    selected = selected,
                    icon = item.icon,
                    label = item.label,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 4.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onSettingsClick),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "菜单",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarTab(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = backgroundColor,
        shape = CircleShape,
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .size(52.dp, 40.dp)
            .clip(CircleShape),
        onClick = onClick,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppCenterFabBottomBar(
    navController: NavHostController,
    onSettingsClick: () -> Unit,
    settingsDragOptions: List<String> = emptyList(),
    onSettingsDragOptionSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(
        initialValue = navController.currentBackStackEntry,
    )
    val currentDestination = currentBackStackEntry?.destination
    val navBarWidthState = LocalNavBarWidth.current
    val density = LocalDensity.current
    val settingsDragState = rememberDragMenuState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .onGloballyPositioned { settingsDragState.containerPositionInWindow = it.positionInWindow() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 4.dp)
                .onGloballyPositioned { coords ->
                    navBarWidthState.value = with(density) { coords.size.width.toDp() }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableMenuAnchor(
                state = settingsDragState,
                modifier = Modifier.size(56.dp),
                onOptionSelected = onSettingsDragOptionSelected,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(onClick = onSettingsClick),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "菜单",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
            ) {
                navItems.filter { it.route != NLtimerRoutes.SETTINGS }.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    FloatingToolbarTab(
                        selected = selected,
                        icon = item.icon,
                        label = item.label,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        }

        CompactSettingsDragMenu(
            state = settingsDragState,
            options = settingsDragOptions,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun CompactSettingsDragMenu(
    state: DragMenuState,
    options: List<String>,
    modifier: Modifier = Modifier,
) {
    if (!state.isDragging || options.isEmpty()) return

    val density = LocalDensity.current
    val menuGapPx = with(density) { SettingsDragMenuGap.toPx() }
    val optionsX = state.anchorLayoutPosition.x - state.containerPositionInWindow.x
    val optionsY = state.anchorLayoutPosition.y - state.optionsRowHeight - menuGapPx - state.containerPositionInWindow.y

    Surface(
        modifier = modifier
            .width(SettingsDragMenuWidth)
            .wrapContentHeight()
            .offset {
                IntOffset(
                    x = optionsX.coerceAtLeast(0f).roundToInt(),
                    y = optionsY.roundToInt(),
                )
            }
            .onGloballyPositioned { coords ->
                state.optionsRowHeight = coords.size.height.toFloat()
            },
        shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            options.forEach { option ->
                CompactSettingsDragMenuItem(
                    state = state,
                    option = option,
                )
            }
        }
    }
}

@Composable
private fun CompactSettingsDragMenuItem(
    state: DragMenuState,
    option: String,
    modifier: Modifier = Modifier,
) {
    val isHovered = state.hoveredOption == option
    val tint = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(
                color = if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = styledAlpha(0.55f)) else Color.Transparent,
                shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL)),
            )
            .onGloballyPositioned { coords ->
                val position = coords.positionInWindow()
                val size = coords.size
                state.optionsLayoutBounds[option] = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height,
                )
            }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = settingsDragOptionIcon(option),
            contentDescription = option,
            tint = tint,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = option,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
        )
    }
}

private fun settingsDragOptionIcon(option: String): ImageVector =
    when (option) {
        "更改布局" -> Icons.Default.Dashboard
        "时间标签设置" -> Icons.Default.Schedule
        "开启侧边时间轴", "关闭侧边时间轴" -> Icons.Default.Timelapse
        else -> Icons.Default.Settings
    }
