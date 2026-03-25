package com.familyvoice.reminders

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 * @HiltAndroidApp triggers Hilt's code generation — no other code needed here.
 * Keep this class lean; heavy initialisation belongs in ViewModel coroutines
 * so it never blocks the main thread at startup.
 */
@HiltAndroidApp
class FamilyVoiceApp : Application()
