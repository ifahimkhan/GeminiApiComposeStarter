# Mobile Application Development — Lab Assignment 1
## Gemini Jetpack Compose AI Assistant

---

### Student Information
- **Student Name:** Vivaan Shrivastava
- **Roll Number:** N157
- **Course:** Mobile Application Development (Subject Code: 702AI0E002)
- **Institution:** SVKM's NMIMS University, School of Technology Management & Engineering (STME), Mumbai
- **Git Branch:** `N157-assignment-1`
- **Base Starter:** [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## 1. Project Overview

This project is an enhanced, production-grade Android AI Chat application built on modern **Jetpack Compose** and **Material 3**, powered by Google's **Gemini Generative AI SDK**. It transforms the single-turn starter into an interactive, multi-turn AI assistant with persistent conversation history, speech-to-text input, hardware-backed API key encryption, Preferences DataStore settings, adaptive multi-screen layouts, and comprehensive automated test suites.

---

## 2. Technical Architecture

The application strictly adheres to the official **Android Clean Architecture** and **Unidirectional Data Flow (UDF)** patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                 Jetpack Compose UI Layer                    │
│   (ChatScreen, MessageBubble, EmptyChatState, InputBar)     │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow<ChatUiState>
                               │ User Events (onSend, onVoice, onRetry)
┌──────────────────────────────┴──────────────────────────────┐
│                    ChatViewModel                            │
│     (StateFlow, viewModelScope, Coroutines, State Hoisting) │
└──────────────────────────────▲──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            │         ChatRepository              │
            │ (Domain Abstraction & Coordination) │
            └─────────▲─────────────────▲─────────┘
                      │                 │
    ┌─────────────────┴───────┐  ┌──────┴─────────────────────────┐
    │     ChatDatabase        │  │       GeminiRepository         │
    │     (Room / SQLite)     │  │ (Google Generative AI Client)  │
    │  - ChatMessageEntity    │  │  - Model: gemini-1.5-flash     │
    │  - ChatDao (Flow)       │  │  - In-memory decrypted key     │
    └─────────────────────────┘  └────────────────────────────────┘
                                                ▲
                                                │ Decrypted Key
                                 ┌──────────────┴─────────────────┐
                                 │       ApiKeyManager            │
                                 │   (Android Keystore + AES-GCM) │
                                 └────────────────────────────────┘
```

### Core Architecture Components:
1. **UI Layer (`ui/chat/`)**:
   - `ChatScreen.kt`: Stateless composable displaying conversations via `LazyColumn` with stable keys (`key = { it.id }`), auto-scrolling to latest messages, custom message bubbles, quick-start prompt chips, and voice input integration.
   - `ChatUiState.kt`: Immutable UI state class representing conversation history, text input, loading status, prompt validation errors, error banners, and retry targets.
   - `ChatViewModel.kt`: Lifecycle-aware state holder exposing `StateFlow<ChatUiState>` collected via `collectAsStateWithLifecycle()`. Manages input, duplicate-send prevention, speech-to-text insertion, message retries, and history clearing.

2. **Domain & Data Layer (`data/`)**:
   - `ChatRepository.kt`: Coordinates persistent message storage with AI network requests. Persists the user message immediately, invokes Gemini generation, and persists the response or error.
   - `GeminiRepository.kt` & `GeminiRepositoryImpl.kt`: Low-level abstraction over `com.google.ai.client.generativeai.GenerativeModel`. Receives in-memory decrypted API keys and translates network, quota, and authentication exceptions into user-friendly messages.

3. **Security Layer (`data/security/`)**:
   - `ApiKeyManager.kt`: Generates and manages hardware-isolated AES-256 keys inside the **Android Keystore**, encrypts API keys at rest with AES-GCM, and decrypts them strictly in-memory.

4. **Persistence Layer (`data/local/`)**:
   - `room/`: SQLite database (`ChatDatabase`, `ChatDao`, `ChatMessageEntity`) preserving conversation history across app restarts and orientation changes.
   - `preferences/`: Preferences DataStore (`UserPreferencesRepository`) storing user preferences (model choice, dynamic color, haptic feedback) without touching chat history.

---

## 3. Mandatory API Key Security Flow

### 3.1 Configuration via `local.properties`
The actual Gemini API key is **never** committed or stored in source code:
1. `local.properties` in the project root is explicitly git-ignored in `.gitignore`.
2. Gradle reads `GEMINI_API_KEY` from `local.properties` at build time:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
3. A safe template `local.properties.example` is tracked in version control:
   ```properties
   ## Example configuration for local builds
   # Copy this file to local.properties and replace the placeholder with your actual key.
   GEMINI_API_KEY=your_api_key_here
   ```

### 3.2 CI/CD Environment Variable Fallback
In `app/build.gradle.kts`, Gradle implements the fallback hierarchy:
```kotlin
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: ""
```
This enables seamless headless CI/CD builds using GitHub Actions secrets without requiring `local.properties`.

### 3.3 Hardware-Backed Encryption at Rest (Android Keystore + AES-256-GCM)
To satisfy assignment requirements for encryption at rest:
- **Key Generation**: On first launch, `ApiKeyManager` generates an AES-256 master key in `AndroidKeyStore` using `KeyGenParameterSpec`:
  - Purpose: `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`
  - Block Mode: `BLOCK_MODE_GCM`
  - Encryption Padding: `ENCRYPTION_PADDING_NONE`
  - Key Size: 256 bits with randomized IV generation.
- **Ciphertext Storage**: The API key is encrypted using `AES/GCM/NoPadding`. Only the Base64-encoded ciphertext and 12-byte GCM initialization vector (IV) are persisted in private storage.
- **Memory-Only Decryption**: When `MainActivity` creates the repository, the key is decrypted directly into an in-memory variable passed to `GenerativeModel`.
- **Zero Leakage**: Plaintext keys are never logged, never shown in UI, never displayed in Toasts, and never stored in Room or DataStore.

### 3.4 Client-Side Security Limitations & Production Recommendations
> [!IMPORTANT]
> **Client-Side Limitation**: Obfuscation and Android Keystore encryption significantly raise the barrier for attackers by preventing passive extraction from APK binaries and static analysis. However, any secret decrypted and used directly on a client device can theoretically be intercepted by an attacker with root access, modified runtime instrumentation (e.g. Frida, Xposed), or compromised hardware memory dumps.

**Recommended Production Architectures**:
1. **Backend Proxy Gateway**: The mobile client authenticates against a trusted backend server (via OAuth 2.0 / Firebase Auth). The backend holds the Gemini API key securely in Secret Manager and forwards generation requests to the Gemini API.
2. **Firebase App Check**: Validates that incoming API traffic originates from genuine, untampered instances of your app using Play Integrity API.
3. **Restricted API Keys**: Restrict Google Cloud API keys by SHA-1 Android fingerprint, package name (`com.fahim.geminiApiComposeStarter`), and specific API services (Google Generative Language API).

---

## 4. Key Features Implemented

### 4.1 Polished Jetpack Compose UI
- **LazyColumn Conversation**: Displays chat bubbles with stable item keys (`it.id`) preventing recomposition jank.
- **Auto-Scroll**: Automatically scrolls to the newest message whenever state updates.
- **Message Bubbles**: Asymmetric rounded bubbles differentiating User (PrimaryContainer, aligned right) and Gemini (SurfaceVariant, aligned left) with formatted timestamps and copy-to-clipboard action.
- **Markdown Formatting**: Renders `**bold**` markdown formatting within AI responses.
- **Empty State**: Welcoming card featuring Vivaan Shrivastava with quick-start suggestion chips that populate the input bar when clicked.
- **Typing / Loading Indicator**: Subtle circular progress bubble indicating Gemini is generating a response.
- **Duplicate Send Prevention**: The send button and input bar are disabled while a request is actively processing to prevent repeated submissions.
- **Error Handling with Retry**: Non-blocking Material 3 error card with an integrated **Retry** action button that resubmits failed queries.

### 4.2 Voice Input (Speech-to-Text)
- Utilizes `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched via `rememberLauncherForActivityResult`.
- Transcribes user speech and inserts text directly into the prompt input field.
- Gracefully handles cancellation, silence, and devices lacking speech recognition without crashing.

### 4.3 Persistent Chat History (Room)
- Built with Room 2.6.1 (`ChatMessageEntity`, `ChatDao`, `ChatDatabase`).
- Conversations survive app closures, configuration changes, and device reboots.
- Includes a top bar action to clear conversation history with a confirmation dialog.

### 4.4 User Preferences (Preferences DataStore)
- Built with Preferences DataStore 1.1.1 (`UserPreferencesRepository`).
- Stores user settings (model selection, dynamic color toggle, haptic feedback) off the main thread with coroutines.

### 4.5 Responsive & Adaptive UI
- Employs window size and screen width factor calculations:
  - Phone portrait: Max message bubble width of 85%.
  - Tablets / landscape: Max message bubble width of 65% with optimal horizontal padding.

### 4.6 Dark Mode
- Full support for Android system dark theme via `isSystemInDarkTheme()`.
- Material 3 dynamic color theming on Android 12+ (API 31+).

### 4.7 Release Build Obfuscation (R8)
- Enabled `isMinifyEnabled = true` in the release build type.
- Custom keep rules in `proguard-rules.pro` protecting Room DAOs, Coroutines, DataStore, and Generative AI SDK reflection.

---

## 5. Automated Testing

### 5.1 Unit Testing (`ChatViewModelTest.kt`)
Tested with `kotlinx-coroutines-test`, `StandardTestDispatcher`, and `FakeChatRepository`.
- **Location**: `app/src/test/java/com/fahim/geminiApiComposeStarter/ChatViewModelTest.kt`
- **Total Tests**: 11
- **Passed**: 11
- **Failures**: 0
- **Success Rate**: 100%

Test Scenarios Covered:
1. `initialState_hasDefaultValues`
2. `onPromptChange_updatesPromptAndClearsErrors`
3. `onSend_withEmptyPrompt_setsPromptError`
4. `onSend_whenApiKeyMissing_setsMissingApiKeyError`
5. `onSend_withValidPrompt_triggersLoadingAndSucceeds`
6. `onSend_whenRepositoryFails_setsErrorMessageAndPreservesFailedPromptForRetry`
7. `onRetry_resendsFailedPrompt`
8. `onSend_preventsDuplicateSendWhileLoading`
9. `onVoiceInputResult_updatesPromptCorrectly`
10. `onClearChat_removesAllMessages`
11. `onDismissError_clearsErrorMessage`

### 5.2 Compose UI Instrumented Testing (`ChatScreenTest.kt`)
Tested with `createComposeRule()` from `androidx.compose.ui.test.junit4`.
- **Location**: `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ChatScreenTest.kt`
- Test Scenarios:
  - Empty state renders welcome text and quick prompts.
  - User and Gemini chat bubbles render with message content.
  - Loading bubble renders when `isLoading = true` and send button disables.
  - Error card displays failure message and triggers retry callback.
  - Text input enables send button.
  - Clicking quick prompt chip populates input prompt.

---

## 6. How to Build and Run

### Prerequisites
- Android Studio Ladybug / Meerkat or compatible CLI
- JDK 17, 21, or 25 (Android Studio JBR)
- Android SDK Platform 36 (minSdk: 26)

### Step 1: Configure API Key
Create `local.properties` in the project root:
```properties
sdk.dir=/path/to/your/android-sdk
GEMINI_API_KEY=your_gemini_api_key_here
```

### Step 2: Run Unit Tests
```bash
./gradlew testDebugUnitTest
```
Test results and HTML report will be generated in `app/build/reports/tests/testDebugUnitTest/index.html`.

### Step 3: Compile Android Instrumented Tests
```bash
./gradlew compileDebugAndroidTestKotlin
```

### Step 4: Build Debug APK
```bash
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Step 5: Build Release APK with R8 Obfuscation
```bash
./gradlew assembleRelease
```
Output APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 7. Git & Security Audit Verification

- [x] Branch: `N157-assignment-1`
- [x] `local.properties` is in `.gitignore` and untracked.
- [x] `local.properties.example` contains only safe placeholder.
- [x] No plaintext API keys exist in Git commits or diffs.
- [x] No secrets in strings, manifests, or Kotlin source code.
