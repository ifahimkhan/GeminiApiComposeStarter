# Gemini Chat Compose Starter (Android)

**Student Name:** Rannbir Sachdeva  
**Roll Number:** N087  
**Branch:** `n087-assingment1`  
**Application ID:** `com.rannbir.geminiApiComposeStarter`

A modern, secure Android chat application built with **Jetpack Compose**, **Google Gemini Generative AI SDK**, **Room Database** for conversation memory, **Preferences DataStore**, and hardware-backed **Android Keystore (AES-256-GCM)** encryption at rest.

---

## Key Features

1. **Multi-Turn Chat Memory**:
   - Past conversations are saved to a local **Room SQLite Database** (`gemini_chat_memory.db`), allowing conversations to survive app kills and reboots.
   - When communicating with Gemini, conversational history is passed to `GenerativeModel.startChat(history = ...)` so Gemini retains full context across queries.
   - Option to clear conversation history at any time with a confirmation prompt.

2. **Hardware-Backed AES-256-GCM Encryption at Rest**:
   - On initial launch, an AES-256-GCM master key is generated inside the **Android Keystore** (`KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT`).
   - The API key is encrypted into memory-isolated ciphertext with a random 96-bit IV and stored locally.
   - Decryption occurs strictly in-memory at the instant `GenerativeModel` is instantiated.
   - Plaintext keys are never logged, displayed, toasted, or serialized.

3. **Modern Jetpack Compose UI**:
   - **LazyColumn** with distinct chat bubbles (User on right, Gemini on left with avatar).
   - Stable item keys (`key = { message.id }`) preventing unnecessary recompositions.
   - Automatic scrolling to the latest message on submission.
   - Formatted Markdown support (bolding and emphasis).
   - Real-time `CircularProgressIndicator` loading state and Snackbar error handling.
   - Fully responsive layout adapting across phone portrait, landscape, and tablet screen widths.
   - Material 3 dynamic color and dark mode theming.

4. **Speech-to-Text (Voice Input)**:
   - Microphone button in the prompt bar integrated with Android's system `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `rememberLauncherForActivityResult`.

5. **CI/CD & Secret Protection**:
   - `local.properties` is strictly git-ignored.
   - `app/build.gradle.kts` features a fallback to `System.getenv("GEMINI_API_KEY")` for CI workflows.
   - Release build obfuscation enabled (`isMinifyEnabled = true`) with custom R8/ProGuard keep rules.

---

## Getting Started

### 1. Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- Android SDK 36 (minSdk 26)
- JDK 21+
- Android Emulator or physical device

### 2. Configure the Gemini API Key
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. Create or open `local.properties` in the project root directory (refer to `local.properties.example`).
3. Add your key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
   *(Note: `local.properties` is in `.gitignore` and must never be committed to Git).*

### 3. Build & Run
To assemble and install the debug build onto a connected emulator or device:
```powershell
.\gradlew assembleDebug
```
Or run directly from Android Studio by selecting `app` and pressing **Run** (`Shift + F10`).

---

## Security Architecture & Key Management

### Encryption Flow (At Rest)
```
+--------------------+
|  local.properties  | (or CI GEMINI_API_KEY env var)
+---------+----------+
          | [Build-time BuildConfig]
          v
+-----------------------------+
|    ApiKeySecurityManager    |
| - KeyGenParameterSpec       | ---> Generates AES-256-GCM key inside Android Keystore
| - Encrypts with AES/GCM     |
+---------+-------------------+
          |
          v
+-----------------------------+
| Encrypted Storage           | (Ciphertext + 96-bit IV)
+-----------------------------+
          |
          | Decrypted strictly in-memory upon GenerativeModel instantiation
          v
+-----------------------------+
| GenerativeModel(apiKey = *) | ---> Outbound TLS 1.3 requests to Google AI
+-----------------------------+
```

### Production Security Considerations & Limitations
While client-side encryption with Android Keystore protects secrets from other apps on the device and prevents casual decompilation, **client-side secrets cannot be 100% hidden from an attacker with root access, Frida/Xposed hooks, or memory dumping tools**.

For enterprise and production deployments, recommend:
1. **Backend Proxy / BFF (Backend for Frontend)**: Move Gemini API calls behind an authenticated backend server (e.g., Cloud Run, Firebase Functions, Go/Node backend). The Android client authenticates with Firebase Authentication or OAuth, and only the backend holds the Gemini API key.
2. **Firebase App Check**: Validates that incoming requests originate from your authentic, untampered app binary using Play Integrity API.
3. **Google Cloud Restricted API Keys**: Restrict the key in Google Cloud Console by Android package name (`com.rannbir.geminiApiComposeStarter`) and SHA-1 certificate fingerprint.

---

## Running Automated Tests

### Unit Tests
The unit test suite exercises `ChatViewModel` using `kotlinx-coroutines-test` and a standalone `FakeGeminiRepository`:
```powershell
.\gradlew testDebugUnitTest
```
Tests verify:
- Initial idle state and hoisted properties
- Input prompt modification and empty validation (`PromptError.EMPTY`)
- Missing API key error handling
- Successful multi-turn messaging and state updates
- API failure reporting via `errorMessage`
- Voice input speech concatenation
- Clearing chat history

### Compose UI Instrumentation Tests
Run instrumentation tests on an active emulator:
```powershell
.\gradlew connectedDebugAndroidTest
```
Tests verify:
- Welcome empty card display with student details
- Bubble message rendering with proper alignment
- Presence of send and microphone action buttons

---

## Verification & Submission Checklist
- [x] Branch name: `n087-assingment1` (starts with Roll No. N087)
- [x] Author: Rannbir Sachdeva (Roll No: N087)
- [x] Package cleaned of previous names: `com.rannbir.geminiApiComposeStarter`
- [x] `local.properties` ignored and excluded from git commits
- [x] `local.properties.example` created
- [x] Multi-turn Room database chat memory integrated
- [x] Android Keystore AES-256-GCM encryption implemented
- [x] Voice input via `RecognizerIntent` implemented
- [x] Material 3 UI with LazyColumn chat bubbles and auto-scroll
- [x] Unit & Compose UI tests written
