package com.familyvoice.reminders.domain.model

/** Lightweight display model read from Firestore. */
data class ReminderItem(
    val id: String       = "",
    val task: String     = "",
    val assignee: String? = null,
    val deadline: String? = null,
    val creatorId: String = "",
)
