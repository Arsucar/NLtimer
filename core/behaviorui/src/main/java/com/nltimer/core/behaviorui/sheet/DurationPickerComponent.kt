package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

private val HOUR_ITEMS = (0..23).map { "${it}时" }
private val MINUTE_ITEMS = (0..59).map { "${it}分" }

@Composable
fun DurationPicker(
    durationMs: Long?,
    onDurationChanged: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    emphasisColor: Color = MaterialTheme.colorScheme.secondary,
    animate: Boolean = true,
) {
    val totalMinutes = (durationMs ?: 0L) / 60_000L
    val hours = (totalMinutes / 60).toInt().coerceIn(0, 23)
    val minutes = (totalMinutes % 60).toInt().coerceIn(0, 59)

    var selectedHour by remember { mutableStateOf(HOUR_ITEMS[hours]) }
    var selectedMinute by remember { mutableStateOf(MINUTE_ITEMS[minutes]) }
    var isEditing by remember { mutableStateOf(false) }
    var editFieldValue by remember { mutableStateOf(TextFieldValue()) }

    var wheelVersion by remember { mutableStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            val t = (durationMs ?: 0L) / 60_000L
            val h = (t / 60).toInt().coerceIn(0, 23)
            val m = (t % 60).toInt().coerceIn(0, 59)
            val digits = if (h > 0) "${h}${m.toString().padStart(2, '0')}" else m.toString()
            editFieldValue = TextFieldValue(
                text = digits,
                selection = TextRange(digits.length),
            )
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditing) {
                Text(
                    text = "预计：",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = emphasisColor,
                    ),
                )
                BasicTextField(
                    value = editFieldValue,
                    onValueChange = { newValue ->
                        val filtered = newValue.text.filter { it.isDigit() }
                        if (filtered.length <= 4) {
                            editFieldValue = TextFieldValue(
                                text = filtered,
                                selection = TextRange(filtered.length),
                            )
                        }
                    },
                    modifier = Modifier
                        .widthIn(max = 48.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = emphasisColor,
                    ),
                    cursorBrush = SolidColor(emphasisColor),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val ms = commitFromText(
                                editText = editFieldValue.text,
                                onDurationChanged = onDurationChanged,
                                onSetHour = { selectedHour = it },
                                onSetMinute = { selectedMinute = it },
                                onBumpVersion = { wheelVersion++ },
                            )
                            if (ms != null) {
                                focusManager.clearFocus()
                                isEditing = false
                            }
                        },
                    ),
                    singleLine = true,
                )
                val preview = if (editFieldValue.text.isNotEmpty()) {
                    val num = editFieldValue.text.toLongOrNull() ?: 0L
                    val ph = (num / 60).toInt().coerceIn(0, 23)
                    val pm = (num % 60).toInt().coerceIn(0, 59)
                    "→ ${ph}时${pm.toString().padStart(2, '0')}分"
                } else ""
                if (preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = emphasisColor.copy(alpha = 0.6f),
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                val durationText = if (durationMs != null && durationMs > 0L) {
                    val h = durationMs / 3_600_000L
                    val m = (durationMs % 3_600_000L) / 60_000L
                    "${h}时${m}分"
                } else "未设置"

                Text(
                    text = "预计：$durationText",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (durationMs != null && durationMs > 0L) emphasisColor
                        else emphasisColor.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier
                        .clickable { isEditing = true }
                        .padding(vertical = 2.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val itemHeight = 32.dp
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    key(wheelVersion) {
                        WheelPicker(
                            items = HOUR_ITEMS,
                            selectedItem = selectedHour,
                            onItemSelected = {
                                selectedHour = it
                                emitDuration(selectedHour, selectedMinute, onDurationChanged)
                            },
                            itemHeight = 32.dp,
                            animate = animate,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    key(wheelVersion) {
                        WheelPicker(
                            items = MINUTE_ITEMS,
                            selectedItem = selectedMinute,
                            onItemSelected = {
                                selectedMinute = it
                                emitDuration(selectedHour, selectedMinute, onDurationChanged)
                            },
                            itemHeight = 32.dp,
                            animate = animate,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun commitFromText(
    editText: String,
    onDurationChanged: (Long?) -> Unit,
    onSetHour: (String) -> Unit,
    onSetMinute: (String) -> Unit,
    onBumpVersion: () -> Unit,
): Long? {
    val digits = editText.filter { it.isDigit() }
    if (digits.isBlank()) {
        onDurationChanged(null)
        onSetHour(HOUR_ITEMS[0])
        onSetMinute(MINUTE_ITEMS[0])
        onBumpVersion()
        return null
    }
    val totalMinutes = digits.toLongOrNull()?.coerceAtMost(23 * 60L + 59) ?: 0L
    val h = (totalMinutes / 60).toInt().coerceIn(0, 23)
    val m = (totalMinutes % 60).toInt().coerceIn(0, 59)
    val ms = totalMinutes * 60_000L
    onDurationChanged(if (ms > 0L) ms else null)
    onSetHour(HOUR_ITEMS[h])
    onSetMinute(MINUTE_ITEMS[m])
    onBumpVersion()
    return ms
}

private fun emitDuration(
    hourLabel: String,
    minuteLabel: String,
    onDurationChanged: (Long?) -> Unit,
) {
    val h = hourLabel.removeSuffix("时").toIntOrNull() ?: 0
    val m = minuteLabel.removeSuffix("分").toIntOrNull() ?: 0
    val totalMs = (h * 60L + m) * 60_000L
    onDurationChanged(if (totalMs > 0L) totalMs else null)
}
