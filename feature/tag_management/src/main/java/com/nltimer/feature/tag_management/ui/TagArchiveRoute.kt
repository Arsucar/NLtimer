package com.nltimer.feature.tag_management.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.nltimer.feature.tag_management.viewmodel.TagArchiveViewModel

@Composable
fun TagArchiveRoute(
    viewModel: TagArchiveViewModel = hiltViewModel(),
) {
    TagArchiveScreen(viewModel = viewModel)
}
