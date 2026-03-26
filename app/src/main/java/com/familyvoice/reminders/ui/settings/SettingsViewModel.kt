package com.familyvoice.reminders.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyvoice.reminders.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val geminiApiKey: StateFlow<String> = userPreferences.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveApiKey(key: String) {
        viewModelScope.launch { userPreferences.setGeminiApiKey(key) }
    }
}
