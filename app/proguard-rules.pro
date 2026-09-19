# proguard-rules.pro
-keepclassmembers class com.nmims.c010.gemini.data.local.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn org.slf4j.**
# Everything else (including our security package) is obfuscated by R8.
