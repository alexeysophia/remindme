package com.familyvoice.reminders.domain.model

import com.google.firebase.Timestamp

data class Reminder(
    val id: String = "",
    val groupId: String = "",
    /** UID of the user who created the reminder. */
    val createdByUid: String = "",
    /** UID of the user the reminder is assigned to. */
    val assignedToUid: String = "",
    /** The task text extracted by Gemini. Mandatory — never null/blank. */
    val task: String = "",
    /** Optional deadline extracted by Gemini. */
    val deadline: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val done: Boolean = false,
)
