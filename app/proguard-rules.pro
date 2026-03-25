# Kotlin
-keepattributes *Annotation*
-keepclassmembers class * { @javax.inject.* *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Hilt — keep generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Domain models (Firestore serialisation)
-keep class com.familyvoice.reminders.domain.model.** { *; }
