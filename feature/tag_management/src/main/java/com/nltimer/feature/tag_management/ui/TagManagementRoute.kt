package com.nltimer.feature.tag_management.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.nltimer.feature.tag_management.viewmodel.TagManagementViewModel

/**
 * 标签管理界面的路由入口
 *
 * 负责创建 ViewModel 并将其传递给实际的界面组件。
 *
 * @param onNavigateBack 返回上一页的回调
 * @param viewModel 标签管理 ViewModel，由 Hilt 自动注入
 */
@Composable
fun TagManagementRoute(
    onNavigateBack: () -> Unit,
    viewModel: TagManagementViewModel = hiltViewModel(),
) {
    TagManagementScreen(
        viewModel = viewModel,
    )
}
