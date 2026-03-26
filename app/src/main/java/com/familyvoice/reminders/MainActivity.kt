package com.familyvoice.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familyvoice.reminders.ui.auth.AuthViewModel
import com.familyvoice.reminders.ui.auth.LoginScreen
import com.familyvoice.reminders.ui.main.MainScreen
import com.familyvoice.reminders.ui.settings.SettingsScreen
import com.familyvoice.reminders.ui.theme.FamilyVoiceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_RECORDING = "extra_start_recording"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FamilyVoiceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.state.collectAsState()

                    when {
                        // Still checking Firebase session
                        authState.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        // Not authenticated → show login
                        !authState.isAuthenticated -> {
                            LoginScreen(viewModel = authViewModel)
                        }

                        // Authenticated → show main navigation
                        else -> {
                            val navController = rememberNavController()
                            NavHost(navController = navController, startDestination = "main") {
                                composable("main") {
                                    MainScreen(
                                        onNavigateToSettings = { navController.navigate("settings") },
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(onBack = { navController.popBackStack() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // TODO: signal ViewModel to auto-start recording when launched from QS Tile
    }
}
