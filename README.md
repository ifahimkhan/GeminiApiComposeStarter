# Gemini Jetpack Compose Starter App - Enhanced Edition

**Course:** Mobile Application Development (Subject Code: 702AI0E002)  
**Institution:** SVKM's NMIMS University, School of Technology Management & Engineering, Mumbai  
**Lab Assignment:** LAB ASSIGNMENT-1  

---

## 🌟 Overview

This project is an enhanced, production-ready Android application built with **Jetpack Compose**, **Google Gemini AI Client SDK**, **Room Database**, **Preferences DataStore**, and **Android Keystore AES-256-GCM** encryption at rest.

The app provides a responsive chat interface where users can interact with Gemini via text or voice input, persist past conversations across app restarts, customize their experience, and secure their API credentials.

---

## 🛠️ Key Architectural Enhancements

### 1. API Key Security & Encryption at Rest (Mandatory)
* **Never Hardcoded:** The Gemini API key is never committed or hardcoded in any Kotlin file, XML resource, or build script.
* **Local Properties & VCS Isolation:** `local.properties` is ignored by Git via `.gitignore`. A template file `local.properties.example` is provided for new developers.
* **CI/CD Environment Fallback:** In `app/build.gradle.kts`, the build system checks `local.properties` first and seamlessly falls back to the `GEMINI_API_KEY` system environment variable for headless CI builds:
  ```kotlin
  val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
      ?: System.getenv("GEMINI_API_KEY") ?: ""
  ```
* **Android Keystore AES-256-GCM Encryption:** On first launch, the application generates a hardware-backed AES-256-GCM key inside the Android Keystore (`AndroidKeyStore`) using `KeyGenParameterSpec`. The plaintext key is encrypted and only the ciphertext and Initialization Vector (IV) are persisted in Preferences DataStore.
* **Decrypted In-Memory Only:** Decryption occurs strictly in-memory at the exact instant the `GenerativeModel` is instantiated. The decrypted key is never logged, toasted, or exposed in the UI.
* **R8 Code Obfuscation:** The release build has `isMinifyEnabled = true` to obfuscate code and symbol names, preventing trivial reverse engineering of decompiled APKs.

### 2. Modern Jetpack Compose UI
* **LazyColumn Chat Stream:** Messages are rendered in a `LazyColumn` using Material 3 chat bubbles (user vs. Gemini) with stable unique keys (`key = { it.id }`) for optimal recomposition performance.
* **Auto-Scrolling:** Employs `rememberLazyListState()` and `LaunchedEffect` to smoothly scroll down to the newest message whenever a response arrives or input is submitted.
* **State Hoisting:** The entire UI state is modeled as an immutable `ChatUiState` hoisted in `ChatViewModel` and exposed via `StateFlow`. Composables collect the state using `collectAsStateWithLifecycle()` to remain completely stateless and lifecycle-aware.
* **Responsive Layouts (`WindowSizeClass`):** Uses AndroidX `calculateWindowSizeClass` to automatically adapt between:
  * **Compact (Phones):** Single-column stream with top app bar, conversation bubbles, and prompt bar.
  * **Expanded (Tablets / Foldables / Landscape):** Adaptive two-pane layout featuring an interactive sidebar (user profile, stats, quick prompt chips, clear history) and an expansive chat stream.
* **Loading & Error Feedback:** Features an inline typing indicator with `CircularProgressIndicator` during generation, a `SnackbarHost` notification, and an interactive `ErrorCard` with a retry button on network failures.
* **Dark Mode & Dynamic Color:** Fully integrates with `isSystemInDarkTheme()` and Material 3 dynamic color palettes in `Theme.kt`.

### 3. Expanded Functionality
* **Voice Input (Speech-to-Text):** Users can speak their queries directly. The voice action triggers `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched with `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
* **Conversation History (Room Database):** Past conversation messages are stored locally in Room (`AppDatabase`, `ChatMessageEntity`, `ChatMessageDao`) so chat history survives process death and app restarts.
* **Personalized Experience (Preferences DataStore):** Remembers user preferences such as user name and AI assistant tone/persona via `UserPreferencesRepository`.
* **Quick Suggestions:** Provides prompt suggestion chips for fast, one-tap queries on common topics.

---

## 🔒 Security Architecture: Know the Limits

> [!IMPORTANT]
> **Client-Side Encryption Considerations:**  
> Client-side encryption with Android Keystore raises the barrier significantly against passive extraction and decompilation. However, any secret stored or decrypted on an untrusted client device can theoretically be extracted by a determined attacker with root access or dynamic instrumentation (e.g., Frida / Xposed).

### Recommended Production Architecture:
1. **Backend Proxy:** In a production architecture, client apps should never store the master Gemini API key directly. Instead, client requests should authenticate against an enterprise backend (e.g., Firebase Cloud Functions, Google Cloud Run) which holds the Gemini API key securely in Secret Manager and mediates all LLM queries.
2. **Firebase App Check:** Protect your backend or AI endpoints using Firebase App Check with Play Integrity Attestation, ensuring only genuine, untampered instances of your app can call the service.
3. **Restricted API Keys:** In Google Cloud / Google AI Studio console, apply strict application restrictions (SHA-256 fingerprint + package name) and API scope restrictions.

---

## 🚀 Getting Started

### 1. Configure Gemini API Key
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. Create or open `local.properties` in the project root.
3. Add your API key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
4. For CI/CD environments, export the environment variable:
   ```bash
   export GEMINI_API_KEY="your_actual_gemini_api_key_here"
   ```

### 2. Build the Project
* **Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
* **Release APK (with R8 Obfuscation):**
  ```bash
  ./gradlew assembleRelease
  ```

---

## 🧪 Running Tests

### Unit Tests
The unit tests verify `ChatViewModel` state transitions, empty prompt validation, missing API key handling, Room insertion, voice STT input, and error recovery using `kotlinx-coroutines-test` and a fake repository.
```bash
./gradlew testDebugUnitTest
```

### Compose UI Instrumented Tests
The UI tests use `createComposeRule()` to verify composable rendering, chat bubble layouts, prompt submission, thinking indicators, and error cards on a device or emulator.
```bash
./gradlew connectedAndroidTest
```

---

## 📋 Submission Checklist Verification

- [x] Branch created following roll number naming convention: `702AI0E002_Gemini_Assignment1`
- [x] Missing `gradle-wrapper.jar` and `ic_assistant.xml` restored and compilation verified.
- [x] API key isolated from Git; `local.properties.example` created.
- [x] Fallback to CI environment variable in `app/build.gradle.kts`.
- [x] Android Keystore AES-256-GCM encryption at rest implemented in `ApiKeyStorage`.
- [x] In-memory decryption only at `GenerativeModel` creation.
- [x] Release build obfuscated with `isMinifyEnabled = true`.
- [x] Material 3 `LazyColumn` chat bubbles with auto-scroll and stable keys.
- [x] State hoisting with `ChatUiState` and `collectAsStateWithLifecycle()`.
- [x] Responsive layout with `WindowSizeClass` (phone compact vs. tablet/landscape two-pane).
- [x] Voice input via `RecognizerIntent` and `rememberLauncherForActivityResult`.
- [x] Room database persistence for chat history across app restarts.
- [x] Preferences DataStore for user preferences and personalization.
- [x] Unit tests with `kotlinx-coroutines-test` and fake repository passing.
- [x] Compose UI tests with `createComposeRule()` implemented.
- [x] Comprehensive README with production security limits and testing instructions.
