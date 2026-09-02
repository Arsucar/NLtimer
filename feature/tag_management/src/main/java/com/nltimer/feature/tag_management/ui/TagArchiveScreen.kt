package com.nltimer.feature.tag_management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.behaviorui.sheet.CategoryGroupCard
import com.nltimer.core.behaviorui.sheet.TagCategorizable
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.util.formatArchiveDate
import com.nltimer.core.designsystem.component.ArchiveItemDetailDialog
import com.nltimer.core.designsystem.component.EmptyStateView
import com.nltimer.core.designsystem.component.LoadingScreen
import com.nltimer.feature.tag_management.viewmodel.TagArchiveViewModel

@Composable
fun TagArchiveScreen(
    viewModel: TagArchiveViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var collapsedGroups by remember { mutableStateOf(emptySet<String>()) }
    var detailTag by remember { mutableStateOf<Tag?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    Box(modifier = modifier.fillMaxSize().navigationBarsPadding()) {
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: "加载失败",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
            }
            uiState.groups.isEmpty() -> EmptyStateView(
                message = "暂无归档标签",
                subtitle = "可在标签编辑页底部点「归档」",
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = uiState.groups,
                        key = { _, group -> group.name },
                    ) { index, group ->
                        val items = remember(group.items) {
                            group.items.map { TagCategorizable(it) }
                        }
                        CategoryGroupCard(
                            index = index,
                            groupName = group.name,
                            items = items,
                            collapsed = group.name in collapsedGroups,
                            onToggleCollapsed = {
                                collapsedGroups = if (group.name in collapsedGroups) {
                                    collapsedGroups - group.name
                                } else {
                                    collapsedGroups + group.name
                                }
                            },
                            showDragHandle = false,
                            onAddItem = null,
                            emptyText = "暂无归档标签",
                            onItemSelected = { id ->
                                detailTag = group.items.firstOrNull { it.id == id }
                            },
                            onItemLongClick = { id ->
                                detailTag = group.items.firstOrNull { it.id == id }
                            },
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    detailTag?.let { tag ->
        ArchiveItemDetailDialog(
            title = tag.name,
            archivedAtText = formatArchiveDate(tag.archivedAt),
            archiveNote = tag.archiveNote,
            onDismiss = { detailTag = null },
            onRestore = {
                val id = tag.id
                detailTag = null
                viewModel.restore(id)
            },
        )
    }
}
