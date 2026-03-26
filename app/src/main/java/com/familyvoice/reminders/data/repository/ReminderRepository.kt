package com.familyvoice.reminders.data.repository

import android.util.Log
import com.familyvoice.reminders.domain.model.Reminder
import com.familyvoice.reminders.domain.model.ReminderIntent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG        = "ReminderRepository"
private const val COLLECTION = "reminders"

@Singleton
class ReminderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    /**
     * Real-time stream of all reminders ordered newest-first.
     * Uses [addSnapshotListener] — emits a new list on every Firestore change.
     */
    fun allRemindersFlow(): Flow<List<Reminder>> = callbackFlow {
        val listener = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot listener error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val task = doc.getString("task") ?: return@mapNotNull null
                        Reminder(
                            id        = doc.id,
                            task      = task,
                            assignee  = doc.getString("assignee"),
                            deadline  = doc.getString("deadline"),
                            creatorId = doc.getString("creatorId") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed doc ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Persists a parsed [ReminderIntent] to Firestore.
     * [createdAt] is stored as Unix epoch millis (Long) for easy ordering.
     */
    suspend fun saveReminder(intent: ReminderIntent): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        if (intent.task == null)
            return Result.failure(IllegalArgumentException("task is null"))

        return try {
            val data = hashMapOf(
                "task"      to intent.task,
                "assignee"  to intent.assignee,
                "deadline"  to intent.deadline,
                "creatorId" to uid,
                "createdAt" to System.currentTimeMillis(),
            )
            firestore.collection(COLLECTION).add(data).await()
            Log.i(TAG, "Reminder saved: ${intent.task}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save reminder", e)
            Result.failure(e)
        }
    }
}
