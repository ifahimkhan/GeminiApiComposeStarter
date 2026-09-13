# ProGuard / R8 Rules for GeminiApiComposeStarter

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase

# Keep Gemini Generative AI SDK classes
-keep class com.google.ai.client.generativeai.** { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }
