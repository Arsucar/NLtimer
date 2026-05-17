package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.data.util.mmddFormatter
import com.nltimer.core.designsystem.theme.styledAlpha
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

private val ColonTextColor = Color(0xFF0A1034)

@Composable
private fun colonTextColor() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
    ColonTextColor
} else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = styledAlpha(0.6f))
}

@Composable
fun DualTimePicker(
    startTime: LocalDateTime = LocalDateTime.now(),
    endTime: LocalDateTime = LocalDateTime.now(),
    animate: Boolean = true,
    onDurationChanged: (Duration) -> Unit = {},
    onTimesChanged: (LocalDateTime, LocalDateTime) -> Unit = { _, _ -> },
    onLeftCenterClick: () -> Unit = {},
    onRightCenterClick: () -> Unit = {},
) {
    val startProperty = remember(startTime) { startTime.withSecond(0).withNano(0) }
    val endProperty = remember(endTime) { endTime.withSecond(0).withNano(0) }

    val today = LocalDate.now()

    val sharedDates = remember(today, startProperty, endProperty) {
        buildDateItems(
            today = today,
            requiredDates = setOf(startProperty.toLocalDate(), endProperty.toLocalDate()),
        )
    }

    val todayIndex = sharedDates.indexOfFirst { it.date == today }
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    var leftSelectedDate by remember { mutableStateOf(sharedDates.find { it.date == startProperty.toLocalDate() } ?: sharedDates[todayIndex]) }
    var leftSelectedHour by remember { mutableStateOf(startProperty.hour.toString().padStart(2, '0')) }
    var leftSelectedMinute by remember { mutableStateOf(startProperty.minute.toString().padStart(2, '0')) }

    var rightSelectedDate by remember { mutableStateOf(sharedDates.find { it.date == endProperty.toLocalDate() } ?: sharedDates[todayIndex]) }
    var rightSelectedHour by remember { mutableStateOf(endProperty.hour.toString().padStart(2, '0')) }
    var rightSelectedMinute by remember { mutableStateOf(endProperty.minute.toString().padStart(2, '0')) }

    val lastNotifiedStart = remember { mutableStateOf(startProperty) }
    val lastNotifiedEnd = remember { mutableStateOf(endProperty) }

    LaunchedEffect(startProperty, endProperty) {
        if (startProperty != lastNotifiedStart.value) {
            sharedDates.find { it.date == startProperty.toLocalDate() }?.let { leftSelectedDate = it }
            leftSelectedHour = startProperty.hour.toString().padStart(2, '0')
            leftSelectedMinute = startProperty.minute.toString().padStart(2, '0')
            lastNotifiedStart.value = startProperty
        }
        if (endProperty != lastNotifiedEnd.value) {
            sharedDates.find { it.date == endProperty.toLocalDate() }?.let { rightSelectedDate = it }
            rightSelectedHour = endProperty.hour.toString().padStart(2, '0')
            rightSelectedMinute = endProperty.minute.toString().padStart(2, '0')
            lastNotifiedEnd.value = endProperty
        }
    }

    val leftDateTime = remember(leftSelectedDate, leftSelectedHour, leftSelectedMinute) {
        leftSelectedDate.date.atTime(leftSelectedHour.toInt(), leftSelectedMinute.toInt())
    }
    val rightDateTime = remember(rightSelectedDate, rightSelectedHour, rightSelectedMinute) {
        rightSelectedDate.date.atTime(rightSelectedHour.toInt(), rightSelectedMinute.toInt())
    }

    LaunchedEffect(leftDateTime, rightDateTime) {
        if (leftDateTime != lastNotifiedStart.value || rightDateTime != lastNotifiedEnd.value) {
            lastNotifiedStart.value = leftDateTime
            lastNotifiedEnd.value = rightDateTime
            onDurationChanged(Duration.between(leftDateTime, rightDateTime))
            onTimesChanged(leftDateTime, rightDateTime)
        }
    }

    DualTimePickerLayout(
        sharedDates = sharedDates,
        hours = hours,
        minutes = minutes,
        todayIndex = todayIndex,
        animate = animate,
        leftSelectedDate = leftSelectedDate,
        leftSelectedHour = leftSelectedHour,
        leftSelectedMinute = leftSelectedMinute,
        rightSelectedDate = rightSelectedDate,
        rightSelectedHour = rightSelectedHour,
        rightSelectedMinute = rightSelectedMinute,
        onLeftDateChanged = { leftSelectedDate = it },
        onLeftHourChanged = { leftSelectedHour = it },
        onLeftMinuteChanged = { leftSelectedMinute = it },
        onRightDateChanged = { rightSelectedDate = it },
        onRightHourChanged = { rightSelectedHour = it },
        onRightMinuteChanged = { rightSelectedMinute = it },
        onLeftCenterClick = onLeftCenterClick,
        onRightCenterClick = onRightCenterClick,
    )
}

