package com.familyvoice.reminders.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyvoice.reminders.domain.model.RecordingState
import com.familyvoice.reminders.domain.model.ReminderItem

private val TABS = listOf(
    HomeTab.ON_ME    to "На мне",
    HomeTab.OUTGOING to "Исходящие",
    HomeTab.ALL      to "Все",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context   = LocalContext.current
    val uiState   by viewModel.uiState.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    // ── Permission state ──────────────────────────────────────────────────────
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasAudioPermission = granted }

    // ── Toast events from ViewModel ───────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.toast.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // ── Status label ──────────────────────────────────────────────────────────
    val statusText: String = uiState.displayMessage ?: when {
        !hasAudioPermission                                           -> "Нажмите кнопку, чтобы разрешить микрофон"
        uiState.recordingState == RecordingState.Idle                -> "Удерживайте кнопку для записи"
        uiState.recordingState == RecordingState.Recording           -> "Запись..."
        uiState.recordingState == RecordingState.Paused              -> "Пауза — свайп вверх для отправки"
        uiState.recordingState == RecordingState.Processing          -> "Отправка в Gemini..."
        else                                                         -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "RemindMe", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                TABS.forEachIndexed { index, (tab, label) ->
                    Tab(
                        selected = uiState.selectedTab.ordinal == index,
                        onClick  = { viewModel.selectTab(tab) },
                        text     = { Text(label) },
                    )
                }
            }

            // ── Tab content ───────────────────────────────────────────────────
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (uiState.selectedTab) {
                    HomeTab.ON_ME    -> PlaceholderContent("На мне")
                    HomeTab.OUTGOING -> PlaceholderContent("Исходящие")
                    HomeTab.ALL      -> AllRemindersContent(reminders)
                }
            }

            // ── Status label ──────────────────────────────────────────────────
            Crossfade(targetState = statusText, label = "status") { text ->
                Text(
                    text  = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Giant record button ───────────────────────────────────────────
            RecordButton(
                recordingState = uiState.recordingState,
                onPressStart   = {
                    if (hasAudioPermission) viewModel.startRecording()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onRelease      = viewModel::pauseRecording,
                onSwipeUp      = viewModel::finalizeRecording,
                onSwipeDown    = viewModel::cancelRecording,
                size           = 120.dp,
            )

            Spacer(modifier = Modifier.navigationBarsPadding().height(48.dp))
        }
    }
}

// ── "Все" tab: real-time LazyColumn ──────────────────────────────────────────

@Composable
private fun AllRemindersContent(reminders: List<ReminderItem>) {
    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text  = "Нет напоминаний",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(reminder)
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: ReminderItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text     = reminder.task,
                style    = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (reminder.assignee != null || reminder.deadline != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    reminder.assignee?.let {
                        Text(
                            text  = "→ $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    reminder.deadline?.let {
                        Text(
                            text  = "⏰ $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderContent(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}
