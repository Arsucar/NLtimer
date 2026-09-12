package com.nltimer.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 通用确认对话框
 *
 * 用于删除等需要用户二次确认的操作场景。
 *
 * 三按钮场景（如删除含事件行为：删除 / 保留事件 / 取消）：[neutralText] + [onNeutral]
 * 均非 null 时在确认按钮左侧渲染第三个按钮；其余情况不渲染，既有调用点零行为变化。
 *
 * @param title 对话框标题
 * @param message 确认提示信息
 * @param confirmText 确认按钮文本，默认"确定"
 * @param dismissText 取消按钮文本，默认"取消"
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调
 * @param modifier 修饰符
 * @param confirmTextColor 确认按钮文字颜色，可用于删除等破坏性操作的红色高亮
 * @param neutralText 中性（第三）按钮文本；null 不显示
 * @param onNeutral 中性按钮回调
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    confirmText: String = "确定",
    dismissText: String = "取消",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmTextColor: Color? = null,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (neutralText != null && onNeutral != null) {
                Row {
                    TextButton(onClick = onNeutral) {
                        Text(text = neutralText, color = Color.Unspecified)
                    }
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = confirmText,
                            color = confirmTextColor ?: Color.Unspecified,
                        )
                    }
                }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = confirmText,
                        color = confirmTextColor ?: Color.Unspecified,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
        modifier = modifier,
    )
}
