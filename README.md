# Gemini AI Jetpack Compose Assistant

**Mobile Application Development (Subject Code: 702AI0E002)**  
**School of Technology Management & Engineering, SVKM\'s NMIMS, Mumbai [2024-25]**  
**Lab Assignment 1**

- **Student Name**: Shivang Tyagi
- **Roll Number**: N127
- **Branch**: `N127-MADa1`
- **Repository**: [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## Table of Contents
1. [Overview](#overview)
2. [Key Features](#key-features)
3. [Architecture & Design](#architecture--design)
4. [API Key Security & Encryption at Rest](#api-key-security--encryption-at-rest)
5. [Limits of Client-Side Security & Production Best Practices](#limits-of-client-side-security--production-best-practices)
6. [Setup and Configuration](#setup-and-configuration)
7. [Running the App & Tests](#running-the-app--tests)

---

## Overview
This application builds upon and significantly elevates the Gemini Jetpack Compose starter application. It provides an intelligent, multi-turn conversational AI assistant powered by Google\'s Gemini model, featuring a modern Material 3 user interface, hardware-backed Keystore AES-256-GCM encryption at rest, local persistence via Room database and Preferences DataStore, voice input, and responsive design for diverse form factors.

---

## Key Features

### 1. Modern Material 3 Chat Interface
- **LazyColumn with Stable Keys**: Renders user and AI messages in distinct chat bubbles with unique IDs as stable keys (`key = { it.id }`), preventing unnecessary recompositions.
- **Auto-Scroll**: Automatically scrolls to the newest message whenever a response is streamed or submitted using `rememberLazyListState()` and reactive `LaunchedEffect`.
- **Markdown & Formatting**: Gemini responses support bold markdown styling via `toBoldAnnotatedString()`.
- **Animated Loading State**: Smooth typing indicator ("Gemini is thinking…") with a circular progress indicator while awaiting API responses.
- **Error Feedback**: Material 3 Snackbar with auto-dismiss and inline retry feedback on network or API failures.
- **Top App Bar with Controls**: Displays the active AI model badge and a "Clear Chat" action with a confirmation dialog.

### 2. Speech-to-Text (Voice Input)
- Users can input prompts via speech using Android\'s native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- Launched cleanly via Compose\'s `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- Voice transcript is automatically recognized and appended to the prompt input field.

### 3. Local Persistence & Personalization
- **Room Database**: All conversation history (`ChatMessageEntity`) is stored locally in an encrypted Room SQLite database (`gemini_chat_database`), allowing chat sessions to survive app closures, process death, and device restarts.
- **Preferences DataStore**: Stores user preferences (e.g., active model `gemini-3.6-flash`, user preferences) reactively using Kotlin Flows.

### 4. Responsive & Adaptive Layout
- Adapts seamlessly to portrait, landscape, foldables, and large tablet screens using `BoxWithConstraints` and responsive width constraints (`widthIn(max = 720.dp)`), ensuring comfortable readability across all device types.

### 5. Dark Theme Support
- Full dark mode support using `isSystemInDarkTheme()` and Material 3 dynamic color schemes (`dynamicDarkColorScheme` / `dynamicLightColorScheme`).

---

## Architecture & Design

The application follows the recommended **Clean Architecture & MVVM** pattern:

```
                  ┌────────────────────────────────────────────────┐
                  │                 Jetpack Compose UI             │
                  │   (ChatScreen, ChatMessageBubble, PromptBar)   │
                  └───────────────────────▲────────────────────────┘
                                          │ StateFlow<ChatUiState>
                                          │ UI Events (onSend, onVoiceInput, etc.)
                  ┌───────────────────────┴────────────────────────┐
                  │                  ChatViewModel                 │
                  └───────▲───────────────────────────────▲────────┘
                          │                               │
        Flow<List<ChatMessage>>                           │ UserPreferences
                          │                               │
             ┌────────────┴─────────────┐    ┌────────────┴─────────────┐
             │     GeminiRepository     │    │ UserPreferencesRepository│
             └────────────┬─────────────┘    └──────────────────────────┘
                          │
       ┌──────────────────┼─────────────────────────┐
       │                  │                         │
┌──────▼──────┐   ┌───────▼────────┐      ┌─────────▼──────────┐
│   Room DB   │   │ GenerativeAI   │      │ SecureApiKeyStorage│
│  (ChatDao)  │   │  SDK (Gemini)  │      │  (AndroidKeyStore) │
└─────────────┘   └────────────────┘      └────────────────────┘
```

- **Stateless UI**: `ChatScreen` composable is completely stateless. All state is held in `ChatUiState` exposed as a `StateFlow` from `ChatViewModel` and collected via `collectAsStateWithLifecycle()`.
- **Coroutines & Thread Safety**: All generative API calls and database transactions are dispatched onto `Dispatchers.IO`, keeping the main thread free and UI smooth at 60/120 FPS.

---

## API Key Security & Encryption at Rest

### 1. Build-Time Configuration & Git Protection
- The secret Gemini API key is stored locally in `local.properties` (which is strictly git-ignored).
- At build time, Gradle exposes the value through `BuildConfig.GEMINI_API_KEY`.
- CI fallback is implemented in `app/build.gradle.kts`:
  ```kotlin
  val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
      ?: System.getenv("GEMINI_API_KEY") ?: ""
  ```
- A `local.properties.example` template is provided in the repository with placeholder values.

### 2. Hardware-Backed Android KeyStore AES-256-GCM Encryption at Rest
- On first application launch, `SecureApiKeyStorage` inspects whether the API key is already encrypted.
- If not, `KeyStoreEncryptionManager` generates a 256-bit AES key inside the hardware-backed **Android KeyStore** using `KeyGenParameterSpec`:
  - Algorithm: `AES`
  - Block Mode: `GCM`
  - Padding: `NoPadding`
  - Key Size: `256 bits`
  - Provider: `AndroidKeyStore`
- The raw API key is encrypted using `AES/GCM/NoPadding`, producing ciphertext and a unique initialization vector (IV).
- **Only the ciphertext and IV are persisted at rest** in `EncryptedSharedPreferences`.
- The secret key **never leaves the KeyStore** hardware boundary.
- **In-Memory Decryption Only**: Decryption is performed strictly in volatile memory at the exact instant the `GenerativeModel` is instantiated. The decrypted key is never displayed in UI, toasted, or written to logs.

### 3. Release Build Obfuscation (R8)
- `isMinifyEnabled = true` is enabled in `app/build.gradle.kts` for the `release` build type.
- ProGuard rules (`proguard-rules.pro`) protect necessary Room, Generative AI SDK, and Coroutines runtime symbols while stripping unused code, renaming classes/methods, and preventing trivial reverse engineering.

---

## Limits of Client-Side Security & Production Best Practices

> [!CAUTION]
> **Important Security Architecture Note**  
> While client-side hardware-backed encryption (Android KeyStore AES-256-GCM) and R8 obfuscation significantly raise the bar against static decompilation and casual extraction, **client-side encryption cannot fully protect a secret against a determined attacker** who has physical access to a rooted device with tools like Frida, Xposed, or dynamic memory inspection.

### Recommended Production Architecture:
1. **Backend Proxy / BFF (Backend for Frontend)**:
   - Move all direct Gemini API calls behind a secure backend service (e.g., Cloud Run, Cloud Functions, or a dedicated Spring/Node.js microservice).
   - Mobile clients authenticate with user credentials (Firebase Auth / OAuth 2.0 / JWT), and the backend server holds the Gemini API key securely in Secret Manager.
2. **Firebase App Check**:
   - Integrate Firebase App Check with **Play Integrity API**.
   - Ensures that only genuine, untampered instances of your app running on certified Android hardware can make requests to your API backend.
3. **Restricted API Keys**:
   - In Google Cloud Console / AI Studio, restrict API keys by application package name and SHA-1 signing certificate fingerprint.
   - Apply API scope restrictions to ensure the key can only access the Generative Language API.

---

## Setup and Configuration

1. Clone the repository and checkout the branch:
   ```bash
   git checkout N127-MADa1
   ```
2. Copy `local.properties.example` to `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
3. Sync the project in Android Studio.

---

## Running the App & Tests

### Automated Unit Tests
The ViewModel and business logic are tested using `kotlinx-coroutines-test` with a `FakeGeminiRepository`:
```powershell
.\gradlew.bat testDebugUnitTest
```
**Test Results**: 9/9 tests passing (100% pass rate).
- Initial state verification
- Prompt change & error clearing
- Empty prompt validation
- Missing API key handling
- Successful generative response flow
- Network/API failure handling
- Voice input appending
- Clear chat history
- Error dismissal

### Compose UI Tests
Compose UI tests verify component rendering, prompt input, and progress states using `createComposeRule()`:
```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Build Debug & Release APKs
```powershell
# Debug APK
.\gradlew.bat assembleDebug

# Obfuscated Release APK (with R8 minification)
.\gradlew.bat assembleRelease
```
