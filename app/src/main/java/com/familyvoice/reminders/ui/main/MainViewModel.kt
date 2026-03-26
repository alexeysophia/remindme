package com.familyvoice.reminders.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyvoice.reminders.domain.model.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab { ON_ME, OUTGOING, ALL }

data class MainUiState(
    val selectedTab: HomeTab           = HomeTab.ON_ME,
    val recordingState: RecordingState = RecordingState.Idle,
    /** Transient override for the status label (e.g. "Запись удалена"). */
    val displayMessage: String?        = null,
)

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun startRecording() {
        if (_uiState.value.recordingState == RecordingState.Processing) return
        _uiState.update { it.copy(recordingState = RecordingState.Recording, displayMessage = null) }
        // TODO: start AudioRecord capture
    }

    fun pauseRecording() {
        if (_uiState.value.recordingState != RecordingState.Recording) return
        _uiState.update { it.copy(recordingState = RecordingState.Paused) }
        // TODO: flush audio chunk to temp file
    }

    fun finalizeRecording() {
        if (_uiState.value.recordingState == RecordingState.Idle) return
        _uiState.update { it.copy(recordingState = RecordingState.Processing) }
        // TODO: send audio buffer to Gemini
    }

    fun cancelRecording() {
        _uiState.update {
            it.copy(recordingState = RecordingState.Idle, displayMessage = "Запись удалена")
        }
        viewModelScope.launch {
            delay(2_000)
            _uiState.update { it.copy(displayMessage = null) }
        }
        // TODO: delete temp audio file
    }
}
