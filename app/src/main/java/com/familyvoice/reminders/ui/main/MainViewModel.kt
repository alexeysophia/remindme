package com.familyvoice.reminders.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyvoice.reminders.data.audio.VoiceRecorder
import com.familyvoice.reminders.data.gemini.GeminiResult
import com.familyvoice.reminders.data.gemini.GeminiService
import com.familyvoice.reminders.data.repository.ReminderRepository
import com.familyvoice.reminders.domain.model.RecordingState
import com.familyvoice.reminders.domain.model.ReminderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val geminiService: GeminiService,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Real-time list of all reminders from Firestore. */
    val reminders: StateFlow<List<ReminderItem>> = reminderRepository.allRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** One-shot Toast messages collected by the UI via LaunchedEffect. */
    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /** Press & Hold — start or resume recording. */
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

    /** Release (no swipe) → pause. */
    fun pauseRecording() {
        if (_uiState.value.recordingState != RecordingState.Recording) return
        voiceRecorder.pause()
        _uiState.update { it.copy(recordingState = RecordingState.Paused) }
    }

    /**
     * Swipe UP — stop recorder → send to Gemini → save to Firestore.
     * Shows Toast on success / error, deletes temp file on success.
     */
    fun finalizeRecording() {
        if (_uiState.value.recordingState == RecordingState.Idle) return
        _uiState.update { it.copy(recordingState = RecordingState.Processing) }

        val audioFile = voiceRecorder.stop()
        if (audioFile == null) {
            Log.w(TAG, "finalizeRecording: no audio file")
            _uiState.update { it.copy(recordingState = RecordingState.Idle) }
            return
        }

        viewModelScope.launch {
            when (val result = geminiService.process(audioFile)) {
                is GeminiResult.NoApiKey -> {
                    _toast.emit("Введите API ключ в настройках")
                }
                is GeminiResult.Failure -> {
                    _toast.emit("Ошибка Gemini: ${result.message}")
                }
                is GeminiResult.Success -> {
                    val intent = result.intent
                    if (intent.task == null) {
                        _toast.emit("Задача не распознана. Попробуйте снова.")
                    } else {
                        reminderRepository.saveReminder(intent)
                            .onSuccess {
                                _toast.emit("Напоминание создано!")
                                audioFile.delete()
                            }
                            .onFailure { e ->
                                _toast.emit("Ошибка сохранения: ${e.message}")
                            }
                    }
                }
            }
            _uiState.update { it.copy(recordingState = RecordingState.Idle) }
        }
    }

    /** Swipe DOWN — discard recording. */
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
