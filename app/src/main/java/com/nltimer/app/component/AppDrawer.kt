package com.nltimer.app.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.R as DR
import com.nltimer.core.designsystem.component.cardColorForStrategy
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledCorner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nltimer.app.navigation.NLtimerRoutes
import java.util.concurrent.CopyOnWriteArrayList

private val MIN_DRAWER_WIDTH = 280.dp

internal data class DrawerMenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

internal val drawerMenuItems = CopyOnWriteArrayList(listOf(
    DrawerMenuItem(NLtimerRoutes.AI_INTER, "AI Inter", Icons.Default.AutoAwesome),
    DrawerMenuItem(NLtimerRoutes.THEME_SETTINGS, "主题配置", Icons.Default.Brightness5),
    DrawerMenuItem(NLtimerRoutes.SETTINGS, "设置", Icons.Default.Settings),
))

private val drawerManagementItems = listOf(
    DrawerMenuItem(NLtimerRoutes.CATEGORIES, "分类", Icons.Default.Category),
    DrawerMenuItem(NLtimerRoutes.MANAGEMENT_ACTIVITIES, "活动", Icons.AutoMirrored.Filled.List),
    DrawerMenuItem(NLtimerRoutes.TAG_MANAGEMENT, "标签", Icons.AutoMirrored.Filled.Label),
    DrawerMenuItem(NLtimerRoutes.BEHAVIOR_MANAGEMENT, "行为", Icons.AutoMirrored.Filled.EventNote),
)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDrawer(
    navController: NavHostController,
    onClose: () -> Unit,
    totalDurationMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val maxDrawerWidth = (screenWidthDp * 0.5f).coerceAtLeast(MIN_DRAWER_WIDTH)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateTo: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        onClose()
    }

    ModalDrawerSheet(
        modifier = modifier.widthIn(
            min = MIN_DRAWER_WIDTH,
            max = maxDrawerWidth,
        ),
    ) {
        Text(
            text = "NLtimer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )

        TotalDurationCircle(totalDurationMs = totalDurationMs)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "管理",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = drawerManagementItems.size,
        ) {
            drawerManagementItems.forEach { item ->
                ManagementChip(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { navigateTo(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val themeItem = drawerMenuItems.firstOrNull { it.route == NLtimerRoutes.THEME_SETTINGS }
        themeItem?.let { item ->
            NavigationDrawerItem(
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { navigateTo(item.route) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        drawerMenuItems
            .filter { it.route != NLtimerRoutes.THEME_SETTINGS }
            .forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(imageVector = item.icon, contentDescription = null) },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = { navigateTo(item.route) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun TotalDurationCircle(totalDurationMs: Long) {
    val strategy = LocalTheme.current.style.cardColorStrategy
    val containerColor = cardColorForStrategy(strategy)
    val cornerRadius = styledCorner(ShapeTokens.CORNER_FULL)
    val flexRounded = remember {
        FontFamily(
            Font(
                resId = DR.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(800),
                    FontVariation.Setting("ROND", 100f),
                ),
            ),
        )
    }

    val durationText = remember(totalDurationMs) {
        val totalSeconds = totalDurationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = flexRounded,
                ),
            )
        }
    }
}

@Composable
private fun ManagementChip(
    item: DrawerMenuItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
