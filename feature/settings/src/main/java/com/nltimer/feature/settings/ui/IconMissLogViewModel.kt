package com.nltimer.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.database.dao.IconSearchMissDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IconMissLogViewModel @Inject constructor(
    private val iconSearchMissDao: IconSearchMissDao,
) : ViewModel() {

    val misses: StateFlow<List<com.nltimer.core.data.database.entity.IconSearchMissEntity>> =
        iconSearchMissDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAll() {
        viewModelScope.launch { iconSearchMissDao.clear() }
    }
}
