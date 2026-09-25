# Gemini API Compose Starter - Lab Assignment 1

An Android application built with **Kotlin**, **Jetpack Compose**, and **MVVM Architecture** integrating Google's **Gemini AI API** (`gemini-1.5-flash`).

---

## 1. API Key Setup

1. Copy `local.properties.example` to `local.properties` at the root of the project:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
2. For CI/CD environments where `local.properties` is omitted, set an environment variable:
   ```bash
   export GEMINI_API_KEY="your_actual_gemini_api_key_here"
   ```
   Gradle automatically falls back to `System.getenv("GEMINI_API_KEY")` when `local.properties` does not contain the key.

> **Note**: `local.properties` is listed in `.gitignore` and must **never** be committed to version control.

---

## 2. Keystore Encryption Flow End-to-End

To protect the API key on-device:

1. **Key Generation**:
   On the first launch, `SecureKeyStorage` checks `AndroidKeyStore`. If `GeminiApiKeyAlias` does not exist, an **AES-256-GCM** key is generated inside the hardware-backed `AndroidKeyStore` using `KeyGenParameterSpec`:
   - Block mode: `GCM`
   - Padding: `NoPadding`
   - Key size: `256 bits`

2. **Encryption**:
   The raw key from `BuildConfig.GEMINI_API_KEY` is encrypted with the hardware key using `Cipher.getInstance("AES/GCM/NoPadding")`.
   Only the **IV** and **ciphertext** (Base64-encoded) are persisted in `SharedPreferences`.

3. **In-Memory Decryption**:
   When `GeminiRepositoryImpl` needs to invoke `GenerativeModel.generateContent(prompt)`, `SecureKeyStorage.getDecryptedApiKey()` decrypts the key in memory using the stored IV and hardware key.
   - The decrypted API key is never logged, toasted, or displayed in the UI.
   - It is passed directly to `GenerativeModel` at call time.

4. **Code Obfuscation**:
   Release builds enable `isMinifyEnabled = true` and `isShrinkResources = true` with R8 rules configured in `app/proguard-rules.pro`.

---

## 3. Running Tests

### Unit Tests
Run unit tests for `ChatViewModel` and data layers using:
```bash
./gradlew testDebugUnitTest
```

### UI Tests
Run Jetpack Compose UI tests on an attached device or emulator:
```bash
./gradlew connectedAndroidTest
```

---

## 4. Features & Architecture

- **UI (Jetpack Compose & Material 3)**:
  - `LazyColumn` conversation flow with distinct user & Gemini chat bubbles.
  - Auto-scrolling to the latest message.
  - Adaptive layout with `BoxWithConstraints` supporting phones, tablets, and landscape orientation.
  - Material 3 theme supporting System Dark Theme and Android 12+ Dynamic Colors.
- **Voice Input**:
  - Voice-to-text prompt transcription via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and `rememberLauncherForActivityResult`.
- **Persistence**:
  - **Room Database**: Chat history persists across restarts in `chat_database`.
  - **Preferences DataStore**: User query preferences persisted in `user_preferences`.
- **State Management**:
  - Unidirectional Data Flow (UDF) with `StateFlow` and `collectAsStateWithLifecycle()`.

---

## 5. Production Notes (Backend Proxy & Security)

In a real production environment, direct mobile-to-Gemini API communication with a client-side API key carries inherent risks:

1. **Backend Proxy Pattern**:
   - Production apps should route AI requests through a secure backend (e.g., Firebase Cloud Functions, Google Cloud Run, or App Engine).
   - The backend securely manages secret keys in Google Secret Manager and handles rate limiting, user authentication, and input sanitization.

2. **Firebase App Check**:
   - Integrate **Firebase App Check** (with Play Integrity on Android) so that only genuine, untampered instances of your app can call your backend proxy.

3. **Restricted API Keys & Quotas**:
   - If direct API calls are necessary, restrict Google Cloud API keys by Android Package Name and SHA-1 fingerprint, and set strict daily usage quotas.
