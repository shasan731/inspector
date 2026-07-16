package com.shahriarhasan.usedphoneinspector.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.datastore.AppSettings
import com.shahriarhasan.usedphoneinspector.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )
}

