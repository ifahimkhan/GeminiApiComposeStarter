# ProGuard / R8 rules for Gemini API Compose Starter
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.ai.client.generativeai.** *;
}
-dontwarn com.google.ai.client.generativeai.**

# Room Database ProGuard Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
