# ProGuard / R8 Configuration for GeminiApiComposeStarter

# Preserve source file names and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod,Signature,*Annotation*

# --------------------------------------------------
# Android Room Persistence
# --------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**

# --------------------------------------------------
# Google Generative AI SDK & Ktor Client
# --------------------------------------------------
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --------------------------------------------------
# Kotlinx Serialization & Coroutines
# --------------------------------------------------
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    <fields>;
}
-keepattributes *Annotation*, InnerClasses, Signature
-dontwarn kotlinx.serialization.**

# --------------------------------------------------
# Security & Cryptography
# --------------------------------------------------
-keep class com.fahim.geminiApiComposeStarter.data.security.** { *; }