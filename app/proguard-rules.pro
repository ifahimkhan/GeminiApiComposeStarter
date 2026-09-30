# Keep annotation / generic info needed by kotlinx.serialization (used by the Gemini SDK).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Gemini SDK models are (de)serialized reflectively.
-keep class com.google.ai.client.generativeai.** { *; }

# Ktor (pulled in by the Gemini SDK) references optional JVM-only classes.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**

# Strip debug/verbose logs from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
