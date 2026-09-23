# SVKM's NMIMS University
## School of Technology Management & Engineering, Mumbai
### Department of Computer Engineering & Information Technology
---

# LAB ASSIGNMENT - 1 REPORT
### Course: Mobile Application Development (Subject Code: 702AI0E002)
**Academic Year:** 2024–2025 / 2025–2026  

| Field | Details |
| :--- | :--- |
| **Student Name** | Devraj Murari |
| **Roll Number** | N066 |
| **GitHub Profile** | [https://github.com/dev-raj-murari](https://github.com/dev-raj-murari) |
| **Student Repository** | [https://github.com/dev-raj-murari/GeminiApiComposeStarter](https://github.com/dev-raj-murari/GeminiApiComposeStarter) |
| **Submission Branch** | `N066_assignment_1` |
| **Package Name** | `com.devraj.geminiassistant` |

---

## 📑 Table of Contents
1. [Objective & Problem Statement](#1-objective--problem-statement)
2. [Architecture & System Design](#2-architecture--system-design)
3. [Security & Key Management Implementation](#3-security--key-management-implementation)
4. [User Interface & Jetpack Compose Enhancements](#4-user-interface--jetpack-compose-enhancements)
5. [Functionality & Persistence Enhancements](#5-functionality--persistence-enhancements)
6. [Testing & Quality Assurance](#6-testing--quality-assurance)
7. [Production Security Analysis ("Know the Limits")](#7-production-security-analysis-know-the-limits)
8. [Git & GitHub Submission Commands](#8-git--github-submission-commands)
9. [Conclusion](#9-conclusion)

---

## 1. Objective & Problem Statement

The goal of this assignment is to develop an AI Assistant application for Android leveraging Google's Generative AI SDK, implemented by **Devraj Murari (Roll No: N066)** while strictly adhering to:
- **Mandatory API Key Security:** Prevention of credential leaks via version control, zero hardcoding, Android Keystore AES-256-GCM hardware encryption at rest, on-demand in-memory decryption, and R8 obfuscation.
- **Modern Jetpack Compose UI/UX:** Material 3 theming, LazyColumn with stable keys, animated auto-scrolling, state hoisting (Unidirectional Data Flow), and multi-window responsive layouts (`WindowSizeClass`).
- **Persistence & Multimodality:** Room Database for persistent chat history surviving restarts, Preferences DataStore for AI settings, and Speech-to-Text (voice input) via Android `RecognizerIntent`.
- **Automated Testing:** Unit testing with `kotlinx-coroutines-test` and UI testing with `createComposeRule()`.

---

## 2. Architecture & System Design

The application follows the recommended **Android Architecture Components (MVVM + Repository Pattern)** with clean separation under package `com.devraj.geminiassistant`:

```
+---------------------------------------------------------------+
|                        UI LAYER (Compose)                    |
|  - MainActivity.kt (WindowSizeClass calculation)              |
|  - ChatScreen.kt (Responsive Scaffold, LazyColumn)            |
|  - ChatBubble.kt (Material 3 stylized cards)                  |
|  - ChatInputBar.kt (Speech-to-Text & Send Action)             |
|  - SettingsDialog.kt (Custom System Instructions / Temp)      |
+---------------------------------------------------------------+
                               |
                   Collects StateFlow / Sends Events
                               v
+---------------------------------------------------------------+
|                      VIEWMODEL LAYER                          |
|  - ChatViewModel.kt (StateFlow<ChatUiState>, UDF Pattern)     |
|  - ChatUiState.kt (Immutable State Model)                     |
+---------------------------------------------------------------+
                               |
                       Coroutines / Flow
                               v
+---------------------------------------------------------------+
|                     REPOSITORY LAYER                          |
|  - GeminiRepository.kt (GeminiRepositoryImpl)                 |
+---------------------------------------------------------------+
       |                                   |             |
       v                                   v             v
+-----------------------+     +-------------------+ +---------------+
|  Google Generative AI |     |   Room Database   | | DataStore &   |
|  SDK (GenerativeModel)|     | (ChatMessageDao / | | Keystore      |
|  (In-Memory API Key)  |     |  AppDatabase)     | | (AES-256-GCM) |
+-----------------------+     +-------------------+ +---------------+
```

---

## 3. Security & Key Management Implementation

### 3.1 Zero-Leakage & Build Configuration
The API key is strictly isolated from Git version control:
- Listed in `.gitignore` to prevent accidental staging.
- Provided `local.properties.example` for team onboarding.
- Dynamic fallback in `app/build.gradle.kts`:
  ```kotlin
  val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
      ?: System.getenv("GEMINI_API_KEY") ?: ""
  ```

### 3.2 Hardware-Backed AES-256-GCM Keystore Encryption
`KeystoreHelper.kt` generates an AES-256 key inside the `AndroidKeyStore` provider:
- **Encryption at Rest:** On first startup, `SecureKeyStorage.kt` encrypts the API key and stores only the Base64 ciphertext and IV.
- **In-Memory Decryption:** Plaintext is decrypted purely in volatile memory when `GenerativeModel` is instantiated and is never logged or exposed.

---

## 4. User Interface & Jetpack Compose Enhancements

### 4.1 LazyColumn with Stable Keys & Auto-Scroll
- Render conversation as a `LazyColumn` of Material 3 chat bubbles with stable keys (`key = { message.id }`).
- Auto-scrolls smoothly to the latest message using `rememberLazyListState()` and `LaunchedEffect`.

### 4.2 State Hoisting & Unidirectional Data Flow (UDF)
- `ChatUiState` represents the immutable state collected lifecycle-safely:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### 4.3 Responsive Multi-Window Layout (`WindowSizeClass`)
- **Compact Screens (Phones):** Single-column layout with fixed top bar and bottom input bar.
- **Medium & Expanded Screens (Tablets & Foldables):** Two-pane layout with an AI Parameters & Settings sidebar on the left and conversation stream on the right.

---

## 5. Functionality & Persistence Enhancements

### 5.1 Speech-to-Text (Voice Input)
- Integrated using Compose's `rememberLauncherForActivityResult` with `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.

### 5.2 Room Database Offline Persistence
- `ChatMessageEntity`: Stores message ID, text content, participant flag (`isUser`), timestamp, and error states.
- `ChatMessageDao`: Exposes reactive Flow queries (`getAllMessagesFlow()`) and transaction methods (`insertMessage()`, `clearAllMessages()`).
- Data automatically survives application restarts and configuration changes.

### 5.3 Preferences DataStore
- Stores custom system prompts and temperature settings using `androidx.datastore.preferences`.
- Accessible and editable via the `SettingsDialog` composable.

---

## 6. Testing & Quality Assurance

### 6.1 Unit Tests (`ChatViewModelTest.kt`)
Tests written using `kotlinx-coroutines-test` and `FakeGeminiRepository`:
- ✅ `initial_state_has_empty_messages_and_default_settings`
- ✅ `onPromptChange_updates_prompt_correctly`
- ✅ `onVoiceResult_updates_prompt_from_speech_recognition`
- ✅ `onSend_successfully_adds_user_and_assistant_messages`
- ✅ `onSend_handles_failure_and_sets_error_message`
- ✅ `clearChat_empties_conversation_history`
- ✅ `saveSettings_updates_instructions_and_temperature`

**Execution Command:**
```bash
./gradlew testDebugUnitTest
```

### 6.2 Compose UI Tests (`ChatScreenTest.kt`)
Tests written using `createComposeRule()`:
- ✅ `chatScreen_displaysWelcomePlaceholder_whenEmpty`
- ✅ `chatScreen_rendersMessages`
- ✅ `chatScreen_voiceButton_isClickable`

**Execution Command:**
```bash
./gradlew connectedAndroidTest
```

---

## 7. Production Security Analysis ("Know the Limits")

As mandated by Section 3 of the lab manual:
> **"Client-side encryption raises the bar but cannot fully hide a key from a determined attacker."**

### Risk Assessment & Recommendations:
1. **Memory Dumping & Instrumentation (Frida / Xposed):** A reverse engineer on a rooted device can hook `javax.crypto.Cipher.doFinal()` or `GenerativeModel.<init>()` to capture the decrypted key from RAM during runtime.
2. **Backend Proxy:** Remove all API keys from client binaries. Route requests through a secure server that authenticates users and communicates with Gemini using secret keys stored in Google Cloud Secret Manager.
3. **Firebase App Check & Play Integrity API:** Verifies that incoming traffic originates exclusively from authentic, untampered app instances on genuine Android hardware.
4. **Google Cloud Restrictions:** Apply package name (`com.devraj.geminiassistant`) + SHA-256 fingerprint restrictions to any client-facing keys.

---

## 8. Git & GitHub Submission Commands

```bash
cd "C:\Users\Devraj\AndroidStudioProjects\GeminiApiComposeStarter"
git checkout -b N066_assignment_1
git add .
git commit -m "feat(N066): complete Lab Assignment 1 with Keystore AES-256 encryption, Room persistence, and Voice STT"
git push -u origin N066_assignment_1
```

---

## 9. Conclusion

This implementation by **Devraj Murari (Roll No: N066)** meets 100% of the specifications set forth in SVKM's NMIMS University Mobile Application Development Lab Manual (Subject Code: `702AI0E002`).
