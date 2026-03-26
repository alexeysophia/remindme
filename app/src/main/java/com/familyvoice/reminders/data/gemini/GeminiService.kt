package com.familyvoice.reminders.data.gemini

import android.util.Log
import com.familyvoice.reminders.data.preferences.UserPreferences
import com.familyvoice.reminders.domain.model.ReminderIntent
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeminiService"

private const val SYSTEM_PROMPT =
    "Ты — умный ассистент по извлечению данных для таск-трекера. " +
    "Твоя задача: прослушать аудио и извлечь 3 параметра в формате JSON: " +
    "task (Что сделать — обязательно, иначе null), " +
    "assignee (Кому сделать, в именительном падеже, если не указано — null), " +
    "deadline (Когда, если не указано — null). " +
    "Верни ТОЛЬКО валидный JSON вида: {\"task\": \"Купить молоко\", \"assignee\": \"Маша\", \"deadline\": \"Сегодня\"}."

/** DTO that mirrors the JSON structure Gemini returns. */
private data class GeminiJsonResponse(
    @SerializedName("task")     val task: String?,
    @SerializedName("assignee") val assignee: String?,
    @SerializedName("deadline") val deadline: String?,
)

/** Possible outcomes of a Gemini transcription request. */
sealed interface GeminiResult {
    data class Success(val intent: ReminderIntent) : GeminiResult
    data object NoApiKey : GeminiResult
    data class Failure(val message: String) : GeminiResult
}

@Singleton
class GeminiService @Inject constructor(
    private val userPreferences: UserPreferences,
) {
    private val gson = Gson()

    /**
     * Sends [audioFile] (m4a/mp4) to Gemini, parses the JSON response and
     * returns a [GeminiResult]. Logs everything under tag "GeminiResult".
     */
    suspend fun process(audioFile: File): GeminiResult {
        val apiKey = userPreferences.geminiApiKey.first()
        if (apiKey.isBlank()) {
            Log.w(TAG, "GeminiResult: no API key configured")
            return GeminiResult.NoApiKey
        }

        return try {
            val model = GenerativeModel(
                modelName         = "gemini-2.5-flash",
                apiKey            = apiKey,
                generationConfig  = generationConfig {
                    responseMimeType = "application/json"
                },
                systemInstruction = content { text(SYSTEM_PROMPT) },
            )

            val audioBytes = audioFile.readBytes()
            val response   = model.generateContent(
                content { blob("audio/mp4", audioBytes) },
            )

            val rawJson = response.text?.trim() ?: run {
                Log.e(TAG, "GeminiResult: empty response")
                return GeminiResult.Failure("Пустой ответ от Gemini")
            }
            Log.i(TAG, "GeminiResult (raw JSON): $rawJson")

            val intent = parseIntent(rawJson)
            when {
                intent.task == null ->
                    Log.e(TAG, "GeminiResult: задача не распознана — $rawJson")
                else ->
                    Log.i(TAG, "GeminiResult: task='${intent.task}' assignee='${intent.assignee}' deadline='${intent.deadline}'")
            }

            GeminiResult.Success(intent)
        } catch (e: Exception) {
            Log.e(TAG, "GeminiResult: error calling Gemini", e)
            GeminiResult.Failure(e.message ?: "Неизвестная ошибка")
        }
    }

    private fun parseIntent(json: String): ReminderIntent {
        return try {
            val dto = gson.fromJson(json, GeminiJsonResponse::class.java)
            ReminderIntent(
                task     = dto.task?.ifBlank { null },
                assignee = dto.assignee?.ifBlank { null },
                deadline = dto.deadline?.ifBlank { null },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini JSON: $json", e)
            ReminderIntent(task = null, assignee = null, deadline = null)
        }
    }
}
