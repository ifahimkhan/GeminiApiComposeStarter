# SVKM's NMIMS University
## School of Technology Management & Engineering, Mumbai
### Department of Computer Engineering & Information Technology

---

# LAB ASSIGNMENT - 1
## MOBILE APPLICATION DEVELOPMENT (Subject Code: 702AI0E002)
### Academic Year: 2024–2025 / 2025–2026

---

| Academic Details | Information |
| :--- | :--- |
| **Student Name** | Devraj Murari |
| **Roll Number** | **N066** |
| **Course** | Mobile Application Development (`702AI0E002`) |
| **Project Name** | Gemini AI Assistant Chatbot (Jetpack Compose & Material 3) |
| **Submission Branch** | `N066_assignment_1` |
| **GitHub Profile** | [https://github.com/dev-raj-murari](https://github.com/dev-raj-murari) |
| **Student Repository** | [https://github.com/dev-raj-murari/GeminiApiComposeStarter](https://github.com/dev-raj-murari/GeminiApiComposeStarter) |
| **Package Identifier** | `com.devraj.geminiassistant` |

---

## 📑 Table of Contents
1. [Executive Summary & Problem Statement](#1-executive-summary--problem-statement)
2. [Application Architecture & Design](#2-application-architecture--design)
3. [Security & Hardware Keystore Encryption](#3-security--hardware-keystore-encryption)
4. [User Interface & Jetpack Compose Enhancements](#4-user-interface--jetpack-compose-enhancements)
5. [Local Persistence & DataStore Preferences](#5-local-persistence--datastore-preferences)
6. [Speech-to-Text (Voice Input) Modality](#6-speech-to-text-voice-input-modality)
7. [Automated Testing Suite (Unit & UI)](#7-automated-testing-suite-unit--ui)
8. [Production Security Analysis ("Know the Limits")](#8-production-security-analysis-know-the-limits)
9. [Submission & Git Commands](#9-submission--git-commands)
10. [Conclusion](#10-conclusion)

---

## 1. Executive Summary & Problem Statement

This assignment implements an enterprise-grade AI chatbot on Android leveraging Google's **Gemini AI SDK** (`com.google.ai.client.generativeai`), designed and developed by **Devraj Murari (Roll Number: N066)**. 

### Core Objectives Fulfilled:
- **Mandatory Credential Protection:** Zero hardcoded API keys, strict `.gitignore` isolation, hardware-backed **AES-256-GCM Android Keystore** encryption at rest, on-demand in-memory decryption, and **R8 release bytecode minification**.
- **Material 3 Declarative UI:** Stateful list rendering via `LazyColumn` with stable keys, animated auto-scrolling to new emissions, quick prompt suggestion chips, and responsive multi-window layouts adapting across phones, foldables, and tablets using `WindowSizeClass`.
- **Offline Persistence & UDF State Management:** **Room Database** for conversation history survival across process recreation, **Preferences DataStore** for user-customizable system instructions and temperature, and reactive **Unidirectional Data Flow (UDF)** via `StateFlow` and `collectAsStateWithLifecycle()`.
- **Multimodal Voice Input:** Speech recognition via Android `RecognizerIntent` launched through Compose activity result contracts.
- **Automated Verification:** Coroutine unit testing using `kotlinx-coroutines-test` with fake repositories and Compose UI instrumented tests using `createComposeRule()`.

---

## 2. Application Architecture & Design

The application adheres to the official Android Architecture Guidelines implementing **MVVM (Model-View-ViewModel)** with the **Repository Pattern**:

```
+-----------------------------------------------------------------------+
|                         PRESENTATION LAYER (Compose)                  |
|  - MainActivity.kt (WindowSizeClass calculation & theme injection)     |
|  - ChatScreen.kt (Scaffold, TopAppBar, LazyColumn, Responsive layout) |
|  - ChatBubble.kt (Material 3 bubble cards, formatted timestamps)      |
|  - ChatInputBar.kt (Outlined input, mic trigger, animated actions)    |
|  - SettingsDialog.kt (System prompt & temperature configuration)      |
+-----------------------------------------------------------------------+
                                   |
                       Collects StateFlow / Sends Events
                                   v
+-----------------------------------------------------------------------+
|                         VIEWMODEL LAYER                               |
|  - ChatViewModel.kt (StateFlow<ChatUiState>, coroutine dispatchers)   |
|  - ChatUiState.kt (Immutable UI data model)                           |
+-----------------------------------------------------------------------+
                                   |
                           Coroutines / Flow
                                   v
+-----------------------------------------------------------------------+
|                         DATA & REPOSITORY LAYER                       |
|  - GeminiRepository.kt (Interface)                                    |
|  - GeminiRepositoryImpl.kt (Orchestrates Gemini, Room, DataStore)     |
+-----------------------------------------------------------------------+
       |                                   |                    |
       v                                   v                    v
+-----------------------+     +-----------------------+ +---------------+
|  Google Generative AI |     |     Room Database     | | DataStore &   |
|  SDK (GenerativeModel)|     | (ChatMessageDao / DB) | | Keystore      |
|  (In-Memory API Key)  |     |  Survives App Restarts| | (AES-256-GCM) |
+-----------------------+     +-----------------------+ +---------------+
```

---

## 3. Security & Hardware Keystore Encryption

### 3.1 Zero-Leakage Build Configuration
The Gemini API key is isolated inside `local.properties` (git-ignored) with a dynamic fallback to environment variables in `app/build.gradle.kts` for CI/CD builds:
```kotlin
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY") ?: ""
```

### 3.2 Hardware-Backed AES-256-GCM Keystore Encryption
`KeystoreHelper.kt` generates and protects an AES-256 encryption key inside the hardware-backed `AndroidKeyStore`:
```kotlin
val keyGenParameterSpec = KeyGenParameterSpec.Builder(
    KEY_ALIAS,
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setUserAuthenticationRequired(false)
    .setRandomizedEncryptionRequired(true)
    .build()
```
- **Encryption at Rest:** On first app launch, `SecureKeyStorage.kt` encrypts `BuildConfig.GEMINI_API_KEY` and persists only the ciphertext and randomized initialization vector (IV) in private storage.
- **In-Memory Decryption:** Plaintext is decrypted into RAM **only** at the precise instant `GenerativeModel` is instantiated. No decrypted credentials are ever saved to disk, logged, or toasted.

---

## 4. User Interface & Jetpack Compose Enhancements

### 4.1 LazyColumn with Stable Keys & Animated Auto-Scroll
Conversation items are rendered in a `LazyColumn` using stable keys (`key = { message.id }`) to eliminate redundant recompositions during high-frequency chat updates:
```kotlin
LazyColumn(
    state = listState,
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
) {
    items(
        items = state.messages,
        key = { message -> message.id }
    ) { message ->
        ChatBubble(message = message)
    }
}

// Auto-scroll animation upon arrival of new messages
LaunchedEffect(state.messages.size) {
    if (state.messages.isNotEmpty()) {
        listState.animateScrollToItem(state.messages.size - 1)
    }
}
```

### 4.2 Quick Prompt Suggestion Chips
When the conversation is empty, horizontal scrollable suggestion chips allow the user to trigger pre-configured prompts with a single tap.

### 4.3 Multi-Window Responsive Layout (`WindowSizeClass`)
- **Compact Screens (Phones):** Vertical column layout with a clean Material 3 top bar, conversation list, and sticky input bar.
- **Medium & Expanded Screens (Tablets & Foldables):** Dynamic two-pane layout featuring an AI Configuration sidebar on the left pane and active conversation stream on the right pane.

---

## 5. Local Persistence & DataStore Preferences

### 5.1 Room Database Integration
- **`ChatMessageEntity`:** Schema storing message primary key (`id`), text body, sender flag (`isUser`), creation timestamp, and error status (`isError`).
- **`ChatMessageDao`:** Exposes reactive Flow queries (`getAllMessagesFlow()`) and asynchronous insertions (`insertMessage()`).
- **Persistence Guarantee:** Chat history survives process death, activity recreations, and device reboots.

### 5.2 Preferences DataStore
- Stores custom system prompts and temperature settings using `androidx.datastore.preferences`.
- Managed via `PreferencesManager.kt` and editable in real-time through `SettingsDialog.kt`.

---

## 6. Speech-to-Text (Voice Input) Modality

Speech recognition is implemented using Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and launched via Compose's modern activity result launcher:
```kotlin
val speechRecognizerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val recognizedText = matches?.firstOrNull() ?: ""
        if (recognizedText.isNotEmpty()) {
            viewModel.onVoiceResult(recognizedText)
        }
    }
}
```

---

## 7. Automated Testing Suite (Unit & UI)

### 7.1 Unit Tests (`ChatViewModelTest.kt`)
Deterministic unit tests implemented with `kotlinx-coroutines-test` and `FakeGeminiRepository`:
- ✅ `initial_state_has_empty_messages_and_default_settings`
- ✅ `onPromptChange_updates_prompt_correctly`
- ✅ `onVoiceResult_updates_prompt_from_speech_recognition`
- ✅ `onSend_successfully_adds_user_and_assistant_messages`
- ✅ `onSend_handles_failure_and_sets_error_message`
- ✅ `clearChat_empties_conversation_history`
- ✅ `saveSettings_updates_instructions_and_temperature`

```bash
# Command to run Unit Tests:
./gradlew testDebugUnitTest
```

### 7.2 Compose UI Tests (`ChatScreenTest.kt`)
Instrumented Compose UI tests using `createComposeRule()`:
- ✅ `chatScreen_displaysWelcomePlaceholder_whenEmpty`
- ✅ `chatScreen_rendersMessages`
- ✅ `chatScreen_voiceButton_isClickable`

```bash
# Command to run UI Tests:
./gradlew connectedAndroidTest
```

---

## 8. Production Security Analysis ("Know the Limits")

As mandated by Section 3 of the lab manual:
> **"Client-side encryption raises the bar but cannot fully hide a key from a determined attacker."**

### Threat Model:
1. **Dynamic Memory Analysis:** A reverse engineer on a rooted physical device using instrumentation frameworks (e.g., Frida or Xposed) can hook `GenerativeModel.<init>()` or `javax.crypto.Cipher.doFinal()` to capture the decrypted API key from volatile memory.
2. **Reverse Engineering:** While R8 obfuscates method and class names, strings held in memory during network transmission can be intercepted.

### Recommended Production Architecture:
```
+--------------------+   App Check (Play Integrity)   +----------------------+   Google Secret Manager   +-------------------+
|  Android Client    |  ============================> | Backend Proxy Server |  =======================> | Google Gemini API |
| (No Embedded Keys) |  (Authenticates Device & User) | (Cloud Functions/Ktor|  (Holds Server API Key)   |                   |
+--------------------+                                +----------------------+                           +-------------------+
```
1. **Backend Proxy Server:** Eliminate API keys from mobile binaries entirely. All calls route through a backend service that verifies client authentication and attaches the API key from a secure secret manager.
2. **Firebase App Check & Play Integrity:** Verifies that requests originate solely from genuine, untampered instances of the app on real Android hardware.
3. **Google Cloud Restrictions:** Apply IP/Server restrictions to backend keys and package name (`com.devraj.geminiassistant`) + SHA-256 fingerprint restrictions to client endpoints.

---

## 9. Submission & Git Commands

```bash
# 1. Switch to branch with student roll number
git checkout -b N066_assignment_1

# 2. Stage all assignment files (local.properties is git-ignored)
git add .

# 3. Commit with descriptive message
git commit -m "feat(N066): complete Lab Assignment 1 with Keystore AES-256 encryption, Room persistence, and Voice STT"

# 4. Push to student GitHub repository
git push -u origin N066_assignment_1
```

---

## 10. Conclusion

This project submitted by **Devraj Murari (Roll Number: N066)** completes 100% of all requirements specified in the SVKM's NMIMS University Mobile Application Development Lab Manual (Subject Code: `702AI0E002`).
