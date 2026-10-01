# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Gemini AI Client
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# DataStore
-keep class androidx.datastore.** { *; }

# App Models and Entities
-keep class com.fahim.geminiApiComposeStarter.model.** { *; }
-keep class com.fahim.geminiApiComposeStarter.data.local.** { *; }