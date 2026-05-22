package com.example.custompicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// --- Theme Colors matching your image ---
val DarkNavy = Color(0xFF0A1034)
val LightGrayText = Color(0xFFC4C4C4)
val HighlightBg = Color(0xFFF2F2F2)
val DividerColor = Color(0xFFEFEFEF)

@Composable
fun CustomDualTimePickerScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            DualTimePicker()
        }
    }
}

@Composable
fun DualTimePicker() {
    // Sample Data
    val leftDates = listOf("03/16", "03/17", "03/18", "03/19", "03/20")
    val rightDates = listOf("前天", "昨天", "今天", "明天", "后天")
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    // State
    var leftSelectedDate by remember { mutableStateOf("03/18") }
    var leftSelectedHour by remember { mutableStateOf("20") }
    var leftSelectedMinute by remember { mutableStateOf("08") }

    var rightSelectedDate by remember { mutableStateOf("今天") }
    var rightSelectedHour by remember { mutableStateOf("09") }
    var rightSelectedMinute by remember { mutableStateOf("44") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // Left Section
        TimePickerSection(
            title = "上尾",
            dates = leftDates,
            hours = hours,
            minutes = minutes,
            selectedDate = leftSelectedDate,
            selectedHour = leftSelectedHour,
            selectedMinute = leftSelectedMinute,
            onDateChanged = { leftSelectedDate = it },
            onHourChanged = { leftSelectedHour = it },
            onMinuteChanged = { leftSelectedMinute = it },
            modifier = Modifier.weight(1f)
        )

        // Vertical Divider
        Divider(
            color = DividerColor,
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(top = 40.dp, bottom = 16.dp)
        )

        // Right Section
        TimePickerSection(
            title = "当前",
            dates = rightDates,
            hours = hours,
            minutes = minutes,
            selectedDate = rightSelectedDate,
            selectedHour = rightSelectedHour,
            selectedMinute = rightSelectedMinute,
            onDateChanged = { rightSelectedDate = it },
            onHourChanged = { rightSelectedHour = it },
            onMinuteChanged = { rightSelectedMinute = it },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TimePickerSection(
    title: String,
    dates: List<String>,
    hours: List<String>,
    minutes: List<String>,
    selectedDate: String,
    selectedHour: String,
    selectedMinute: String,
    onDateChanged: (String) -> Unit,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40.dp

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Black Header Tag
        Box(
            modifier = Modifier
                .background(Color.Black, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Picker Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selected Row Highlight Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighlightBg)
            )

            // The Wheels
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Date Picker
                WheelPicker(
                    items = dates,
                    selectedItem = selectedDate,
                    onItemSelected = onDateChanged,
                    itemHeight = itemHeight,
                    modifier = Modifier.weight(1.5f)
                )

                // Hour Picker
                WheelPicker(
                    items = hours,
                    selectedItem = selectedHour,
                    onItemSelected = onHourChanged,
                    itemHeight = itemHeight,
                    modifier = Modifier.weight(1f)
                )

                // Static Colon separator
                Text(
                    text = ":",
                    color = DarkNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Minute Picker
                WheelPicker(
                    items = minutes,
                    selectedItem = selectedMinute,
                    onItemSelected = onMinuteChanged,
                    itemHeight = itemHeight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A custom Wheel Picker implementation using LazyColumn
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemHeight: Dp = 40.dp,
    visibleItemsCount: Int = 3,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current

    // Add empty items at the top and bottom to allow centering the first and last items
    val paddingCount = visibleItemsCount / 2
    val paddedItems = remember(items) {
        val list = mutableListOf<T?>()
        repeat(paddingCount) { list.add(null) }
        list.addAll(items)
        repeat(paddingCount) { list.add(null) }
        list
    }

    // Scroll to the initially selected item
    LaunchedEffect(selectedItem) {
        val index = items.indexOf(selectedItem)
        if (index != -1 && listState.firstVisibleItemIndex != index) {
            listState.scrollToItem(index)
        }
    }

    // Update selected item based on scroll position
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> items.getOrNull(index) }
            .distinctUntilChanged()
            .collect { item ->
                if (item != null) {
                    onItemSelected(item)
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(itemHeight * visibleItemsCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(paddedItems.size) { index ->
            val item = paddedItems[index]
            val isSelected = item == selectedItem

            Box(
                modifier = Modifier.height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                if (item != null) {
                    Text(
                        text = item.toString(),
                        style = TextStyle(
                            fontSize = if (isSelected) 18.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) DarkNavy else LightGrayText,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}