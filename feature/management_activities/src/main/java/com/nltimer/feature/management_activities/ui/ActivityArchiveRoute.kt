package com.nltimer.feature.management_activities.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.nltimer.feature.management_activities.viewmodel.ActivityArchiveViewModel

@Composable
fun ActivityArchiveRoute(
    viewModel: ActivityArchiveViewModel = hiltViewModel(),
) {
    ActivityArchiveScreen(viewModel = viewModel)
}
