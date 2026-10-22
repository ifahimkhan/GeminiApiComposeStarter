# Gemini Chat App (Jetpack Compose + Google AI SDK)
### NMIMS Mobile Application Development — Lab Assignment 1

- **Student Name**: Gatha Jain
- **Roll Number**: N038
- **Program / Branch / Batch**: MBA Tech Computer Engineering, Batch D1
- **Course**: Mobile Application Development (Subject Code: 702AI0E002)
- **Institution**: SVKM's NMIMS University, School of Technology Management & Engineering, Mumbai
- **Feature Branch**: `N038_GathaJain-Gemini-Chat-App-Assignment1`
- **Target Repository**: `ifahimkhan/GeminiApiComposeStarter:master`

---

## 1. Setup & API Key Configuration

### Local Development
1. Create a `local.properties` file in the project root (this file is ignored by git):
   ```properties
   sdk.dir=/Users/your_username/Library/Android/sdk
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
2. Obtain your free Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
3. Gradle reads `local.properties` at build time and exposes the key securely via `BuildConfig.GEMINI_API_KEY`. The key is **never** committed to version control.

### CI/CD Pipeline Fallback
In `app/build.gradle.kts`, an environment variable fallback is configured:
```kotlin
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY") ?: ""
```
This enables GitHub Actions and cloud build agents to inject the key from repository secrets without writing files to disk.

---

## 2. Security Architecture & Encryption Flow

### AES-256-GCM Keystore Encryption at Rest
1. **Key Generation**: On the application's first launch, a hardware-backed AES-256 key is generated inside the `AndroidKeyStore` provider using `KeyGenParameterSpec` with `BLOCK_MODE_GCM` and `ENCRYPTION_PADDING_NONE`.
2. **Encryption**: The raw API key is encrypted using AES-256-GCM, producing a 12-byte initialization vector (IV) and ciphertext with a 128-bit authentication tag.
3. **Persistence**: Only the Base64-encoded IV and ciphertext are persisted to disk using Jetpack `Preferences DataStore`.
4. **Decryption in Memory**: The key is decrypted in memory exclusively when `GenerativeModel` is instantiated and is never logged, toasted, or exposed.
5. **Code Obfuscation (R8)**: In release builds, `isMinifyEnabled = true` and `isShrinkResources = true` obfuscate class and field names, preventing trivial reverse engineering.

### Production Security Considerations & Limitations
Client-side encryption safeguards API keys against basic file dumps and casual inspection on unrooted devices. However, client-side encryption **cannot completely prevent extraction by a sophisticated attacker with root access or dynamic instrumentation (e.g., Frida)**.

In a commercial production deployment:
1. **Backend Proxy / BFF Pattern**: Direct calls from mobile clients to the Gemini API should be moved behind a secured backend server (e.g., Google Cloud Functions, Cloud Run, or Firebase Extensions). The mobile client authenticates using Firebase Authentication or OAuth 2.0, and the backend holds the master Gemini API key.
2. **Firebase App Check**: Attests that incoming requests originate genuinely from your authentic Android app binary on an untampered device (via Play Integrity API).
3. **API Key Restrictions**: In Google Cloud Console, restrict API keys by Android application package name and SHA-1 certificate fingerprint.

---

## 3. Architecture & Implemented Features

### Multi-Turn Chat & Room Database Persistence
- **Entity (`ChatMessageEntity`)**: Stores `id`, `text`, `participant` (`USER` / `MODEL`), and `timestamp`.
- **DAO (`ChatDao`)**: Emits real-time query updates via Kotlin Coroutines `Flow<List<ChatMessageEntity>>`.
- **Multi-turn Context**: `GeminiRepository` maps historical conversation messages into Google AI SDK `Content` objects, feeding them into `GenerativeModel.startChat(history = pastContent)`. This preserves conversational memory across questions (e.g., *"Who is the founder of Google?"* followed by *"Who is the CEO of Google?"*).

### Modern Jetpack Compose UI (Material 3)
- **Chat Bubbles**: Distinct Material 3 styled message cards with subtle rounded shapes for User (`primaryContainer`) and AI (`surfaceVariant`).
- **Auto-Scroll**: `LaunchedEffect(messages.size)` automatically scrolls the `LazyColumn` to the latest message.
- **Speech-to-Text (STT)**: Integrated microphone button triggering `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `rememberLauncherForActivityResult`.
- **Dark Mode Support**: Reactive theme switch in the TopAppBar allowing instant toggling between Light and Dark mode.
- **Adaptive Layout**: Adapts layout between Phone and Tablet screens using window size classes (limiting max chat width on tablets for optimal readability).
- **Loading & Error Handling**: Animated `CircularProgressIndicator` during generation, with error notifications displayed via `SnackbarHost`.

---

## 4. Testing & Verification

### Running Automated Tests
- **Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
  Runs `ChatViewModelTest` covering prompt validation, loading states, coroutine dispatcher handling, and error states.
- **Compose UI Tests**:
  ```bash
  ./gradlew connectedAndroidTest
  ```
  Runs the 7 comprehensive tests in `ChatScreenTest`:
  1. `emptyChat_showsPlaceholder`
  2. `typingInPromptField_updatesText`
  3. `whenIdle_sendButtonIsEnabled`
  4. `whileLoading_sendButtonIsDisabled`
  5. `emptyPromptError_isShown`
  6. `sendButton_invokesOnSend`
  7. `messages_areDisplayed`
