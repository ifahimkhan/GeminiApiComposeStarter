# Project-specific ProGuard / R8 rules for GeminiApiComposeStarter

# Preserve line numbers and source file names for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room persistence: preserve RoomDatabase implementations, Entity fields, and DAO definitions
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class **_Impl extends androidx.room.RoomDatabase {
    <init>();
}
-keep class com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity { *; }
-keep interface com.fahim.geminiApiComposeStarter.data.local.ChatDao { *; }
-keep class * implements com.fahim.geminiApiComposeStarter.data.local.ChatDao {
    <init>(...);
}

# User preferences: preserve ThemeMode enum entries and valueOf method for DataStore string persistence
-keep enum com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode { *; }

# Chat UI models used in StateFlow and Room mappings
-keep class com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage { *; }
-keep enum com.fahim.geminiApiComposeStarter.ui.chat.Participant { *; }