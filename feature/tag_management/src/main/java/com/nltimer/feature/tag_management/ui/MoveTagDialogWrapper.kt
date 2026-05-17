package com.nltimer.feature.tag_management.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Tag
import com.nltimer.core.behaviorui.sheet.CategoryGroup
import com.nltimer.core.behaviorui.sheet.CategoryPickerDialog
import com.nltimer.core.behaviorui.sheet.StringCategoryCategorizable

@Composable
fun MoveTagDialogWrapper(
    tag: Tag,
    currentCategory: String?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    val categoryItems = remember(categories) {
        categories.map { StringCategoryCategorizable(it) }
    }
    val groupedCategories = remember(categoryItems) {
        listOf(
            CategoryGroup(
                id = 0L,
                name = "所有分类",
                items = categoryItems,
                onClear = {
                    onConfirm(null)
                },
                clearLabel = "清除",
            )
        )
    }

    CategoryPickerDialog(
        title = "将「${tag.name}」移动到：",
        items = categoryItems,
        categoryGroups = groupedCategories,
        selectedId = currentCategory?.hashCode()?.toLong() ?: 0L,
        onItemSelected = { id ->
            val selectedName = categoryItems.find { it.itemId == id }?.name
            onConfirm(selectedName)
        },
        onDismiss = onDismiss,
        showHeader = false,
    )
}
