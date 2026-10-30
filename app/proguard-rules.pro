# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# AndroidX DataStore Preferences
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.protobuf.** { *; }

# Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-keep class com.fahim.geminiApiComposeStarter.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Kotlin Serialization (used by Generative AI SDK)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialInfo *;
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class *$$serializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    *** Companion;
}
