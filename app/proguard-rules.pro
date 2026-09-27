# R8 rules for the release build (isMinifyEnabled = true).

# The Gemini SDK (Ktor + kotlinx.serialization) references optional SLF4J classes.
-dontwarn org.slf4j.**

# Keep the Gemini SDK's serializable request/response models intact.
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Hide original source file names in stack traces.
-renamesourcefileattribute SourceFile