@Composable
private fun DualTimePickerLayout(
    sharedDates: List<DateItem>,
    hours: List<String>,
    minutes: List<String>,
    todayIndex: Int,
    animate: Boolean,
    leftSelectedDate: DateItem,
    leftSelectedHour: String,
    leftSelectedMinute: String,
    rightSelectedDate: DateItem,
    rightSelectedHour: String,
    rightSelectedMinute: String,
    onLeftDateChanged: (DateItem) -> Unit,
    onLeftHourChanged: (String) -> Unit,
    onLeftMinuteChanged: (String) -> Unit,
    onRightDateChanged: (DateItem) -> Unit,
    onRightHourChanged: (String) -> Unit,
    onRightMinuteChanged: (String) -> Unit,
    onLeftCenterClick: () -> Unit,
    onRightCenterClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TimePickerSection(
            dates = sharedDates,
            hours = hours,
            minutes = minutes,
            selectedDate = leftSelectedDate,
            selectedHour = leftSelectedHour,
            selectedMinute = leftSelectedMinute,
            initialDateIndex = todayIndex,
            animate = animate,
            onDateChanged = onLeftDateChanged,
            onHourChanged = onLeftHourChanged,
            onMinuteChanged = onLeftMinuteChanged,
            modifier = Modifier.weight(1f),
            onCenterClick = onLeftCenterClick,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(top = 40.dp, bottom = 16.dp),
        )

        TimePickerSection(
            dates = sharedDates,
            hours = hours,
            minutes = minutes,
            selectedDate = rightSelectedDate,
            selectedHour = rightSelectedHour,
            selectedMinute = rightSelectedMinute,
            initialDateIndex = todayIndex,
            animate = animate,
            onDateChanged = onRightDateChanged,
            onHourChanged = onRightHourChanged,
            onMinuteChanged = onRightMinuteChanged,
            modifier = Modifier.weight(1f),
            onCenterClick = onRightCenterClick,
        )
    }
}

