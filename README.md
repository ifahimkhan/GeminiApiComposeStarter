# Gemini Assistant — Jetpack Compose Starter Enhancement (Assignment 1)

An Android chat client powered by the **Google Gemini SDK**, built with **Jetpack Compose (Material 3)**, **Android Keystore AES-256-GCM encryption**, **Room**, and **Preferences DataStore**.

## Overview of Implemented Enhancements

1. **API Key Protection & Encryption at Rest**
   - Reads `GEMINI_API_KEY` from `local.properties` at build time with an automatic fallback to `System.getenv("GEMINI_API_KEY")` for CI/CD pipelines.
   - Generates a hardware-backed 256-bit AES-GCM key inside `AndroidKeyStore` (`CryptoKeyManager`) and encrypts the API key on first launch (`SecureApiKeyStore`).
   - Persists only the Base64-encoded ciphertext and initialization vector (IV) inside `EncryptedSharedPreferences`.
   - Decrypts the key strictly in-memory when constructing `GenerativeModel`, without logging or displaying the key.
   - Enables R8 code shrinking and obfuscation (`isMinifyEnabled = true`, `isShrinkResources = true`) on release builds.

2. **Jetpack Compose Material 3 UI & Adaptive Layout**
   - Displays chat history in a `LazyColumn` with stable item keys (`key = { item.id }`) and automatic smooth scrolling to the latest message.
   - Uses distinct Material 3 `Surface` bubbles for user prompts and Gemini responses, with bold markdown (`**...**`) rendering.
   - Adapts bubble maximum widths across compact phones and wider tablet/landscape layouts via `BoxWithConstraints`.
   - Provides a `CircularProgressIndicator` while awaiting responses and a `Snackbar` with a **Retry** action when network/API errors occur.
   - Supports dark theme and Material 3 color schemes in `Theme.kt`.

3. **Voice Input & Local Persistence**
   - Integrates speech-to-text input via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and `rememberLauncherForActivityResult`.
   - Stores conversation history in a local **Room** database (`ChatDatabase`, `MessageDao`, `MessageEntity`) so messages persist across app restarts.
   - Stores user configuration in **Preferences DataStore** (`UserPreferencesDataStore`).

4. **Unit & UI Testing**
   - `ChatViewModelTest`: Coroutine unit tests using `StandardTestDispatcher` and `FakeGeminiRepository`.
   - `ChatScreenTest`: Compose UI tests verifying placeholder rendering, message bubbles, and prompt input.

---

## Production Security Considerations

While encrypting the API key at rest with `AndroidKeyStore` (AES-256-GCM) and enabling R8 obfuscation prevents trivial static extraction from the APK or unencrypted disk storage, client-side secrets can still be inspected on rooted devices via runtime memory hooking (e.g., Frida).

In a production environment:
- **Backend Proxy / API Gateway**: Move Gemini API calls behind an authenticated backend service (such as Cloud Functions or Cloud Run) so the Gemini API key remains exclusively on server infrastructure.
- **Firebase App Check**: Pair backend endpoints or Vertex AI in Firebase with **Firebase App Check** (Play Integrity API) to verify that incoming requests originate from genuine, untampered instances of the app.

---

## Setup Instructions

1. Copy `local.properties.example` to `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
2. Open the project in Android Studio and sync Gradle.
3. Run the `app` configuration on an Android emulator or physical device (API 26+).

## Running Tests

- **Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Instrumented Compose UI Tests**:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```
