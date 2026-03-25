package com.familyvoice.reminders.domain.model

/** Lifecycle of a single voice recording session. */
sealed interface RecordingState {
    /** No active recording — button is at rest. */
    data object Idle : RecordingState

    /** Finger is down; microphone is actively capturing audio. */
    data object Recording : RecordingState

    /** Finger lifted without swipe — audio is buffered, ready to resume. */
    data object Paused : RecordingState

    /** Audio sent to Gemini; waiting for the response. */
    data object Processing : RecordingState
}