@Composable
fun SingleTimePicker(
    startTime: LocalDateTime = LocalDateTime.now(),
    animate: Boolean = true,
    onTimeChanged: (LocalDateTime) -> Unit = {},
    onCenterClick: () -> Unit = {},
) {
    val startProperty = remember(startTime) { startTime.withSecond(0).withNano(0) }

    val today = LocalDate.now()

    val sharedDates = remember(today, startProperty) {
        buildDateItems(today = today, requiredDates = setOf(startProperty.toLocalDate()))
    }

    val todayIndex = sharedDates.indexOfFirst { it.date == today }
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    var selectedDate by remember { mutableStateOf(sharedDates.find { it.date == startProperty.toLocalDate() } ?: sharedDates[todayIndex]) }
    var selectedHour by remember { mutableStateOf(startProperty.hour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(startProperty.minute.toString().padStart(2, '0')) }

    val lastNotifiedTime = remember { mutableStateOf(startProperty) }

    LaunchedEffect(startProperty) {
        if (startProperty != lastNotifiedTime.value) {
            sharedDates.find { it.date == startProperty.toLocalDate() }?.let { selectedDate = it }
            selectedHour = startProperty.hour.toString().padStart(2, '0')
            selectedMinute = startProperty.minute.toString().padStart(2, '0')
            lastNotifiedTime.value = startProperty
        }
    }

    val currentDateTime = remember(selectedDate, selectedHour, selectedMinute) {
        selectedDate.date.atTime(selectedHour.toInt(), selectedMinute.toInt())
    }

    LaunchedEffect(currentDateTime) {
        if (currentDateTime != lastNotifiedTime.value) {
            lastNotifiedTime.value = currentDateTime
            onTimeChanged(currentDateTime)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TimePickerSection(
            dates = sharedDates,
            hours = hours,
            minutes = minutes,
            selectedDate = selectedDate,
            selectedHour = selectedHour,
            selectedMinute = selectedMinute,
            initialDateIndex = todayIndex,
            animate = animate,
            onDateChanged = { selectedDate = it },
            onHourChanged = { selectedHour = it },
            onMinuteChanged = { selectedMinute = it },
            modifier = Modifier.weight(1f),
            onCenterClick = onCenterClick,
        )
    }
}

@Composable
internal fun TimePickerSection(
    modifier: Modifier = Modifier,
    dates: List<DateItem>,
    hours: List<String>,
    minutes: List<String>,
    selectedDate: DateItem,
    selectedHour: String,
    selectedMinute: String,
    initialDateIndex: Int = 0,
    animate: Boolean = true,
    onDateChanged: (DateItem) -> Unit,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit,
    onCenterClick: () -> Unit = {},
) {
    val itemHeight = 32.dp
    Column(
        modifier = modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            TimePickerHighlight(itemHeight = itemHeight)
            TimePickerWheelsRow(
                dates = dates,
                hours = hours,
                minutes = minutes,
                selectedDate = selectedDate,
                selectedHour = selectedHour,
                selectedMinute = selectedMinute,
                initialDateIndex = initialDateIndex,
                animate = animate,
                onDateChanged = onDateChanged,
                onHourChanged = onHourChanged,
                onMinuteChanged = onMinuteChanged,
                onCenterClick = onCenterClick,
            )
        }
    }
}

@Composable
private fun TimePickerHighlight(itemHeight: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
    )
}

@Composable
private fun TimePickerWheelsRow(
    dates: List<DateItem>,
    hours: List<String>,
    minutes: List<String>,
    selectedDate: DateItem,
    selectedHour: String,
    selectedMinute: String,
    initialDateIndex: Int,
    animate: Boolean,
    onDateChanged: (DateItem) -> Unit,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit,
    onCenterClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DateWheelPicker(
            items = dates,
            selectedItem = selectedDate,
            onItemSelected = onDateChanged,
            itemHeight = 32.dp,
            initialScrollIndex = initialDateIndex,
            animate = animate,
            modifier = Modifier.weight(1.5f),
            onCenterClick = onCenterClick,
        )
        WheelPicker(
            items = hours,
            selectedItem = selectedHour,
            onItemSelected = onHourChanged,
            itemHeight = 32.dp,
            animate = animate,
            modifier = Modifier.weight(1f),
            onCenterClick = onCenterClick,
        )
        TimePickerColon()
        WheelPicker(
            items = minutes,
            selectedItem = selectedMinute,
            onItemSelected = onMinuteChanged,
            itemHeight = 32.dp,
            animate = animate,
            modifier = Modifier.weight(1f),
            onCenterClick = onCenterClick,
        )
    }
}

@Composable
private fun TimePickerColon() {
    Text(
        text = ":",
        color = colonTextColor(),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 0.dp),
    )
}

private fun buildDateItems(
    today: LocalDate,
    requiredDates: Set<LocalDate>,
): List<DateItem> {
    val defaultStart = today.minusDays(3)
    val defaultEnd = today.plusDays(3)
    val start = requiredDates.minOrNull()?.let { minOf(defaultStart, it) } ?: defaultStart
    val end = requiredDates.maxOrNull()?.let { maxOf(defaultEnd, it) } ?: defaultEnd
    val dateFormatter = mmddFormatter

    return generateSequence(start) { date ->
        date.plusDays(1).takeIf { !it.isAfter(end) }
    }.map { date ->
        DateItem(
            displayText = if (date == today) "今天" else date.format(dateFormatter),
            date = date,
            isSpecial = date == today,
        )
    }.toList()
}
