# Gemini Compose Starter App

A modern, secure, responsive, and resilient Android chat application built with **Jetpack Compose**, **Material 3**, **Google Generative AI SDK (Gemini 3.6 Flash)**, **Android Keystore AES-256-GCM encryption**, **Room database**, and **Preferences DataStore**.

---

## Features

- **Material 3 Chat UI**: Sleek conversation layout using `LazyColumn` with distinct user and Gemini chat bubbles, bold markdown formatting, and automatic scrolling to the latest message.
- **Hardware-Backed Key Encryption at Rest**: On first launch, the Gemini API key is encrypted using **AES-256-GCM** inside the **Android Keystore** (`KeyGenParameterSpec`) and persisted only as ciphertext in **Preferences DataStore**. The key is decrypted strictly in-memory at the moment `GenerativeModel` is instantiated, and never exposed in logs, toasts, or UI.
- **Persistent Chat History (Room)**: Conversation messages are persisted in a local **Room Database**, surviving app restarts and process recreation. Users can also clear conversation history at any time.
- **Speech-to-Text Input**: Input prompts via voice using `RecognizerIntent` launched with Compose's `rememberLauncherForActivityResult`.
- **Responsive Layout**: Adapts gracefully across compact phones, tablets, foldables, and landscape orientations with constrained max-width containers and responsive top app bar.
- **Dark Mode & Dynamic Color**: Full support for system dark theme and Android 12+ dynamic Material You color theming.
- **R8 / ProGuard Obfuscation**: Release builds enable code minification (`isMinifyEnabled = true`) with tailored keep rules protecting Google Generative AI, Room, and Coroutines.
- **Comprehensive Testing Suite**: Coroutine-based unit tests using `kotlinx-coroutines-test` and `FakeGeminiRepository`, alongside Compose UI instrumentation tests using `createComposeRule()`.

---

## Getting Started: API Key Setup

