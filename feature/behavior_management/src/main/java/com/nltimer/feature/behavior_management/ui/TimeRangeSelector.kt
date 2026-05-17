package com.nltimer.feature.behavior_management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.theme.appAssistChipBorder
import com.nltimer.feature.behavior_management.model.TimeRangePreset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(
    currentPreset: TimeRangePreset,
    currentDate: LocalDate,
    currentHour: Int,
    onPresetChange: (TimeRangePreset) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (Int) -> Unit,
    onNavigate: (direction: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var presetExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val navLabel = when (currentPreset) {
        TimeRangePreset.FOUR_HOURS -> "4小时"
        TimeRangePreset.EIGHT_HOURS -> "8小时"
        TimeRangePreset.ONE_DAY -> "天"
        TimeRangePreset.THREE_DAYS -> "3天"
        TimeRangePreset.SEVEN_DAYS -> "周"
        TimeRangePreset.ONE_MONTH -> "月"
        TimeRangePreset.ONE_YEAR -> "年"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box {
            AssistChip(
                onClick = { presetExpanded = true },
                label = {
                    Text(text = currentPreset.label, style = MaterialTheme.typography.labelMedium)
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                border = appAssistChipBorder(),
            )
            DropdownMenu(
                expanded = presetExpanded,
                onDismissRequest = { presetExpanded = false },
            ) {
                TimeRangePreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = {
                            onPresetChange(preset)
                            presetExpanded = false
                        },
                    )
                }
            }
        }

        IconButton(onClick = { onNavigate(-1) }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "前一$navLabel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AssistChip(
            onClick = { showDatePicker = true },
            label = {
                Text(
                    text = formatDate(currentDate, currentPreset, currentHour),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            border = appAssistChipBorder(),
        )

        IconButton(onClick = { onNavigate(1) }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "后一$navLabel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDatePicker) {
        when (currentPreset) {
            TimeRangePreset.FOUR_HOURS, TimeRangePreset.EIGHT_HOURS -> {
                HourAnchorPickerDialog(
                    initialDate = currentDate,
                    initialHour = currentHour,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { date, hour ->
                        onDateChange(date)
                        onHourChange(hour)
                        showDatePicker = false
                    }
                )
            }
            TimeRangePreset.ONE_YEAR -> {
                YearPickerDialog(
                    initialYear = currentDate.year,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { year ->
                        onDateChange(currentDate.withYear(year).withDayOfYear(1))
                        showDatePicker = false
                    }
                )
            }
            TimeRangePreset.ONE_MONTH -> {
                MonthPickerDialog(
                    initialDate = currentDate,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { date ->
                        onDateChange(date)
                        showDatePicker = false
                    }
                )
            }
            else -> {
                val initialMillis = currentDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selected = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                onDateChange(selected)
                            }
                            showDatePicker = false
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("取消")
                        }
                    },
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

private fun formatDate(date: LocalDate, preset: TimeRangePreset, hour: Int): String {
    return when (preset) {
        TimeRangePreset.ONE_YEAR -> "${date.year}年"
        TimeRangePreset.ONE_MONTH -> "${date.year}年${date.monthValue}月"
        TimeRangePreset.FOUR_HOURS, TimeRangePreset.EIGHT_HOURS ->
            "${date.year}/${date.monthValue}/${date.dayOfMonth} ${hour.coerceIn(0, 23).toString().padStart(2, '0')}:00"
        else -> "${date.year}/${date.monthValue}/${date.dayOfMonth}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourAnchorPickerDialog(
    initialDate: LocalDate,
    initialHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Int) -> Unit,
) {
    val initialMillis = initialDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    var selectedHour by remember(initialHour) { mutableStateOf(initialHour.coerceIn(0, 23)) }
    val anchors = (0..23).toList()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                    Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } ?: initialDate
                onConfirm(selectedDate, selectedHour)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    ) {
        Column {
            DatePicker(state = datePickerState)
            LazyVerticalGrid(
                modifier = Modifier.height(160.dp),
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(anchors) { hour ->
                    AssistChip(
                        onClick = { selectedHour = hour },
                        label = { Text("${hour.toString().padStart(2, '0')}:00") },
                        colors = if (hour == selectedHour) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun YearPickerDialog(
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val currentYear = LocalDate.now().year
    val years = (currentYear - 10..currentYear + 10).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("选择年份") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(years) { year ->
                    AssistChip(
                        onClick = { onConfirm(year) },
                        label = { Text(year.toString()) },
                        colors = if (year == initialYear) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun MonthPickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val months = (1..12).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("${initialDate.year}年 选择月份") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(months) { month ->
                    AssistChip(
                        onClick = { onConfirm(initialDate.withMonth(month).withDayOfMonth(1)) },
                        label = { Text("${month}月") },
                        colors = if (month == initialDate.monthValue) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
        }
    )
}
