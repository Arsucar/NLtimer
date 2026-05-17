package com.nltimer.core.designsystem.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.LocalTheme

val LocalNavBarWidth = compositionLocalOf<MutableState<Dp>> { mutableStateOf(0.dp) }

@Composable
fun BoxScope.BottomBarDragFab(
    state: DragFabState,
    icon: ImageVector,
    dragOptions: List<String>,
    modifier: Modifier = Modifier,
    label: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    cornerRadius: Dp = 28.dp,
    onClick: () -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    val isCenterFab = LocalTheme.current.bottomBarMode == BottomBarMode.CENTER_FAB
    val navBarWidth = LocalNavBarWidth.current.value

    DragActionFab(
        state = state,
        icon = icon,
        label = label,
        containerColor = containerColor,
        contentColor = contentColor,
        cornerRadius = cornerRadius,
        onClick = onClick,
        onOptionSelected = onOptionSelected,
        modifier = modifier
            .then(
                if (isCenterFab && navBarWidth > 0.dp) {
                    Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = navBarWidth + 20.dp, end = 20.dp, bottom = 8.dp)
                        .fillMaxWidth()
                } else {
                    Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 12.dp, bottom = 8.dp)
                }
            ),
    )

    FabDragOptions(
        state = state,
        options = dragOptions,
    )
}
