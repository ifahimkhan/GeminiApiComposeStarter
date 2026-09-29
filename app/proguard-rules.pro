# Project-specific ProGuard / R8 rules for GeminiApiComposeStarter

# Preserve line numbers and source file names for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room persistence: preserve Entity fields and DAO interface definitions for SQLite queries
-keep class com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity { *; }
-keep interface com.fahim.geminiApiComposeStarter.data.local.ChatDao { *; }

# User preferences: preserve ThemeMode enum entries and valueOf method for DataStore string persistence
-keep enum com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode { *; }