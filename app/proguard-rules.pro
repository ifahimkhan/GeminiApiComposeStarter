# Add project specific ProGuard rules here.

# Keep Gemini SDK classes
-keep class com.google.ai.client.** { *; }

# Keep Room entities
-keep class com.n149.geminichat.data.MessageEntity { *; }

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class **_HiltComponents { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Keep DataStore generated serialisers
-keep class androidx.datastore.** { *; }

# Keep BuildConfig (needed at runtime, but key value is obfuscated by R8)
-keep class com.n149.geminichat.BuildConfig { *; }
