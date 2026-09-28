# Project specific ProGuard / R8 configuration rules.

# Preserve line numbers and source files for meaningful stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Domain Models and Entities
-keep class com.fahim.geminiApiComposeStarter.data.model.** { *; }
-keep class com.fahim.geminiApiComposeStarter.data.local.** { *; }
-keep class com.fahim.geminiApiComposeStarter.data.preferences.** { *; }
-keep class com.fahim.geminiApiComposeStarter.data.security.** { *; }

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class **_Impl { *; }

# Google Generative AI Client SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Android Keystore & Crypto
-keepclassmembers class java.security.KeyStore { *; }
-keepclassmembers class javax.crypto.Cipher { *; }