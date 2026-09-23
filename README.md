# Gemini AI Assistant (Jetpack Compose)

**Mobile Application Development (Subject Code: 702AI0E002)**  
**School of Technology Management & Engineering, SVKM's NMIMS, Mumbai [2024-25]**  
**Lab Assignment 1**

- **Student Name**: Tanishq Vakte
- **Roll Number**: N112
- **Branch**: `N112_MADa1`
- **Repository**: [tanishqvakte-hui/N112_MAD_ASS1](https://github.com/tanishqvakte-hui/N112_MAD_ASS1)

---

## Overview
This application is an enhanced, production-grade Gemini conversational AI assistant built using Jetpack Compose, Clean Architecture, and Kotlin Coroutines. It features hardware-backed Android KeyStore AES-256-GCM encryption at rest, persistent SQLite chat history via Room, user preferences via Jetpack Preferences DataStore, native speech-to-text voice input, dynamic Material 3 design, and comprehensive automated test coverage.

---

## Core Features & Enhancements

### 1. Modern Material 3 Chat UI
- **LazyColumn with Stable Keys**: Renders user and Gemini chat bubbles using stable identifiers (`key = { it.id }`), preventing unnecessary recompositions.
- **Auto-Scrolling**: Automatically scrolls smoothly to the newest message whenever a response is streamed or generated.
- **Thinking Indicator Bubble**: Displays an animated "Gemini is thinking…" state with a circular progress indicator while waiting for the Gemini API.
- **Markdown & Code Formatting**: Beautifully renders bold text and structured markdown in AI responses.
- **Error Feedback**: Material 3 Snackbar with auto-dismiss and error card fallback upon network/server exceptions.
- **Conversation Management**: TopAppBar controls with active AI model badge (`gemini-3.6-flash`) and a confirmation dialog for conversation deletion.

### 2. Speech-to-Text Voice Input
- Integrated with Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `rememberLauncherForActivityResult`.
- Allows users to speak queries directly; recognized speech is transcribed into the prompt input bar.

### 3. Local Persistence & Personalization
- **Room Database (`gemini_chat_database`)**: Stores all conversations in an SQLite database using Room 2.7.0, ensuring chat history survives app closures, process death, and device reboots.
- **Preferences DataStore (`UserPreferencesRepository`)**: Persists user preferences and active model settings reactively via Kotlin Flows.

### 4. Responsive & Adaptive Layout
- Adapts to phones, tablets, and landscape orientations with `BoxWithConstraints` and responsive width constraints (`widthIn(max = 720.dp)`).

---

## Security Architecture & Encryption at Rest

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
   - Generated inside the hardware TEE / StrongBox using `KeyGenParameterSpec.Builder(KEY_ALIAS, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)`.
   - Raw API key is encrypted once on first startup. Only ciphertext and the 12-byte GCM initialization vector (IV) are persisted at rest.
   - Master key never leaves the Android KeyStore hardware boundary.
2. **In-Memory Decryption Only**:
   - Decrypted only at the exact instant `GenerativeModel` is instantiated. Never logged or exposed.
3. **CI Environment Variable Fallback**:
   - `app/build.gradle.kts` falls back to `System.getenv("GEMINI_API_KEY")` when running in headless CI environments.
4. **Release Build Obfuscation (R8)**:
   - Configured `isMinifyEnabled = true` and `isShrinkResources = true` in release builds with custom ProGuard rules.

---

## Limits of Client-Side Security & Production Architecture

> [!CAUTION]
> While hardware-backed KeyStore encryption and R8 obfuscation protect against static reverse-engineering, client-side secrets can still be vulnerable on rooted devices with dynamic memory inspection tools (Frida, Xposed).

**Production Best Practices**:
1. **Backend Proxy (BFF)**: Move Gemini API calls behind a secure backend service (Cloud Run, Firebase Cloud Functions). Mobile clients authenticate via OAuth/JWT.
2. **Firebase App Check**: Protect backend endpoints using Play Integrity API attestation.
3. **Restricted Keys**: Restrict Google AI Studio keys by Android package name and SHA-1 certificate fingerprint.

---

## Testing & Quality Assurance

- **Unit Tests**: Full unit test suite in `ChatViewModelTest.kt` using `kotlinx-coroutines-test` and `FakeGeminiRepository`. Tests verify state hoisting, input validation, voice transcription, Room database persistence across instances, and error handling.
- **Instrumented Tests**: Compose UI tests in `ChatScreenTest.kt` verifying composable rendering.

To execute unit tests:
```powershell
.\gradlew.bat testDebugUnitTest
```

To assemble an obfuscated release APK:
```powershell
.\gradlew.bat assembleRelease
```
