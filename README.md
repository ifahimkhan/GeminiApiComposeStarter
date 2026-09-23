# Gemini Chat Studio (Jetpack Compose)

**Mobile Application Development (Subject Code: 702AI0E002)**  
**School of Technology Management & Engineering, SVKM's NMIMS, Mumbai [2024-25]**  
**Lab Assignment 1**

- **Student Name**: Cheshta Modi
- **Roll Number**: N065
- **Branch**: `N065_MADA1`
- **Repository**: [cheshtamodi12/N065_MADA1](https://github.com/cheshtamodi12/N065_MADA1)

---

## Project Overview
Gemini Chat Studio is an enterprise-grade, conversational mobile AI assistant built using Jetpack Compose and Kotlin Coroutines. It demonstrates modern Android architectural patterns (Clean Architecture & MVI), hardware-backed API key security, local database persistence, and multi-modal voice input.

---

## Key Highlights & Features

### 1. Modern Jetpack Compose UI
- **LazyColumn with Stable Keys**: Conversation messages are rendered in a performant list using unique UUID stable keys (`key = { it.id }`) to prevent unnecessary recomposition cycles.
- **Auto-Scroll Behavior**: Reactive `LaunchedEffect` automatically animates the scroll position to the bottom on new incoming messages or thinking states.
- **Animated Thinking Indicator**: Renders a dedicated "Gemini is thinking…" status bubble with a `CircularProgressIndicator` during asynchronous inference.
- **Adaptive Layout**: Adapts cleanly to different screen form factors (phones, foldables, tablets, landscape) via responsive constraints.
- **Error Feedback**: Graceful error handling using Material 3 `SnackbarHost` and error card bubbles.

### 2. Speech-to-Text Voice Input
- Built-in speech recognition using Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and Compose's `rememberLauncherForActivityResult`.
- Transcribes spoken audio into prompt queries with runtime permission handling.

### 3. Local Persistence & State Management
- **Room SQLite Database (`gemini_chat_database`)**: Automatically caches conversation records (`ChatMessageEntity`) using Room 2.7.0, ensuring chat history survives app closures, process death, and phone restarts.
- **Preferences DataStore (`UserPreferencesRepository`)**: Persists user settings and model configuration (`gemini-3.6-flash`) using asynchronous Kotlin Flows.

---

## Security Architecture & KeyStore Encryption at Rest

```
┌────────────────────────────────────────────────────────┐
│                   local.properties                     │
│         (Git-ignored, build-time compilation)          │
└───────────────────────────┬────────────────────────────┘
                            │ BuildConfig.GEMINI_API_KEY
┌───────────────────────────▼────────────────────────────┐
│                    On First Launch                     │
│       KeyStoreManager.kt (AndroidKeyStore Provider)    │
│            AES-256-GCM Hardware-Backed Master Key      │
└───────────────────────────┬────────────────────────────┘
                            │ Encrypt Key + 12-byte IV
┌───────────────────────────▼────────────────────────────┐
│              EncryptedSharedPreferences                │
│             (Persisted at rest as ciphertext)          │
└───────────────────────────┬────────────────────────────┘
                            │ Decrypt strictly in-memory
┌───────────────────────────▼────────────────────────────┐
│                 GeminiRepositoryImpl                   │
│         GenerativeModel("gemini-3.6-flash", apiKey)    │
│      (Zero logs, zero toasts, never exposed to UI)     │
└────────────────────────────────────────────────────────┘
```

1. **Hardware-Backed Android KeyStore (AES-256-GCM)**:
   - The master key is generated inside the hardware TEE / StrongBox via `KeyGenParameterSpec` with `AndroidKeyStore` provider.
   - Raw API keys are encrypted at rest using AES-256-GCM with a unique 12-byte initialization vector (IV) and persisted in `EncryptedSharedPreferences`.
   - The master secret key never leaves the hardware boundary.
2. **In-Memory Decryption Only**:
   - The API key is decrypted strictly into volatile memory at the moment `GenerativeModel` is instantiated, and never written to logs or displayed in the UI.
3. **CI Environment Fallback**:
   - `app/build.gradle.kts` falls back to `System.getenv("GEMINI_API_KEY")` when running in headless CI environments.
4. **R8 Release Obfuscation**:
   - Configured `isMinifyEnabled = true` and `isShrinkResources = true` with custom ProGuard rules.

---

## Production Security Considerations

> [!CAUTION]
> While hardware-backed KeyStore encryption and R8 obfuscation protect against static reverse-engineering, client-side secrets can still be extracted by an attacker with physical access to a rooted device using dynamic memory inspection tools (Frida, Xposed).

**Recommended Production Architecture**:
1. **Backend Proxy (BFF)**: Route all Gemini API requests through a secure backend proxy (Cloud Run, Firebase Cloud Functions). Clients authenticate with OAuth 2.0 / Firebase Auth.
2. **Firebase App Check**: Protect backend endpoints with Play Integrity API device attestation.
3. **API Key Restrictions**: Restrict Google AI Studio keys by Android package name and SHA-1 signing certificate fingerprint.

---

## Testing & Quality Assurance

- **Unit Tests**: Full unit test coverage in `ChatViewModelTest.kt` using `kotlinx-coroutines-test` and `FakeGeminiRepository` (verifies state hoisting, input validation, Room database persistence, and error handling).
- **Instrumented Tests**: Compose UI tests in `ChatScreenTest.kt` verifying composable rendering and interaction.

Run unit tests:
```powershell
.\gradlew.bat testDebugUnitTest
```

Assemble obfuscated release APK:
```powershell
.\gradlew.bat assembleRelease
```
