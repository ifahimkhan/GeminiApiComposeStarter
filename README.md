# Gemini API Compose Starter — Android Chat Application

## 1. Project Overview

**Gemini API Compose Starter** is a native Android chat application built with modern Android development standards. The application implements an MVVM architecture with a clean separation of concerns, providing an interactive chat interface powered by the official Google Generative AI SDK (`gemini-3.6-flash`).

### Core Technology Stack
- **UI & Design:** Jetpack Compose with Material 3 components and dynamic theming.
- **AI Integration:** Google Generative AI SDK (`com.google.ai.client.generativeai`).
- **Local Persistence:** AndroidX Room database (`AppDatabase`, `ChatDao`, `ChatMessageEntity`) for offline conversation history.
- **Preferences Storage:** AndroidX Preferences DataStore (`UserPreferencesRepository`) for persistent user theme preferences.
- **Hardware Cryptography:** Android Keystore system with AES-256-GCM encryption (`ApiKeySecurityManager`).
- **Asynchronous Execution:** Kotlin Coroutines and StateFlow for reactive, lifecycle-aware state management.

---

## 2. Gemini API Key Configuration

To connect the application to the Gemini API, an API key obtained from [Google AI Studio](https://aistudio.google.com/) is required.

### Setup Instructions
1. In the root directory of the project, locate the template file `local.properties.example`.
2. Create or open your local `local.properties` file:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
3. Save the file. During build synchronization, Gradle reads this value and generates `BuildConfig.GEMINI_API_KEY`.

### Version Control Protection & CI Fallback
- **Git Protection:** `local.properties` is explicitly ignored in `.gitignore` and must **never** be committed to version control.
- **CI / Headless Build Fallback:** In [`app/build.gradle.kts`](app/build.gradle.kts), the build script reads from `local.properties` first and falls back to the environment variable:
  ```kotlin
  val geminiApiKey =
      localProperties.getProperty("GEMINI_API_KEY")
          ?: System.getenv("GEMINI_API_KEY")
          ?: ""
  ```
  This enables automated CI/CD pipelines to build the project without checking in configuration files.

> **Security Rule:** Plaintext API keys must never be hardcoded into Kotlin source files, XML manifests/resources, Gradle build files, documentation, or Git commit history.

---

## 3. API Key Security Architecture

The app implements cryptographic defense-in-depth through [`ApiKeySecurityManager`](app/src/main/java/com/fahim/geminiApiComposeStarter/data/security/ApiKeySecurityManager.kt).

### Cryptographic Workflow
1. **Master Key Generation:** 
   - A 256-bit AES key is generated and securely held inside the hardware-backed **Android Keystore** (`AndroidKeyStore`) under the alias `gemini_api_key_master_key`.
   - The master key material never leaves the secure hardware environment (TEE/SE where supported).
2. **Authenticated Encryption (AES-256-GCM):**
   - Cipher transformation: `AES/GCM/NoPadding`.
   - A cryptographically fresh, randomized 12-byte Initialization Vector (IV) is generated for every encryption operation (`setRandomizedEncryptionRequired(true)`).
   - A 128-bit authentication tag ensures ciphertext integrity and authenticity.
3. **Encrypted Storage at Rest:**
   - Only the Base64-encoded IV and ciphertext (`encrypted_api_key_iv`, `encrypted_api_key_ciphertext`) are saved in private application `SharedPreferences` (`gemini_secure_key_prefs`).
   - Plaintext API keys are never written to disk or database files.
4. **In-Memory Lifecycle:**
   - On application startup in `MainActivity`, the key is decrypted in memory and passed directly to `GeminiRepositoryImpl`.
   - The key is retained only ephemerally in RAM during the lifecycle of the repository.
5. **Zero Exposure:**
   - The API key is never logged via Android `Log`, displayed on screen, or exposed through UI components.

> **Defense-in-Depth Notice:** While hardware-backed Keystore encryption protects the API key at rest against unauthorized physical extraction or backup inspection, client-side encryption is not absolute protection against determined reverse-engineering on rooted devices with runtime instrumentation (e.g., Frida, memory dumpers).

---

## 4. Key Features

### Gemini Chat Interface
- **Conversation Bubbles:** Distinct Material 3 containers visually differentiating user queries (aligned right, primary container color) and model responses (aligned left, surface variant color with assistant icon).
- **Markdown Formatting:** Model responses parse bold markers (`**text**`) into Compose annotated bold strings using `toBoldAnnotatedString()`.
- **Loading & State Feedback:** In-flight queries display a dedicated "Thinking..." indicator bubble with an animated circular progress indicator.
- **Error Handling:** Non-blocking error notices (such as network failures or quota exhaustion) are surfaced through Material 3 `SnackbarHost`.
- **Message Stability:** `LazyColumn` employs stable unique message keys (`key = { message.id }`) and automatically animates scrolling to the latest message.

### Room Conversation Persistence
- Powered by `AppDatabase`, `ChatDao`, and the `ChatMessageEntity` Room table (`chat_messages`).
- Every user prompt and model response is persisted to SQLite via `withContext(Dispatchers.IO)`.
- `ChatViewModel` observes conversation history reactively via `chatDao.getAllMessages()`, ensuring conversation history seamlessly survives configuration changes, process death, and application restarts.

### Voice Speech-to-Text
- Implemented using Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched via `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- Voice input is handled directly by Android's system speech recognizer dialog, inserting recognized text into the active prompt field (appending with a space if text already exists).
- **Zero Dangerous Permissions:** Does **not** require or declare `android.permission.RECORD_AUDIO`, as audio capture is delegated entirely to the external system activity.
- Gracefully handles devices without speech recognition by catching `ActivityNotFoundException` and displaying an informative snackbar.

### Persistent Theme Preferences
- Backed by Jetpack **Preferences DataStore** (`UserPreferencesRepository`).
- A theme toggle button in the `TopAppBar` allows switching between explicit Dark Mode, explicit Light Mode, or following the system default (`isSystemInDarkTheme()`).
- The user's selection is stored asynchronously on disk and collected in `MainActivity` with lifecycle awareness.

### Responsive & Adaptive Layout
- The chat content is wrapped inside a centered, bounded container with a maximum width constraint of `840.dp` (`Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth().align(Alignment.TopCenter)`).
- Chat bubbles scale responsively up to `560.dp` (`Modifier.widthIn(max = 560.dp)`), preventing excessive, unreadable stretching across wide tablet, foldable, and landscape screens while remaining natural and comfortable on compact phones.

---

## 5. Security Limitations & Production Recommendations

For production environments, client-side mobile applications require additional architectural considerations:

1. **Backend Proxy Architecture (Recommended for Production):**
   - Distributing a Gemini API key within a client APK means the key can theoretically be intercepted by an attacker running a rooted device with memory debugging tools.
   - Production systems should route LLM requests through an authenticated backend server (e.g., Cloud Functions, Cloud Run, or a custom API gateway). The backend securely stores the API secret and authenticates client sessions.
2. **API Key Restrictions:**
   - If calling Google APIs directly from a mobile client, restrict the API key in Google Cloud Console to specific APIs (Gemini API only) and bind it to the Android application package name and release signing certificate SHA-1 fingerprint.
3. **App Attestation:**
   - Incorporate attestation services such as **Play Integrity API** or **Firebase App Check** to ensure requests originate exclusively from genuine, untampered instances of your app installed via Google Play.

---

## 6. Testing Strategy

The project contains a comprehensive automated test suite across unit and UI testing layers.

### 1. ViewModel Unit Tests (`ChatViewModelTest`)
- **Location:** [`app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt`](app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt)
- **Framework:** JUnit 4 + `kotlinx-coroutines-test` (`1.9.0`) using `StandardTestDispatcher`.
- **Pure Kotlin Fakes:** Employs `FakeGeminiRepository` and `FakeChatDao` with `CompletableDeferred` synchronization to ensure 100% deterministic testing across thread dispatchers.
- **Test Cases Covered (8 / 8 Passing):**
  1. `initialState_defaultsAreCorrect`: Verifies empty state defaults.
  2. `existingPersistedMessages_loadedIntoState`: Verifies preloaded DAO entities map correctly to domain messages.
  3. `onPromptChange_updatesPromptAndClearsPromptError`: Verifies typing updates prompt and clears validation errors.
  4. `blankPrompt_setsPromptErrorAndDoesNotCallRepository`: Verifies whitespace/blank prompt validation.
  5. `missingApiKey_setsErrorMessageAndDoesNotCallRepository`: Verifies missing key handling.
  6. `successfulSend_clearsPromptInsertsMessagesAndManagesLoading`: Verifies prompt clearing, user message insertion, loading lifecycle, and model message persistence.
  7. `repositoryFailure_setsErrorMessageAndStopsLoading`: Verifies error propagation when repository throws or fails.
  8. `duplicateSend_ignoredWhileLoading`: Verifies in-flight request debouncing.

### 2. Compose UI Instrumented Tests (`ChatScreenTest`)
- **Location:** [`app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt`](app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt)
- **Framework:** `androidx.compose.ui.test.junit4.createComposeRule` + AndroidJUnit4.
- **Hermetic Design:** Tests `ChatScreen` directly via state injection with `ChatUiState`, requiring zero mocking of ViewModels, Room, or backend network services.
- **Test Cases Covered (9 Tests):**
  1. `topAppBar_rendersTitle`: Verifies "Gemini Chat" title display.
  2. `promptField_isDisplayed`: Verifies prompt input field visibility.
  3. `sendButton_isDisplayed`: Verifies "Send" button visibility.
  4. `voiceInputButton_isDisplayed`: Verifies "Voice input" microphone button visibility.
  5. `promptField_typing_invokesOnPromptChange`: Verifies text input triggers callback.
  6. `sendButton_click_invokesOnSend`: Verifies Send button click triggers `onSend`.
  7. `chatMessages_renderUserAndModelBubbles`: Verifies rendering of USER and MODEL bubbles.
  8. `loadingState_displaysThinkingIndicator`: Verifies "Thinking..." loading indicator.
  9. `themeToggle_reflectsThemeState`: Verifies theme toggle icons, accessibility descriptions, and callback invocation.

> **Execution Note for Instrumented Tests:** Running `assembleDebugAndroidTest` compiles and packages the instrumented test APK without errors. Actual execution of `connectedDebugAndroidTest` requires a running Android emulator or connected physical Android device.

---

## 7. Release Build & Obfuscation

The release build variant is configured in [`app/build.gradle.kts`](app/build.gradle.kts) for maximum security and size reduction:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

- **R8 Minification (`isMinifyEnabled = true`):** Performs dead-code elimination, bytecode optimization, and obfuscates class, method, and field names.
- **Resource Shrinking (`isShrinkResources = true`):** Automatically discards unused drawables, layouts, and strings from the packaged APK.
- **ProGuard Optimization:** Employs Google's aggressive `proguard-android-optimize.txt` rules. The build compiles cleanly with zero R8 warnings or keep-rule conflicts.

---

## 8. Build & Test Commands Cheatsheet

Run the following commands from the project root in PowerShell:

### Compile & Assemble Builds
```powershell
# Set Android SDK environment variable (if not already set in your profile)
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Assemble Debug APK
.\gradlew.bat assembleDebug

# Clean and Assemble Obfuscated Release APK
.\gradlew.bat clean assembleRelease
```

### Run Automated Tests
```powershell
# Run all local JVM unit tests
.\gradlew.bat testDebugUnitTest

# Compile instrumented Compose UI test APK
.\gradlew.bat assembleDebugAndroidTest

# Run instrumented UI tests (requires running emulator or connected device)
.\gradlew.bat connectedDebugAndroidTest
```
