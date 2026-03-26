package com.familyvoice.reminders.domain.model

/**
 * Structured result parsed from Gemini's JSON response.
 *
 * @property task     What to do — mandatory. Null means Gemini couldn't extract a task.
 * @property assignee Who should do it (nominative case). Null → self-assign.
 * @property deadline Optional deadline string as returned by Gemini.
 */
data class ReminderIntent(
    val task: String?,
    val assignee: String?,
    val deadline: String?,
)
