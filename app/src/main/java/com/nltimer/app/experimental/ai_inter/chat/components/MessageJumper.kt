package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MessageJumper(
    show: Boolean,
    state: LazyListState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = show,
        modifier = modifier,
        enter = slideInHorizontally { it * 2 },
        exit = slideOutHorizontally { it * 2 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            JumpButton(Icons.Default.KeyboardDoubleArrowUp, "跳到顶部") {
                scope.launch { state.scrollToItem(0) }
            }
            JumpButton(Icons.Default.KeyboardArrowUp, "上翻一项") {
                scope.launch {
                    state.animateScrollToItem((state.firstVisibleItemIndex - 1).coerceAtLeast(0))
                }
            }
            JumpButton(Icons.Default.KeyboardArrowDown, "下翻一项") {
                scope.launch { state.animateScrollToItem(state.firstVisibleItemIndex + 1) }
            }
            JumpButton(Icons.Default.KeyboardDoubleArrowDown, "跳到底部") {
                scope.launch { state.scrollToItem((state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) }
            }
        }
    }
}

@Composable
private fun JumpButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        tonalElevation = 4.dp,
    ) {
        Icon(icon, desc, modifier = Modifier.padding(8.dp).size(20.dp))
    }
}
