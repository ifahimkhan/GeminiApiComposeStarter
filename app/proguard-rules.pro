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

# ---- Release build (isMinifyEnabled = true) ----
# Keep line numbers for crash reports but hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gemini SDK (uses Ktor + kotlinx.serialization internally). Keep its model classes and
# silence warnings about optional JVM-only classes that do not exist on Android.
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
