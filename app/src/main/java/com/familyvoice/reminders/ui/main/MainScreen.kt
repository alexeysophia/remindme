package com.familyvoice.reminders.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyvoice.reminders.R
import com.familyvoice.reminders.domain.model.RecordingState

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages via snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData  = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            // ── Top: 3 tabs ─────────────────────────────────────────────────
            HomeTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab,
            )

            // ── Middle: tab content ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                when (uiState.selectedTab) {
                    HomeTab.ON_ME    -> OnMeContent()
                    HomeTab.OUTGOING -> OutgoingContent()
                    HomeTab.ALL      -> AllContent()
                }
            }

            // ── Gesture hint text ────────────────────────────────────────────
            GestureHint(state = uiState.recordingState)

            // ── Bottom: giant record button ──────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                RecordButton(
                    recordingState = uiState.recordingState,
                    onPressStart   = viewModel::startRecording,
                    onRelease      = viewModel::pauseRecording,
                    onSwipeUp      = viewModel::finalizeRecording,
                    onSwipeDown    = viewModel::cancelRecording,
                    size           = 100.dp,
                )
            }
        }
    }
}

// ── Tab row ──────────────────────────────────────────────────────────────────

@Composable
private fun HomeTabRow(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    val tabs = listOf(
        HomeTab.ON_ME    to "На мне",
        HomeTab.OUTGOING to "Исходящие",
        HomeTab.ALL      to "Все",
    )
    TabRow(selectedTabIndex = selectedTab.ordinal) {
        tabs.forEachIndexed { index, (tab, label) ->
            Tab(
                selected = selectedTab.ordinal == index,
                onClick  = { onTabSelected(tab) },
                text     = { Text(label) },
            )
        }
    }
}

// ── Tab content placeholders ──────────────────────────────────────────────────

@Composable
private fun OnMeContent() {
    PlaceholderContent("Напоминания на мне")
}

@Composable
private fun OutgoingContent() {
    PlaceholderContent("Исходящие напоминания")
}

@Composable
private fun AllContent() {
    PlaceholderContent("Все напоминания группы")
}

@Composable
private fun PlaceholderContent(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Gesture hint ─────────────────────────────────────────────────────────────

@Composable
private fun GestureHint(state: RecordingState) {
    val hint = when (state) {
        RecordingState.Idle       -> stringResource(R.string.hint_hold_to_record)
        RecordingState.Recording  -> stringResource(R.string.hint_swipe_up_to_send)
        RecordingState.Paused     -> stringResource(R.string.hint_swipe_up_to_send)
        RecordingState.Processing -> "Обрабатываю…"
    }
    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.padding(bottom = 12.dp).then(Modifier.fillMaxSize().run { this }),
        ) {
            Text(
                text  = hint,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                ),
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
