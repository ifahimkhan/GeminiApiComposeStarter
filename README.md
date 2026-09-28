# Gemini Jetpack Compose Chat Application

**Course:** Mobile Application Development (Lab Assignment-1)  
**Institution:** SVKM's NMIMS University — School of Technology Management & Engineering  
**Student Name:** Yash Tiwari  
**Roll Number:** N141  
**Git Submission Branch:** `N141-assignment-1`  

---

## 1. Project Overview

This repository contains the complete implementation of **Mobile Application Development Lab Assignment-1**, built on top of Google's Gemini Generative AI SDK, Android Jetpack Compose, Material 3, and modern Clean Android Architecture.

The application delivers an intelligent conversational chat experience powered by the Gemini 1.5 Flash model with offline persistence, speech-to-text voice recognition, cryptographic key storage via Android Keystore, Preferences DataStore configuration, and adaptive UI support across phone, landscape, and tablet form factors.

---

## 2. Key Features

- **Jetpack Compose & Material 3 UI:**
  - Modern message bubble layout differentiating user prompts and Gemini responses with role-based styling, subtle tonal elevations, and smooth rounded geometry.
  - Efficient `LazyColumn` conversation rendering with stable unique item keys (`key = { it.id }`).
  - Automatic animated scrolling (`animateScrollToItem`) smoothly positioning the latest message into view upon transmission or generation.
  - Markdown bold syntax parsing (`**bold text**` rendered as `FontWeight.Bold` via `toBoldAnnotatedString()`).
  - Interactive empty-state suggestions allowing one-tap insertion of common starter prompts.
  - Formatted message timestamps (`hh:mm a`).
  - Contextual error banners featuring one-tap prompt retry mechanisms.
  - Distinct animated bottom progress indicator (`CircularProgressIndicator` with `"Gemini is thinking..."`) providing non-blocking visual feedback during generation.

- **Persistent Chat History (Room Database):**
  - Messages survive application closures, process recreation, and configuration/orientation changes.
  - SQLite entity schema (`ChatMessageEntity`) tracking `id`, `content`, `role` (USER / GEMINI), `timestamp`, and `isError`.
  - Non-blocking reactive queries backed by Kotlin Coroutines `Flow<List<ChatMessageEntity>>`.
  - One-tap history purge via top action bar menu with dialog confirmation.

- **Lightweight Preferences (Preferences DataStore):**
  - Reactive storage for user settings: AI model selection (default: `gemini-1.5-flash`), theme preference (System Default, Light, Dark), and dynamic color toggle.
  - Segregated from chat data to avoid storing chat message arrays in DataStore.

- **Voice Input (Speech-to-Text):**
  - Integrated voice button utilizing Android's `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via Compose `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
  - Seamlessly transcribes spoken speech into the chat input field.
  - Resilient error handling covering missing speech recognizers, speech cancellation, and empty transcriptions without crashing.

- **Responsive & Adaptive Layout:**
  - Utilizes `WindowWidthSizeClass` (Compact, Medium, Expanded).
  - Dynamically calculates maximum chat bubble widths (e.g., 85% for Compact portrait, 65% for Medium landscape/foldable, 50% for Expanded tablet) to prevent excessively wide text bubbles and preserve typographic readability.
  - Input field automatically adjusts padding, layout sizing, and keyboard navigation.

- **System Dark Mode & Dynamic Theming:**
  - Full Material 3 dynamic color scheme support (`dynamicLightColorScheme` and `dynamicDarkColorScheme` on Android 12+).
  - High-contrast, WCAG-compliant color pairings across user bubbles, assistant cards, text fields, and icons.

- **Clean Architecture & State Management:**
  - Clear separation of concerns: `UI (Composables)` → `ViewModel` → `Repository Layer` → `Local (Room/DataStore)` + `Remote (Gemini API)`.
  - Unidirectional Data Flow (UDF) managed via `ChatUiState` and exposed through immutable `StateFlow<ChatUiState>`.
  - Lifecycle-aware Compose collection via `collectAsStateWithLifecycle()` to prevent unnecessary background execution and memory leaks.
  - State hoisting ensuring composables remain stateless, highly previewable, and testable.

- **Duplicate Request Protection:**
  - Input send button and action triggers are immediately disabled while a request is in progress (`isLoading == true`), preventing race conditions and accidental multiple requests.

---

## 3. Technology Stack & Dependencies

