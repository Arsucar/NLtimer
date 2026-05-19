package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue // 如果你用了 by 关键字，这个也必须导
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickModel: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
val animatedBottomPadding by animateDpAsState(
    targetValue = if (isImeVisible) 0.dp else 70.dp,
    label = "IME Padding Animation"
)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // .padding(bottom = if (isImeVisible) 0.dp else 70.dp)
            .navigationBarsPadding()
            .padding(bottom = animatedBottomPadding)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .hazeEffect(
                state = hazeState,
                style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow),
            ),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("输入消息…") },
                modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                maxLines = 5,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                enabled = !isSending,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onPickModel) {
                    Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("模型", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))

                val containerColor = when {
                    isSending -> MaterialTheme.colorScheme.errorContainer
                    text.isBlank() -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.primary
                }
                val onContainerColor = when {
                    isSending -> MaterialTheme.colorScheme.onErrorContainer
                    text.isBlank() -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onPrimary
                }
                Surface(shape = CircleShape, color = containerColor, modifier = Modifier.size(36.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = if (isSending) onStop else onSend,
                            enabled = isSending || text.isNotBlank(),
                        ) {
                            Icon(
                                imageVector = if (isSending) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (isSending) "停止" else "发送",
                                tint = onContainerColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