### 1. Obtain an API Key
Request a Gemini API key from [Google AI Studio](https://aistudio.google.com/).

### 2. Store the Key Locally
Open or create `local.properties` in the project root directory (this file is git-ignored):
```properties
GEMINI_API_KEY=your_actual_gemini_api_key_here
```
> [!IMPORTANT]
> Never commit `local.properties` or paste your raw API key into Kotlin source files, XML resources, or committed Gradle scripts. A template file [`local.properties.example`](local.properties.example) is provided for convenience.

### 3. CI/CD Environment Variable Fallback
For automated CI pipelines (e.g. GitHub Actions), Gradle automatically falls back to the `GEMINI_API_KEY` system environment variable if `local.properties` does not contain the key:
```kotlin
val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: "").trim()
```

---

## Security Architecture & Encryption Flow

### Encryption Flow (At Rest)
1. **Key Generation**: Upon first initialization, `KeystoreManager` generates an AES-256 key inside the hardware-backed `AndroidKeyStore` provider using `KeyGenParameterSpec`:
   - Algorithm: `AES`
   - Block Mode: `GCM`
   - Padding: `NoPadding`
   - Key Size: `256 bits`
2. **Encryption**: The raw API key is encrypted using AES/GCM/NoPadding. The generated Initialization Vector (IV) and ciphertext are Base64-encoded.
3. **Persistence**: Only the Base64 IV and ciphertext are saved into Preferences DataStore (`secure_gemini_prefs`). The plain API key is discarded.
4. **On-Demand Decryption**: At the exact moment `GenerativeModel` needs to execute a query, `SecureApiKeyStorage` reads the IV and ciphertext, queries `KeystoreManager` to decrypt the key in-memory, and immediately creates the model instance. The decrypted secret is never logged, toasted, or cached in persistent storage.

### Security Boundaries & Production Recommendations
> [!WARNING]
> **Client-Side Security Limitations**: While Android Keystore and R8 code obfuscation substantially increase the barrier against reverse engineering and static analysis, client-side secrets can never be 100% hidden from a determined attacker with physical root access, modified runtime environments, or memory instrumentation tools (e.g., Frida).

For enterprise and commercial production deployments, adhere to the following best practices:
1. **Backend-for-Frontend (BFF) Proxy**: Route all Gemini API requests through a secure backend proxy server (e.g., Cloud Functions, Cloud Run, or custom backend). The backend holds the Gemini API key securely in a secret manager (e.g., Google Cloud Secret Manager).
2. **Firebase App Check**: Protect your backend endpoints with Firebase App Check (Play Integrity on Android) to verify that incoming traffic originates exclusively from your genuine, unmodified app.
3. **Restricted API Keys & Quotas**: Configure Google Cloud API key restrictions to limit API usage to authorized IP addresses and set strict per-minute/per-day quotas.

---

## Project Structure

```
app/src/
├── androidTest/java/com/fahim/geminiApiComposeStarter/
│   └── ui/chat/ChatScreenTest.kt            # Compose UI tests with createComposeRule
├── main/java/com/fahim/geminiApiComposeStarter/
│   ├── MainActivity.kt                      # Main activity & dependency injection
│   ├── data/
│   │   ├── GeminiRepository.kt              # Repository abstraction
│   │   ├── GeminiRepositoryImpl.kt          # Implementation with on-demand key decryption
│   │   └── local/
│   │       ├── AppDatabase.kt               # Room Database
│   │       ├── ChatDao.kt                   # Room DAO with Flow streams
│   │       └── ChatMessageEntity.kt         # Room Entity for chat history
│   ├── security/
│   │   ├── KeystoreManager.kt               # Android Keystore AES-256-GCM crypto
│   │   └── SecureApiKeyStorage.kt           # DataStore encrypted key manager
│   ├── ui/
│   │   ├── chat/
│   │   │   ├── ChatScreen.kt                # Responsive LazyColumn Material 3 UI
│   │   │   ├── ChatUiState.kt               # Unidirectional StateFlow data models
│   │   │   └── ChatViewModel.kt             # State management, voice input, Room sync
│   │   ├── text/BoldMarkdown.kt             # Markdown parser for **bold** text
│   │   └── theme/                           # Material 3 Theme, Color, Typography
│   └── AndroidManifest.xml
└── test/java/com/fahim/geminiApiComposeStarter/
    └── ui/chat/
        ├── ChatViewModelTest.kt             # Unit tests for ChatViewModel
        ├── FakeGeminiRepository.kt          # Test double for GeminiRepository
        └── MainDispatcherRule.kt            # Coroutines test dispatcher rule
```

---

## Running Tests & Building

### 1. Run Unit Tests
Execute the unit tests on the JVM:
```bash
./gradlew test
```

### 2. Run Compose UI Instrumented Tests
Run Compose UI tests on an connected device or emulator:
```bash
./gradlew connectedAndroidTest
```

### 3. Assemble Debug APK
```bash
./gradlew assembleDebug
```

### 4. Assemble Release APK (with R8 Minification)
Verify that R8 obfuscation and ProGuard rules build cleanly:
```bash
./gradlew assembleRelease
```

---

## Submission Checklist

- [x] **No Secrets in History**: `local.properties` is in `.gitignore`; no API keys or plaintext credentials are committed.
- [x] **Example File**: `local.properties.example` provides a clear template.
- [x] **Keystore Encryption**: AES-256-GCM encryption with Android Keystore; decrypted only in memory.
- [x] **Room Persistence**: Chat history persists across restarts in Room.
- [x] **Voice Input**: Speech recognition via `RecognizerIntent`.
- [x] **Responsive Compose UI**: LazyColumn with Material 3 bubbles, loading indicator, and adaptive layout.
- [x] **Tests**: Unit tests with `kotlinx-coroutines-test` and UI tests with `createComposeRule()`.
- [x] **Release Obfuscation**: `isMinifyEnabled = true` with tested ProGuard rules.
