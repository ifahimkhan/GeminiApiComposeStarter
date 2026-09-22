# Gemini AI Assistant (Jetpack Compose Starter - Assignment 1)

A modern, persistent, secure, and responsive Android AI Chat application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **Preferences DataStore**, **Android Keystore**, and the **Google Generative AI SDK (Gemini)**.

---

## 📱 Features

- **Jetpack Compose & Material 3 UI**: Clean, modern AI chat interface with user/assistant chat bubbles, markdown bold text rendering, auto-scrolling, and empty-state prompt suggestions.
- **Persistent Chat History (Room Database)**: Stores conversation history across activity recreations and app restarts using asynchronous Room DB and Kotlin Flows.
- **User Preferences (Preferences DataStore)**: Manages theme preferences (System, Light, Dark) and UI settings.
- **Client-Side API Key Encryption (AES-256-GCM)**: Uses **Android Keystore** to generate a 256-bit AES key in hardware-backed storage, encrypts the API key, and persists ciphertext in DataStore. Decrypted only in-memory when constructing the Gemini client.
- **Voice Input (Speech-to-Text)**: Integrated microphone input powered by `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and `rememberLauncherForActivityResult`.
- **Responsive Layouts**: Supports portrait phones, landscape phones, and tablets using `WindowSizeClass` and dynamic max-width message containers.
- **Obfuscated Release Builds**: R8 minification and ProGuard rules configured for release builds without breaking reflection or serialization.
- **Unit & UI Test Suite**: Includes deterministic unit tests (`kotlinx-coroutines-test`) and Compose UI tests (`createComposeRule`).

---

## 🔑 Security & API Key Setup

### 1. Local Configuration (`local.properties`)
To keep your API key secure and out of version control:
1. Copy `local.properties.example` to `local.properties` in the project root directory.
2. Add your Gemini API key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
3. Gradle reads `GEMINI_API_KEY` from `local.properties` (or `GEMINI_API_KEY` environment variable) and exposes it via `BuildConfig.GEMINI_API_KEY`.

> [!IMPORTANT]
> `local.properties` is listed in `.gitignore` and must **NEVER** be committed to version control.

### 2. AES-256-GCM Encryption Architecture
- **Hardware Security**: On first run, `SecurityManager` generates an AES-256-GCM key inside the hardware-backed **Android Keystore** (`AndroidKeyStore`).
- **DataStore Storage**: The API key is encrypted using AES-256-GCM and stored only as ciphertext and initialization vector (IV) in **Preferences DataStore**.
- **In-Memory Decryption**: Plaintext key decryption occurs exclusively in volatile RAM when creating the `GenerativeModel` client.
- **Zero Exposure**: The API key is never logged, exposed in UI state, or saved in plaintext to disk.

> [!WARNING]
> **Production Security Limitations**: Client-side encryption protects keys from static reverse-engineering of committed files, but does **NOT** make an API key completely immune to a determined attacker with root/runtime memory access. For production applications, Gemini API calls should be placed behind a secure backend proxy server with API rate-limiting and Firebase App Check enforcement.

---

## 🏗️ Architecture Overview

The app follows Clean Architecture and unidirectional data flow (UDF):

```
UI (Compose Composables + Material 3)
         │
         ▼
ViewModel (ChatViewModel - StateFlow<ChatUiState>)
         │
 ┌───────┴──────────────────────────────┐
 ▼                                      ▼
Room DB (ChatHistory)      Preferences DataStore (Theme / Encrypted Key)
                                        │
                                        ▼
                           SecurityManager (Android Keystore)
                                        │
                                        ▼
                           Gemini Repository (GenerativeAI SDK)
```

---

## 🛠️ How to Build & Run

### Prerequisites
- JDK 17 or higher
- Android Studio Ladybug (or newer) / Android SDK 36

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build (Minified with R8)
```bash
./gradlew assembleRelease
```

---

## 🧪 Running Tests

### Unit Tests
Tests `ChatViewModel`, state flow transitions, fake repository responses, and Room operations:
```bash
./gradlew testDebugUnitTest
```

### Compose UI Tests
Runs UI component tests verifying chat bubbles, input field, and loading states:
```bash
./gradlew connectedAndroidTest
```

---

## 📄 License
Created for Mobile Application Development Assignment-1.
