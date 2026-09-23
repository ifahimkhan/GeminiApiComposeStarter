# ProGuard and R8 rules for GeminiApiComposeStarter

# Keep BuildConfig fields including encrypted keys / constants
-keepclassmembers class com.rannbir.geminiApiComposeStarter.BuildConfig {
    public static final java.lang.String GEMINI_API_KEY;
}

# Google Generative AI SDK rules
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Serialization and Data classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod