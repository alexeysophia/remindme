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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyvoice.reminders.domain.model.RecordingState
import com.familyvoice.reminders.domain.model.Reminder

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

    // ── Audio permission ──────────────────────────────────────────────────────
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

    // ── Toast events ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.toast.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // ── Status label ──────────────────────────────────────────────────────────
    val statusText: String = uiState.displayMessage ?: when {
        !hasAudioPermission                                  -> "Нажмите кнопку, чтобы разрешить микрофон"
        uiState.recordingState == RecordingState.Idle        -> "Удерживайте кнопку для записи"
        uiState.recordingState == RecordingState.Recording   -> "Запись..."
        uiState.recordingState == RecordingState.Paused      -> "Пауза — свайп вверх для отправки"
        uiState.recordingState == RecordingState.Processing  -> "Отправка в Gemini..."
        else                                                 -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RemindMe", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
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

// ── "Все" tab ─────────────────────────────────────────────────────────────────

@Composable
private fun AllRemindersContent(reminders: List<Reminder>) {
    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text  = "Напоминаний пока нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(reminder)
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: Reminder) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // ── Task (prominent) ──────────────────────────────────────────────
            Text(
                text       = reminder.task,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis,
            )

            // ── Meta row (assignee + deadline) ────────────────────────────────
            val hasMeta = reminder.assignee != null || reminder.deadline != null
            if (hasMeta) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.2f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    reminder.assignee?.let { assignee ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = "Исполнитель",
                                modifier           = Modifier.size(15.dp),
                                tint               = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text  = assignee,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    reminder.deadline?.let { deadline ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.Schedule,
                                contentDescription = "Дедлайн",
                                modifier           = Modifier.size(15.dp),
                                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text  = deadline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderContent(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}
