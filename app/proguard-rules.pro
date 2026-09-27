# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ---- Added for this assignment (release/minified builds) ----

# Kotlin data classes used as domain/UI models: keep member names so no runtime reflection
# (e.g. copy(), equals(), Compose's state snapshotting) breaks after obfuscation.
-keepclassmembers class com.fahim.geminiApiComposeStarter.data.ChatMessage { *; }
-keepclassmembers class com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState { *; }

# Room generates code at compile time; keep entities/DAO implementations intact.
-keep class com.fahim.geminiApiComposeStarter.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Google Generative AI SDK talks to a REST API and parses JSON with reflection-based
# serialization internally — keep its model classes so responses still deserialize correctly.
-keep class com.google.ai.client.generativeai.** { *; }
-keep class com.google.ai.client.generativeai.type.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Kotlinx Serialization / coroutines internals used transitively by the SDK.
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** { *; }

# Keep Kotlin Metadata so kotlin-reflect based libraries (Room, kotlinx.serialization) work.
-keep class kotlin.Metadata { *; }