| Layer / Component | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 (with AGP 9.0 built-in toolchain) |
| UI Framework | Jetpack Compose (BOM) | 2024.09.00 |
| Design System | Material Design 3 | 1.3.0 |
| Adaptive Layout | Material 3 Window Size Class | 1.3.0 |
| Architecture | Android Jetpack (ViewModel, Lifecycle, StateFlow) | 2.8.5 |
| Local Database | AndroidX Room (Runtime, KTX, KSP) | 2.6.1 (`room.generateKotlin = "true"`) |
| Preferences | AndroidX Preferences DataStore | 1.1.1 |
| AI Integration | Google Generative AI Android SDK | 0.9.0 |
| Cryptography | Android Keystore System + AES-256-GCM | Native Android Security Framework |
| Asynchronous / Coroutines | KotlinX Coroutines Android | 1.8.1 |
| Unit Testing | JUnit 4, KotlinX Coroutines Test | 4.13.2 / 1.8.1 |
| UI Testing | Compose UI Test JUnit 4, Compose Manifest | 1.7.2 / 1.9.2 |
| Shrinker & Obfuscation | R8 / ProGuard (`isMinifyEnabled = true`) | AGP 9.0 |

---

## 4. API Key Security & Cryptographic Architecture

### 4.1. The Vulnerability of Client-Side API Keys
Embedding raw, unencrypted API credentials in client applications creates critical vulnerabilities:
1. Decompilation tools (e.g., `jadx`, `apktool`) easily extract plaintext strings from bytecode, `strings.xml`, or `BuildConfig`.
2. Hardcoding secrets in source code risks irreversible credential leakage when pushing to public or shared Git repositories.
3. Plaintext storage in shared preferences or SQLite databases exposes keys to rooted devices, backup extraction, or local physical attacks.

### 4.2. Implementation Architecture: Android Keystore + AES-256-GCM
To satisfy the strict assignment requirements for encryption at rest:
1. **Android Keystore System:** The app generates a hardware-backed master key under the alias `GeminiApiKeyAlias` using `KeyGenParameterSpec` with algorithm `KeyProperties.KEY_ALGORITHM_AES` (256-bit) and block mode `KeyProperties.BLOCK_MODE_GCM` with `KeyProperties.ENCRYPTION_PADDING_NONE`.
2. **Authenticated Encryption (AES-256-GCM):**
   - Each encryption operation generates a unique cryptographically secure 12-byte Initialization Vector (IV).
   - The cipher outputs ciphertext and an authentication tag protecting both confidentiality and integrity.
   - The combined payload (`IV + Ciphertext`) is stored encrypted at rest.
3. **Strict In-Memory Decryption:**
   - The API key is decrypted **strictly in memory** when initializing the `GenerativeModel` instance.
   - Decrypted plaintext keys are **never** logged, never stored in Room, never stored in DataStore, never displayed in Toast or UI, and never exposed in error messages.

### 4.3. Build-Time Configuration & Fallback Hierarchy
The API key is injected at build time without hardcoding:
```
local.properties (GEMINI_API_KEY)
       ↓ (if absent)
Environment Variable (GEMINI_API_KEY)
       ↓ (if absent)
Empty String ("")
```

Implemented in `app/build.gradle.kts`:
```kotlin
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: ""

buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
```

### 4.4. Security Audit of Repository
- `local.properties` is strictly ignored by `.gitignore` and **must never be committed**.
- `local.properties.example` provides a safe placeholder template:
  ```properties
  # Template for local development. Copy to local.properties and set your real key.
  GEMINI_API_KEY=your_api_key_here
  ```

### 4.5. Production Recommendation
While on-device AES-256-GCM encryption with Android Keystore raises the bar significantly against casual inspection, **client-side encryption alone cannot completely prevent compromise by a determined attacker with dynamic instrumentation tools (e.g., Frida, Xposed) or memory analysis**.

For commercial production deployments, the following architecture is required:
1. **Backend Proxy / API Gateway:** The mobile client authenticates against a secure backend (e.g., Cloud Functions / App Engine / Cloud Run). The backend retains the Gemini API key securely in Secret Manager and forwards vetted requests to Google AI.
2. **Firebase App Check:** Validates that incoming API requests originate strictly from an authentic, untampered instance of your mobile application using Play Integrity attestation.
3. **API Key Restrictions:** Configure Cloud Console restrictions restricting API keys by package name, SHA-256 signing fingerprint, and specific authorized API endpoints (Generative Language API only).

