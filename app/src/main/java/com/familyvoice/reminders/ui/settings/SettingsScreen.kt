package com.familyvoice.reminders.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val savedApiKey by viewModel.geminiApiKey.collectAsState()

    // Local input state, initialised once from DataStore
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text  = "Gemini API Key",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value         = apiKeyInput,
                onValueChange = { input ->
                    apiKeyInput = input
                    viewModel.saveApiKey(input)   // auto-save on every change
                },
                modifier              = Modifier.fillMaxWidth(),
                label                 = { Text("Вставьте ключ из Google AI Studio") },
                singleLine            = true,
                visualTransformation  = if (apiKeyVisible) VisualTransformation.None
                                        else PasswordVisualTransformation(),
                keyboardOptions       = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon          = {
                    val icon = if (apiKeyVisible) Icons.Default.Visibility
                               else Icons.Default.VisibilityOff
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = if (apiKeyVisible) "Скрыть ключ" else "Показать ключ",
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Получить ключ: aistudio.google.com → Get API Key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
