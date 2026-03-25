package com.familyvoice.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familyvoice.reminders.ui.main.MainScreen
import com.familyvoice.reminders.ui.settings.SettingsScreen
import com.familyvoice.reminders.ui.theme.FamilyVoiceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** Set by [RecordTileService] to auto-start recording on launch. */
        const val EXTRA_START_RECORDING = "extra_start_recording"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: let Compose own insets for maximum visual space
        enableEdgeToEdge()

        val startRecording = intent.getBooleanExtra(EXTRA_START_RECORDING, false)

        setContent {
            FamilyVoiceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen()
                            // TODO: if startRecording, trigger ViewModel.startRecording()
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handle re-launch from Quick Settings Tile while app is already alive
        if (intent.getBooleanExtra(EXTRA_START_RECORDING, false)) {
            // TODO: signal ViewModel to start recording
        }
    }
}
