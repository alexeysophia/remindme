package com.familyvoice.reminders.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyvoice.reminders.data.audio.VoiceRecorder
import com.familyvoice.reminders.domain.model.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

enum class HomeTab { ON_ME, OUTGOING, ALL }

data class MainUiState(
    val selectedTab: HomeTab           = HomeTab.ON_ME,
    val recordingState: RecordingState = RecordingState.Idle,
    val displayMessage: String?        = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val voiceRecorder: VoiceRecorder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * Called on button press:
     * - [RecordingState.Idle]   → start a new recording
     * - [RecordingState.Paused] → resume the paused recording
     */
    fun startRecording() {
        val current = _uiState.value.recordingState
        if (current == RecordingState.Processing) return
        try {
            when (current) {
                RecordingState.Idle   -> voiceRecorder.start()
                RecordingState.Paused -> voiceRecorder.resume()
                else                  -> return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start/resume recording", e)
            return
        }
        _uiState.update { it.copy(recordingState = RecordingState.Recording, displayMessage = null) }
    }

    /** Called on button release (finger lift with no swipe). */
    fun pauseRecording() {
        if (_uiState.value.recordingState != RecordingState.Recording) return
        voiceRecorder.pause()
        _uiState.update { it.copy(recordingState = RecordingState.Paused) }
    }

    /** Called on swipe UP — stop and hand off to Gemini (TODO). */
    fun finalizeRecording() {
        if (_uiState.value.recordingState == RecordingState.Idle) return
        _uiState.update { it.copy(recordingState = RecordingState.Processing) }
        val file = voiceRecorder.stop()
        if (file == null) {
            Log.w(TAG, "finalizeRecording: no audio file — aborting")
            _uiState.update { it.copy(recordingState = RecordingState.Idle) }
            return
        }
        Log.i(TAG, "Готово к отправке в Gemini: ${file.absolutePath}")
        // TODO: send file to Gemini, parse result, create Reminder in Firestore
        // For now reset state so the user can record again
        _uiState.update { it.copy(recordingState = RecordingState.Idle) }
    }

    /** Called on swipe DOWN — discard the recording. */
    fun cancelRecording() {
        voiceRecorder.cancel()
        _uiState.update {
            it.copy(recordingState = RecordingState.Idle, displayMessage = "Запись удалена")
        }
        viewModelScope.launch {
            delay(2_000)
            _uiState.update { it.copy(displayMessage = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.cancel()
    }
}
