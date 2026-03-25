package com.familyvoice.reminders.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary          = RecordRed,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = RecordRedDark,
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkSurface2,
)

@Composable
fun FamilyVoiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = FamilyVoiceTypography,
        content     = content,
    )
}
