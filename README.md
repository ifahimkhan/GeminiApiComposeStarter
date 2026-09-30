# Gemini AI Assistant Application - N022 Submission

An Android application built with **Jetpack Compose**, **Material 3**, **Google Gemini AI Client**, **Android Keystore**, and **Room Persistence**.

---

## Technical Features

### 1. Keystore Security (`KeyStoreManager.kt` & `ApiKeyManager.kt`)
- Generates **AES-256-GCM** secret key inside `AndroidKeyStore`.
- Encrypts `BuildConfig.GEMINI_API_KEY` at rest and stores ciphertext and IV via `EncryptedSharedPreferences`.
- Key is decrypted in memory during runtime without exposing plaintext keys in logs or layouts.

### 2. Room Persistence & Preferences
- Room database schema (`ConversationMessage`, `ConversationDao`, `AppDatabase`).
- Exposes conversation items reactively as a `Flow<List<ConversationMessage>>`.
- User preferences managed via Preferences DataStore (`UserPreferencesRepository`).

### 3. Material 3 UI & Speech Recognition
- Customized Warm Coral & Slate Dark color scheme (`Color.kt`, `Theme.kt`).
- Interactive welcome screen with suggestion chips ("Summarize", "Code Help", "Ideas").
- Elevated Card message cards with model indicator badges.
- Voice-to-text input via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.

---

## Build & Test Instructions

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### UI Tests
```bash
./gradlew connectedCheck
```
