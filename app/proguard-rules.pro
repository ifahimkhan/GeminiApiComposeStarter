# Add project specific ProGuard rules here.
# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# DataStore
-keep class androidx.datastore.** { *; }

# Keep data models and Room entities
-keep class com.fahim.geminiApiComposeStarter.data.local.** { *; }
-keep class com.fahim.geminiApiComposeStarter.ui.chat.** { *; }
-keep class com.fahim.geminiApiComposeStarter.security.** { *; }