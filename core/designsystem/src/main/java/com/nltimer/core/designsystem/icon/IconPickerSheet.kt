package com.nltimer.core.designsystem.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nltimer.core.designsystem.R
import java.text.BreakIterator
@Composable
fun IconPickerSheet(
    currentIconKey: String?,
    onIconSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    defaultEmoji: String = "📖",
) {
    val initialTab = when {
        currentIconKey?.startsWith("hi:") == true -> 0
        currentIconKey?.startsWith("mi:") == true -> 1
        else -> 0
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showManualInput by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var manualInput by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val manualFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(showSearch) {
        if (showSearch) searchFocusRequester.requestFocus()
    }
    LaunchedEffect(showManualInput) {
        if (showManualInput) manualFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = {
            if (showSearch) {
                showSearch = false
                searchQuery = ""
                focusManager.clearFocus()
            } else if (showManualInput) {
                showManualInput = false
                manualInput = ""
                focusManager.clearFocus()
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp),
            ) {                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.icon_picker_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                showManualInput = false
                            },
                            placeholder = { Text(stringResource(R.string.icon_picker_search), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .focusRequester(searchFocusRequester),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (showManualInput) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { raw ->
                                manualInput = truncateToCodePoints(raw, maxCodePoints = 4)
                                showSearch = false
                            },
                            placeholder = { Text(stringResource(R.string.icon_picker_manual_input), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (manualInput.isNotBlank()) {
                                        onIconSelected(manualInput)
                                    }
                                },
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .focusRequester(manualFocusRequester),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(
                            onClick = {
                                if (manualInput.isNotBlank()) {
                                    onIconSelected(manualInput)
                                }
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(stringResource(R.string.icon_picker_confirm), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (!showManualInput) {
                        IconButton(
                            onClick = {
                                showManualInput = !showManualInput
                                if (showManualInput) showSearch = false
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Keyboard,
                                contentDescription = stringResource(R.string.icon_picker_manual_input),
                                tint = if (showManualInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (!showSearch) {
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                                if (showSearch) showManualInput = false
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.icon_picker_search),
                                tint = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = { onIconSelected(null) }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.icon_picker_reset),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val tabs = listOf(
                    stringResource(R.string.icon_picker_tab_hugeicons),
                    stringResource(R.string.icon_picker_tab_icons),
                    stringResource(R.string.icon_picker_tab_emoji),
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                        )
                    }
                }

                when (selectedTab) {
                    0 -> HugeIconTab(
                        currentIconKey = currentIconKey,
                        searchQuery = searchQuery,
                        onIconSelected = {
                            onIconSelected(it)
                            onDismiss()
                        },
                    )
                    1 -> MaterialIconTab(
                        searchQuery = searchQuery,
                        onIconSelected = {
                            onIconSelected(it)
                            onDismiss()
                        },
                    )
                    2 -> EmojiTab(
                        currentIconKey = currentIconKey,
                        defaultEmoji = defaultEmoji,
                        searchQuery = searchQuery,
                        onIconSelected = {
                            onIconSelected(it)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}
@Composable
private fun EmojiTab(
    currentIconKey: String?,
    defaultEmoji: String,
    searchQuery: String,
    onIconSelected: (String?) -> Unit,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(EmojiCategory.entries.first()) }

    val filteredEmojis = remember(searchQuery, selectedCategory) {
        val byCategory = EmojiCatalog.findByCategory(selectedCategory)
        if (searchQuery.isBlank()) byCategory else EmojiCatalog.searchEmoji(searchQuery)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryScrollRow(
            categories = EmojiCategory.entries,
            selectedCategory = selectedCategory,
            categoryLabel = { emojiCategoryLabel(it) },
            onCategorySelected = { cat ->
                selectedCategory = cat
            },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredEmojis, key = { it.emoji + it.name }) { entry ->
                val isCurrent = currentIconKey == entry.emoji
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onIconSelected(entry.emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entry.emoji,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
@Composable
private fun HugeIconTab(
    currentIconKey: String?,
    searchQuery: String,
    onIconSelected: (String?) -> Unit,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(HugeIconCategory.entries.first()) }

    val filteredIcons = remember(searchQuery, selectedCategory) {
        val byCategory = HugeIconCatalog.icons.filter { it.category == selectedCategory }
        if (searchQuery.isBlank()) byCategory else {
            val q = searchQuery.lowercase()
            byCategory.filter { it.name.contains(q, ignoreCase = true) || it.keywords.any { it.contains(q, ignoreCase = true) } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryScrollRow(
            categories = HugeIconCategory.entries,
            selectedCategory = selectedCategory,
            categoryLabel = { hugeIconCategoryLabel(it) },
            onCategorySelected = { cat ->
                selectedCategory = cat
            },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredIcons, key = { it.name }) { entry ->
                val isCurrent = currentIconKey == "hi:${entry.name}"
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onIconSelected("hi:${entry.name}") },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = entry.imageVectorProvider(),
                            contentDescription = entry.name,
                            modifier = Modifier.size(24.dp),
                            tint = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialIconTab(
    searchQuery: String,
    onIconSelected: (String?) -> Unit,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(IconCategory.entries.first()) }

    val filteredIcons = remember(searchQuery, selectedCategory) {
        val byCategory = MaterialIconCatalog.icons.filter { it.category == selectedCategory }
        if (searchQuery.isBlank()) byCategory else {
            val q = searchQuery.lowercase()
            byCategory.filter { it.name.contains(q, ignoreCase = true) || it.keywords.any { it.contains(q, ignoreCase = true) } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryScrollRow(
            categories = IconCategory.entries,
            selectedCategory = selectedCategory,
            categoryLabel = { iconCategoryLabel(it) },
            onCategorySelected = { cat ->
                selectedCategory = cat
            },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredIcons, key = { "${it.style}:${it.name}" }) { entry ->
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onIconSelected("mi:${entry.style}:${entry.name}") },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = entry.imageVectorProvider(),
                            contentDescription = entry.name,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> CategoryScrollRow(
    categories: List<T>,
    selectedCategory: T,
    categoryLabel: @Composable (T) -> String,
    onCategorySelected: (T) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        categories.forEach { category ->
            CategoryTab(
                label = categoryLabel(category),
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
            textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None,
        ),
        color = color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun emojiCategoryLabel(category: EmojiCategory): String = when (category) {
    EmojiCategory.FREQUENT -> stringResource(R.string.emoji_category_frequent)
    EmojiCategory.EXPRESSIONS -> stringResource(R.string.emoji_category_expressions)
    EmojiCategory.GESTURES -> stringResource(R.string.emoji_category_gestures)
    EmojiCategory.ANIMALS -> stringResource(R.string.emoji_category_animals)
    EmojiCategory.FOOD -> stringResource(R.string.emoji_category_food)
    EmojiCategory.TRAVEL -> stringResource(R.string.emoji_category_travel)
    EmojiCategory.ACTIVITIES -> stringResource(R.string.emoji_category_activities)
    EmojiCategory.OBJECTS -> stringResource(R.string.emoji_category_objects)
    EmojiCategory.NATURE -> stringResource(R.string.emoji_category_nature)
    EmojiCategory.SYMBOLS -> stringResource(R.string.emoji_category_symbols)
}

@Composable
private fun iconCategoryLabel(category: IconCategory): String = when (category) {
    IconCategory.ACTION -> stringResource(R.string.icon_category_action)
    IconCategory.COMMUNICATION -> stringResource(R.string.icon_category_communication)
    IconCategory.CONTENT -> stringResource(R.string.icon_category_content)
    IconCategory.DEVICE -> stringResource(R.string.icon_category_device)
    IconCategory.IMAGE -> stringResource(R.string.icon_category_image)
    IconCategory.MAPS -> stringResource(R.string.icon_category_maps)
    IconCategory.NAVIGATION -> stringResource(R.string.icon_category_navigation)
    IconCategory.SOCIAL -> stringResource(R.string.icon_category_social)
    IconCategory.EDITOR -> stringResource(R.string.icon_category_editor)
    IconCategory.AV -> stringResource(R.string.icon_category_av)
    IconCategory.PLACES -> stringResource(R.string.icon_category_places)
    IconCategory.HARDWARE -> stringResource(R.string.icon_category_hardware)
}

@Composable
private fun hugeIconCategoryLabel(category: HugeIconCategory): String = when (category) {
    HugeIconCategory.GENERAL -> stringResource(R.string.hi_category_general)
    HugeIconCategory.ARROWS -> stringResource(R.string.hi_category_arrows)
    HugeIconCategory.COMMUNICATION -> stringResource(R.string.hi_category_communication)
    HugeIconCategory.MEDIA -> stringResource(R.string.hi_category_media)
    HugeIconCategory.FILES -> stringResource(R.string.hi_category_files)
    HugeIconCategory.BUSINESS -> stringResource(R.string.hi_category_business)
    HugeIconCategory.USERS -> stringResource(R.string.hi_category_users)
    HugeIconCategory.DATETIME -> stringResource(R.string.hi_category_datetime)
    HugeIconCategory.DEVICES -> stringResource(R.string.hi_category_devices)
    HugeIconCategory.MAPS -> stringResource(R.string.hi_category_maps)
    HugeIconCategory.WEATHER -> stringResource(R.string.hi_category_weather)
    HugeIconCategory.EDUCATION -> stringResource(R.string.hi_category_education)
    HugeIconCategory.HEALTH -> stringResource(R.string.hi_category_health)
    HugeIconCategory.OTHER -> stringResource(R.string.hi_category_other)
}

private fun truncateToCodePoints(text: String, maxCodePoints: Int): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val sb = StringBuilder()
    var count = 0
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE && count < maxCodePoints) {
        sb.append(text, start, end)
        count++
        start = end
        end = iterator.next()
    }
    return sb.toString()
}
