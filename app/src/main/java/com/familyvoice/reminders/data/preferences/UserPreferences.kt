package com.familyvoice.reminders.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.GEMINI_API_KEY] ?: ""
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GEMINI_API_KEY] = key
        }
    }

    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }
}
