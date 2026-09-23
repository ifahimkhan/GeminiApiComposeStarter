# ==========================================
# GEMINI API COMPOSE STARTER - R8 RULES
# ==========================================

# Preserve annotation metadata
-keepattributes *Annotation*

# Preserve generic type signatures
-keepattributes Signature

# Preserve Kotlin metadata
-keep class kotlin.Metadata { *; }

# Preserve Room database entities and generated implementations
-keep class com.fahim.geminiApiComposeStarter.data.ChatEntity { *; }
-keep class com.fahim.geminiApiComposeStarter.data.ChatDatabase { *; }
-keep class com.fahim.geminiApiComposeStarter.data.ChatDao_Impl { *; }

# Preserve Room generated database implementation
-keep class com.fahim.geminiApiComposeStarter.data.ChatDatabase_Impl { *; }

# Preserve Gemini SDK classes
-keep class com.google.ai.client.generativeai.** { *; }

# Preserve encrypted API key storage
-keep class com.fahim.geminiApiComposeStarter.data.SecureApiKeyStorage { *; }