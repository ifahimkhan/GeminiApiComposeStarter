# Gemini AI Chat Application - N017 Submission

A modern Android application built with **Jetpack Compose**, **Material 3**, **Google Gemini AI Client**, **Android Keystore Security**, and **Room Persistence**.

---

## Technical Features

### 1. Keystore API Key Security (`KeyStoreManager.kt` & `ApiKeyManager.kt`)
- Key generated with **AES-256-GCM** inside `AndroidKeyStore`.
- Stores API key ciphertext and IV in `EncryptedSharedPreferences`.
- Key is decrypted dynamically in memory at runtime without logging.

### 2. Room Persistence & Settings
- Room database schema (`MessageEntity`, `ChatDao`, `AppDatabase`).
- Exposes historical messages asynchronously as a `Flow<List<MessageEntity>>`.
- User settings managed via Preferences DataStore (`UserPreferencesRepository`).

### 3. Material 3 UI & Speech Recognition
- Customized Teal & Indigo color palette (`Color.kt`, `Theme.kt`).
- Speech-to-Text input launcher with `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- Auto-scrolling message list with bold markdown parsing support.

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
