# ProGuard rules for PITWALL (F1 Gemini Chatbot)

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Android Keystore & Crypto
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }