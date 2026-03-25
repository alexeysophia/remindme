# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**FamilyVoiceReminders** — Android app for voice-first family task management.
Package: `com.familyvoice.reminders` | minSdk 26 | targetSdk 35

## Build commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (requires signing config)
./gradlew :app:assembleRelease

# Install on connected device
./gradlew :app:installDebug

# Run all tests
./gradlew :app:test

# Run a single test class
./gradlew :app:test --tests "com.familyvoice.reminders.SomeTest"

# Lint
./gradlew :app:lint
```

> **First-time setup**: The Gradle wrapper JAR is missing from the repo.
> Open the project in Android Studio — it will create `gradlew`, `gradlew.bat`,
> and `gradle/wrapper/gradle-wrapper.jar` automatically.

## Important constraints

- **No `applicationIdSuffix`** on any build type — Firebase `google-services.json` is registered for exactly `com.familyvoice.reminders`. Adding a suffix will break `processGoogleServices`.
- **No work on the main thread** in `Application` or `Activity.onCreate` — all I/O must be deferred to ViewModel coroutines or `LaunchedEffect`.
- `google-services.json` must be placed in `app/` and is git-ignored; it is never committed.

## Required manual setup (Firebase & Google Cloud)

See the "Firebase & GCloud Setup" section at the bottom of this file.

## Architecture

**Layer flow:** UI → ViewModel → (Repository — TBD) → Firebase / Gemini API

| Layer | Package | Responsibility |
|-------|---------|----------------|
| UI | `ui/main`, `ui/settings`, `ui/tile` | Jetpack Compose screens, no business logic |
| ViewModel | `ui/main/MainViewModel` | `StateFlow<MainUiState>`, maps gestures to state |
| Domain | `domain/model` | Pure Kotlin data classes (no Android deps) |
| DI | `di/AppModule` | Hilt singletons: `FirebaseAuth`, `FirebaseFirestore` |

**Key state machine** — `RecordingState` (`domain/model/RecordingState.kt`):
`Idle → Recording ↔ Paused → Processing → Idle`

**Record button gesture contract** (`ui/main/RecordButton.kt`):
- Press & hold → `onPressStart` (Recording)
- Lift finger (< 80 dp travel) → `onRelease` (Paused)
- Swipe UP (> 80 dp) → `onSwipeUp` (Processing → send to Gemini)
- Swipe DOWN (> 80 dp) → `onSwipeDown` (Idle — discard)

**Quick Settings Tile** (`ui/tile/RecordTileService.kt`): launches `MainActivity`
with `EXTRA_START_RECORDING = true` so recording begins from the lock screen.

**Startup performance**: no work on the main thread in `Application` or `Activity.onCreate`.
All heavy I/O must go into `ViewModel` coroutines (or `LaunchedEffect` in Compose).

## Firebase & GCloud Setup

Steps the developer must do **manually** in the consoles:

### Firebase Console (console.firebase.google.com)

1. **Create project** → "FamilyVoiceReminders"
2. **Add Android app** → package `com.familyvoice.reminders`
   - Download `google-services.json` → place in `app/`
3. **Authentication** → Sign-in method → enable **Google**
4. **Firestore Database** → Create database (production mode)
   - Apply security rules (see below)
5. **Firestore Security Rules**:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /groups/{groupId} {
         allow read, write: if request.auth.uid in resource.data.memberUids;
       }
       match /reminders/{reminderId} {
         allow read, write: if request.auth.uid in
           get(/databases/$(database)/documents/groups/$(resource.data.groupId)).data.memberUids;
       }
     }
   }
   ```

### Google Cloud Console (console.cloud.google.com)

1. Select the same project created by Firebase.
2. **APIs & Services** → Enable **Gemini API** (or Vertex AI Gemini).
3. **Credentials** → Create an **API Key** → restrict it to "Generative Language API".
4. The user enters this key in the app's Settings screen — it is stored in
   `EncryptedSharedPreferences` (never committed to source control or Firestore).

### SHA-1 for Google Sign-In

```bash
# Debug keystore (run from project root)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
Add the printed SHA-1 fingerprint to **Firebase → Project Settings → Your apps → SHA certificate fingerprints**.
