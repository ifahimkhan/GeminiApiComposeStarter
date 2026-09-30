# Gemini AI Compose Chat Application - Lab Assignment 1

An Android application built with **Jetpack Compose**, **Material 3**, **Google Gemini AI Client**, **Android Keystore**, and **Room Persistence**.

---

## Features Implemented

### 1. API Key Encryption at Rest (Android Keystore Security)
- **Key Store Manager (`KeyStoreManager.kt`)**: Automatically generates an **AES-256-GCM** key pair inside the hardware-backed `AndroidKeyStore`.
- **Encrypted Preferences (`ApiKeyManager.kt`)**: Encrypts `BuildConfig.GEMINI_API_KEY` at first run and stores the Base64 ciphertext and IV using `EncryptedSharedPreferences`.
- **In-Memory Key Retrieval**: Decrypts the API key strictly in-memory when initializing `GenerativeModel`. The plaintext key is never logged or exposed in UI views.

### 2. Room Persistence & DataStore
- **Room Database (`AppDatabase.kt`, `ChatDao.kt`, `ChatMessageEntity.kt`)**: Persists chat history across app restarts.
- **Asynchronous Flow**: Exposes database messages as a reactive `Flow<List<ChatMessageEntity>>`.
- **Preferences DataStore (`UserPreferencesRepository.kt`)**: Manages app preferences such as model selection and voice feature toggles.

### 3. Material 3 UI / UX Overhaul & Voice Input
- **Distinct Chat Bubbles**: Modern Material 3 chat interface separating User (`primaryContainer`) and AI Assistant (`secondaryContainer`) messages.
- **Markdown & Bold Formatting**: Custom text formatting rendering markdown bold tags (`**text**`).
- **LazyColumn & Auto-Scroll**: Uses `LazyColumn` with stable keys (`key = { message.id }`) and automatically scrolls to the newest message upon receipt.
- **Speech-to-Text Voice Input**: Integrates `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `rememberLauncherForActivityResult` for voice-to-text input.
- **Feedback & Error Recovery**: Displays progress indicators during API calls and interactive `SnackbarHost` retry options for errors.

### 4. Unit Testing
- Comprehensive unit tests in `ChatViewModelTest.kt` using `kotlinx-coroutines-test` and `FakeGeminiRepository`.
- Android UI tests in `ChatScreenTest.kt` validating Jetpack Compose UI elements.

---

## Security Architecture & Reverse-Engineering Limitations

### Hardware-Backed AES-256-GCM Encryption Flow
1. On app initial launch, `ApiKeyManager` checks for stored ciphertext.
2. If absent, `KeyStoreManager` creates a 256-bit AES key with `GCM` block mode and `NoPadding` inside `AndroidKeyStore`.
3. The API key is encrypted and saved alongside its initialisation vector (IV) in `EncryptedSharedPreferences`.
4. During Gemini API execution, the key is decrypted in memory and passed directly to `GenerativeModel`.

### Client-Side Key Limitations (Reverse-Engineering Analysis)
- While storing keys in `AndroidKeyStore` prevents unauthorized static extraction from plain application storage or `shared_prefs`, **client-side secrets on untrusted user devices remain inherently vulnerable**:
  - Memory dump / dynamic analysis tools (e.g., Frida, Xposed) hooked at runtime can intercept decrypted strings in RAM.
  - Decompilation of APK binaries can analyze static fallbacks if hardcoded.
- **Production Recommendation**: API keys should be hosted on a secure backend server/proxy (e.g., Firebase Functions or custom API gateway) that authenticates users prior to proxying requests to Google AI Studio APIs.

---

## Setup & Build Instructions

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or higher
- Android SDK 36

### Configuration
1. Clone repository and switch to your student branch:
   ```bash
   git checkout N014_assignment
   ```
2. Create or configure `local.properties` in project root:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
3. Sync and build the project in Android Studio.

---

## Test Execution Commands

Run local unit tests via Gradle:
```bash
./gradlew testDebugUnitTest
```

Run Android Compose UI tests on an connected device/emulator:
```bash
./gradlew connectedCheck
```
