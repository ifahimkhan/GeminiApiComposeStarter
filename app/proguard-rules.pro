# Add project specific ProGuard rules here.

# Gemini SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable