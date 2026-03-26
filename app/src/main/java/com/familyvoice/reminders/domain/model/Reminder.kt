package com.familyvoice.reminders.domain.model

/**
 * Core domain model for a voice reminder stored in Firestore.
 *
 * @property id         Firestore document ID (empty before insertion).
 * @property task       What to do — mandatory, never blank.
 * @property assignee   Who should do it (nominative case). Null → self-assigned.
 * @property deadline   Optional deadline string as returned by Gemini.
 * @property creatorId  Firebase UID of the user who recorded the audio.
 * @property createdAt  Unix epoch milliseconds — used for ordering.
 */
data class Reminder(
    val id: String        = "",
    val task: String      = "",
    val assignee: String? = null,
    val deadline: String? = null,
    val creatorId: String = "",
    val createdAt: Long   = 0L,
)
