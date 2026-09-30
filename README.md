# Gemini AI Chat Android Starter

A production-hardened Android AI chat application built using **Kotlin**, **Jetpack Compose**, **Room Database**, **DataStore Preferences**, and the **Google Gemini API**.

---

## Implemented Features

- **Gemini AI Conversations**: Multi-turn conversation chat powered by Google Gemini (`gemini-3.5-flash` with automatic fallback to `gemini-2.5-flash`).
- **Multi-Session Chat History**: Create new conversations, view chat history sessions, and switch between saved conversations stored in Room.
- **Voice Input**: Integrated Speech-to-Text (`RecognizerIntent`) for hands-free prompt composition.
- **Persistent Theme Preferences**: Persistent System/Light/Dark theme selector powered by Jetpack DataStore Preferences.
- **Hardware-Backed API Key Security**: Hardware-backed AES-256-GCM encryption in Android Keystore with ciphertext and IV saved in DataStore.
- **Responsive Layout**: Adapts dynamically across Compact, Medium, and Expanded screen sizes (window size classes) with permanent side-pane support on tablet/desktop displays.
- **Production Hardening**: Release code obfuscation and resource shrinking via R8.

---

## Local & CI Setup

### Local Setup
1. Copy `local.properties.example` to `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
2. Obtain a Gemini API key from [Google AI Studio](https://aistudio.google.com/).
3. In `local.properties`, set your API key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```

### CI / CD Setup
Set the `GEMINI_API_KEY` environment variable in your CI pipeline settings:
```bash
export GEMINI_API_KEY="your_ci_gemini_api_key"
```

---

## Encryption & Security Architecture

1. **Hardware-Backed Key Generation**: An AES-256 key is generated inside Android Keystore (`AndroidKeyStore` provider, alias `GeminiApiKeyAlias`) in a hardware-backed Trusted Execution Environment (TEE).
2. **First-Run Encryption**: On initial launch, `SecureApiKeyStorage` reads the configured API key from `BuildConfig.GEMINI_API_KEY`, encrypts it using `AES/GCM/NoPadding` with a secure random 12-byte IV, and stores the Base64-encoded ciphertext and IV into DataStore.
3. **On-Demand Decryption**: When executing Gemini API requests, the API key is decrypted in memory on an I/O background thread (`Dispatchers.IO`) and supplied to `GenerativeModel`.
4. **Zero Exposure**: Plaintext API keys are never logged, never stored in `ChatUiState`, and never exposed in UI exception messages.

---

## Running Tests

### Unit Tests
Run local JVM unit tests (ViewModel, repository fake, coroutine dispatcher rule):
```bash
./gradlew testDebugUnitTest
```

### Instrumentation / UI Tests
Run Compose UI tests on an connected Android device or emulator:
```bash
./gradlew connectedDebugAndroidTest
```

---

## Production Security Limits & Recommendations

- **Client-Side Storage Boundary**: Storing API keys within native mobile client binaries or local storage carries inherent reverse-engineering risks. On-device Android Keystore AES-256-GCM encryption provides maximum protection for client-side storage.
- **Production Best Practice**: For production applications, proxy all AI API requests through an authenticated backend server or Firebase AI Logic with App Check, restricting API key usage by Android package name and SHA-1 fingerprint.
- **SDK Deprecation Notice**: Note that the legacy `com.google.ai.client.generativeai` client SDK is scheduled for future deprecation; production backends should transition to Google Gen AI REST / gRPC endpoints.
