package com.nltimer.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.TagDisplayConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DialogConfigViewModel @Inject constructor(
    private val settingsPrefs: SettingsPrefs,
) : ViewModel() {

    val dialogConfig: StateFlow<DialogGridConfig> = settingsPrefs.getDialogConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DialogGridConfig())

    val homeLayoutConfig: StateFlow<HomeLayoutConfig> = settingsPrefs.getHomeLayoutConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLayoutConfig())

    val tagDisplayConfig: StateFlow<TagDisplayConfig> = settingsPrefs.getTagDisplayConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagDisplayConfig())

    fun updateConfig(config: DialogGridConfig) {
        viewModelScope.launch {
            settingsPrefs.updateDialogConfig(config)
        }
    }

    fun updateHomeLayoutConfig(config: HomeLayoutConfig) {
        viewModelScope.launch {
            settingsPrefs.updateHomeLayoutConfig(config)
        }
    }

    fun updateTagDisplayConfig(config: TagDisplayConfig) {
        viewModelScope.launch {
            settingsPrefs.updateTagDisplayConfig(config)
        }
    }
}