---

## 5. Build and Execution Instructions

### Prerequisites
- JDK 21 (located at `JAVA_HOME`)
- Android SDK (API 34 / 36 with Build-Tools installed)
- Android Studio Ladybug / Meerkat or compatible Gradle environment

### 5.1. Configuring the Gemini API Key
1. Generate an API key at [Google AI Studio](https://aistudio.google.com/).
2. Copy `local.properties.example` to `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<Username>\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=AIzaSy...your_actual_key_here
   ```
3. Alternatively, export an environment variable:
   ```bash
   export GEMINI_API_KEY="AIzaSy...your_actual_key_here"
   ```

### 5.2. Compiling the Application
- **Compile Kotlin Debug:**
  ```bash
  ./gradlew compileDebugKotlin
  ```
- **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk` (11.92 MB)

- **Build Production Release APK (with R8 Minification):**
  ```bash
  ./gradlew assembleRelease
  ```
  Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (2.13 MB)  
  *R8 Shrinking reduces binary footprint by ~82% while obfuscating classes and removing dead code.*

---

## 6. Testing

### 6.1. Running Unit Tests
Unit tests are implemented in `app/src/test/java/com/fahim/geminiApiComposeStarter/ChatViewModelTest.kt` using `kotlinx-coroutines-test`, `StandardTestDispatcher`, and fake repository/DAO implementations. Tests execute completely offline without calling live Gemini endpoints.

Execute via terminal:
```bash
./gradlew testDebugUnitTest
```

#### Test Suite Coverage (11 tests, 0 failures, 0 errors):
1. `initialState_hasExpectedDefaults` — Verifies clean initial state, empty prompt, and lack of false errors.
2. `onPromptChange_updatesPromptAndClearsError` — Verifies prompt text synchronization and error banner dismissal upon typing.
3. `onSend_emptyPrompt_setsEmptyPromptError` — Asserts validation preventing empty or whitespace-only messages.
4. `onSend_successfulResponse_persistsMessagesAndResetsLoading` — Verifies prompt submission, loading state transition, mock response insertion, and Room persistence.
5. `onSend_failedResponse_setsErrorAndAllowsRetry` — Tests network/API error recovery and populates `lastFailedPrompt` for retry.
6. `onRetry_retriesFailedPrompt` — Verifies one-tap resubmission of previously failed prompts.
7. `onDismissError_clearsErrorMessage` — Verifies error dismissal handling.
8. `onSend_loadingState_preventsDuplicateSendRequests` — Ensures concurrent send attempts are ignored while Gemini is actively generating.
9. `onSend_missingApiKey_showsMissingKeyError` — Tests graceful feedback when no API key is configured.
10. `onVoiceResult_insertsRecognizedTextIntoPrompt` — Validates speech recognition text insertion into active prompt state.
11. `onClearHistory_removesAllMessages` — Validates database history clearance.

### 6.2. Compose UI Instrumentation Tests
Compose UI tests are implemented in `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ChatScreenTest.kt` using `createComposeRule()`.

Execute on an Android device or emulator:
```bash
./gradlew connectedAndroidTest
```

Tests verify:
- Chat screen scaffolding and top app bar display.
- Outlined text input field presence and interaction.
- Send and Voice microphone icon button presence and semantic labels.
- Empty state suggestion chips rendering and clicking.
- Chat message bubble rendering for user prompts and assistant responses.
- Active loading progress indicator rendering when `isLoading = true`.

---

## 7. Submission Checklist Verification

- [x] Student Name: **Yash Tiwari**
- [x] Roll Number: **N141**
- [x] Branch: **`N141-assignment-1`**
- [x] Jetpack Compose & Material 3
- [x] Room Database chat history persistence
- [x] Preferences DataStore user settings
- [x] Android Keystore AES-256-GCM encryption at rest
- [x] In-memory API key decryption
- [x] `local.properties` ignored and excluded from Git
- [x] `local.properties.example` created with safe placeholder
- [x] Secret audit passed (zero secrets committed)
- [x] `LazyColumn` with stable item keys
- [x] Auto-scrolling to latest message
- [x] WindowSizeClass adaptive layout
- [x] Speech-to-Text Voice Input
- [x] System Dark Mode & Dynamic Colors
- [x] Unit Tests executed and passing (11/11)
- [x] Debug APK build verified (`assembleDebug`)
- [x] Release APK build with R8 minification verified (`assembleRelease`)
