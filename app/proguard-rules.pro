# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ── Google Generative AI SDK ──────────────────────────────────────────────────
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ── DataStore ─────────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ── Coroutines ────────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ── Kotlin Serialization ──────────────────────────────────────────────────────
-dontwarn kotlinx.serialization.**

# ── Application Domain & AI Models ───────────────────────────────────────────
-keep class com.fahim.geminiApiComposeStarter.model.** { *; }
-keep class com.fahim.geminiApiComposeStarter.core.ai.** { *; }