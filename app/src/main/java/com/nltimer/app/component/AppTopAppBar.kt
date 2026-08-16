package com.nltimer.app.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.designsystem.R as DR
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.toDisplayString
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

data class MomentFilterOption(
    val label: String,
    val key: String,
)

data class MomentSortOption(
    val label: String,
    val key: String,
)

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberDateTitleFont(): FontFamily = remember {
    FontFamily(
        Font(
            resId = DR.font.google_sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800),
            ),
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun TitleRow(
    title: String,
    isDateTitle: Boolean,
    titleStyle: @Composable (Boolean, FontFamily) -> TextStyle,
    layoutLabel: String?,
    onLayoutChange: ((HomeLayout) -> Unit)?,
    momentFilterLabel: String?,
    momentFilterOptions: List<MomentFilterOption>,
    momentFilterKey: String?,
    onMomentFilterChange: ((String) -> Unit)?,
    momentSortOptions: List<MomentSortOption>,
    momentSortKey: String?,
    onMomentSortChange: ((String) -> Unit)?,
    rowModifier: Modifier = Modifier,
) {
    var layoutMenuExpanded by remember { mutableStateOf(false) }
    var momentMenuExpanded by remember { mutableStateOf(false) }
    val dateTitleFont = rememberDateTitleFont()

    Row(verticalAlignment = Alignment.Bottom, modifier = rowModifier) {
        Text(
            title,
            style = titleStyle(isDateTitle, dateTitleFont),
        )
        if (layoutLabel != null) {
            Box {
                Text(
                    text = " $layoutLabel",
                    style = TextStyle(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onLayoutChange != null) {
                        Modifier.clickable { layoutMenuExpanded = true }
                    } else Modifier,
                )
                if (onLayoutChange != null) {
                    DropdownMenu(
                        expanded = layoutMenuExpanded,
                        onDismissRequest = { layoutMenuExpanded = false },
                    ) {
                        HomeLayout.entries.forEach { layout ->
                            DropdownMenuItem(
                                text = { Text(layout.toDisplayString()) },
                                onClick = {
                                    onLayoutChange(layout)
                                    layoutMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        if (momentFilterLabel != null) {
            Box {
                Text(
                    text = momentFilterLabel,
                    style = TextStyle(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onMomentFilterChange != null) {
                        Modifier
                            .padding(start = 16.dp)
                            .clickable { momentMenuExpanded = true }
                    } else Modifier.padding(start = 16.dp),
                )
                if (onMomentFilterChange != null) {
                    DropdownMenu(
                        expanded = momentMenuExpanded,
                        onDismissRequest = { momentMenuExpanded = false },
                    ) {
                        momentFilterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        color = if (momentFilterKey == option.key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    onMomentFilterChange(option.key)
                                    momentMenuExpanded = false
                                },
                            )
                        }
                        if (momentSortOptions.isNotEmpty()) {
                            HorizontalDivider()
                            momentSortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label,
                                            color = if (momentSortKey == option.key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        onMomentSortChange?.invoke(option.key)
                                        momentMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HazeWrappedTitle(
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    if (hazeState != null) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow),
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            content()
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun AppTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    isDateTitle: Boolean = false,
    hazeState: HazeState? = null,
    layoutLabel: String? = null,
    onLayoutChange: ((HomeLayout) -> Unit)? = null,
    momentFilterLabel: String? = null,
    momentFilterOptions: List<MomentFilterOption> = emptyList(),
    momentFilterKey: String? = null,
    onMomentFilterChange: ((String) -> Unit)? = null,
    momentSortOptions: List<MomentSortOption> = emptyList(),
    momentSortKey: String? = null,
    onMomentSortChange: ((String) -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            HazeWrappedTitle(hazeState) {
                TitleRow(
                    title = title,
                    isDateTitle = isDateTitle,
                    titleStyle = { isDate, font ->
                        if (isDate) {
                            MaterialTheme.typography.titleLarge.copy(
                                fontFamily = font,
                                fontWeight = FontWeight.W800,
                                fontSize = 14.sp,
                            )
                        } else {
                            MaterialTheme.typography.titleLarge.copy(
                                fontFamily = font,
                                fontWeight = FontWeight.W800,
                            )
                        }
                    },
                    layoutLabel = layoutLabel,
                    onLayoutChange = onLayoutChange,
                    momentFilterLabel = momentFilterLabel,
                    momentFilterOptions = momentFilterOptions,
                    momentFilterKey = momentFilterKey,
                    onMomentFilterChange = onMomentFilterChange,
                    momentSortOptions = momentSortOptions,
                    momentSortKey = momentSortKey,
                    onMomentSortChange = onMomentSortChange,
                )
            }
        },
        navigationIcon = navigationIcon,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun AppCollapsedTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    isDateTitle: Boolean = false,
    hazeState: HazeState? = null,
    layoutLabel: String? = null,
    onLayoutChange: ((HomeLayout) -> Unit)? = null,
    momentFilterLabel: String? = null,
    momentFilterOptions: List<MomentFilterOption> = emptyList(),
    momentFilterKey: String? = null,
    onMomentFilterChange: ((String) -> Unit)? = null,
    momentSortOptions: List<MomentSortOption> = emptyList(),
    momentSortKey: String? = null,
    onMomentSortChange: ((String) -> Unit)? = null,
) {
    TopAppBar(
        title = {
            HazeWrappedTitle(hazeState) {
                TitleRow(
                    title = title,
                    isDateTitle = isDateTitle,
                    titleStyle = { isDate, font ->
                        if (isDate) {
                            MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = font,
                                fontWeight = FontWeight.W800,
                                fontSize = 21.sp,
                                lineHeight = 21.sp,
                            )
                        } else {
                            MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = font,
                                fontWeight = FontWeight.W800,
                                fontSize = 32.sp,
                                lineHeight = 32.sp,
                            )
                        }
                    },
                    rowModifier = Modifier.padding(bottom = 5.dp),
                    layoutLabel = layoutLabel,
                    onLayoutChange = onLayoutChange,
                    momentFilterLabel = momentFilterLabel,
                    momentFilterOptions = momentFilterOptions,
                    momentFilterKey = momentFilterKey,
                    onMomentFilterChange = onMomentFilterChange,
                    momentSortOptions = momentSortOptions,
                    momentSortKey = momentSortKey,
                    onMomentSortChange = onMomentSortChange,
                )
            }
        },
        navigationIcon = navigationIcon,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}
