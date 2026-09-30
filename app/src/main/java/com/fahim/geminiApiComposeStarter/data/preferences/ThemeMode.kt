package com.fahim.geminiApiComposeStarter.data.preferences

/**
 * Type-safe representation of the user's theme preference.
 * Persisted using stable uppercase enum name strings ("SYSTEM", "LIGHT", "DARK").
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /**
         * Safely parses a persisted string value, defaulting to [SYSTEM] if null, empty, or unknown.
         */
        fun fromStorage(value: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}
