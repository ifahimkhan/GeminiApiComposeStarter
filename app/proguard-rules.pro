# Proguard / R8 rules for Gemini Chat app

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Generative AI SDK & Ktor classes
-dontwarn io.ktor.**
-dontwarn com.google.ai.client.generativeai.**
-keep class com.google.ai.client.generativeai.** { *; }
-keep class io.ktor.** { *; }

# Keep DataStore models
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
