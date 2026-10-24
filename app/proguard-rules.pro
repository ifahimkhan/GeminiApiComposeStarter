# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** { *; }

# Serialization and Reflection
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# AndroidX DataStore
-keep class androidx.datastore.** { *; }

# Android Keystore and Security
-keep class com.fahim.geminiApiComposeStarter.data.security.** { *; }
