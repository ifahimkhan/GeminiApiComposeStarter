# Proguard / R8 rules for GeminiApiComposeStarter

# Keep Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Keep BuildConfig fields
-keep class com.example.assignment1_c061.BuildConfig { *; }
