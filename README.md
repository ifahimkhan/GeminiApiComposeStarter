# Gemini Jetpack Compose Assistant - Lab Assignment 1

**Student Roll No**: N083  
**Student Name**: Harshit Rai  
**Course/Lab**: Android Mobile Application Development - LAB ASSIGNMENT 1  

---

## 1. Project Overview

This Android application is an AI-powered conversational assistant built using **Jetpack Compose**, the **Google Gemini Generative AI API**, **Room Database** for offline-first persistence, and **Preferences DataStore** for user preferences.

The app is engineered according to Google's official Android Architecture guidelines, following unidirectional data flow (UDF) with MVVM, coroutines, and Material 3 design patterns.

---

## 2. Setup & API Key Configuration

To safeguard secrets, API keys are never hardcoded into source code or committed to version control.

### Step-by-Step Setup:
1. Open or create the `local.properties` file in the project root directory (refer to `local.properties.example` for the template):
   ```properties
   ## local.properties (git-ignored)
   sdk.dir=C:\\Users\\Harshit\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
   ```
2. During the Gradle sync, `app/build.gradle.kts` extracts `GEMINI_API_KEY` from `local.properties` and exposes it via `BuildConfig.GEMINI_API_KEY`.
3. If no key is set or the key is empty, the app will safely notify the user via a Snackbar prompt instead of crashing.

---

## 3. Cryptographic Storage & Security Architecture

### Keystore AES-256-GCM Encryption at Rest
- **Key Generation & Storage**: The app utilizes `SecurityManager.kt` using the **Android KeyStore** provider (`AndroidKeyStore`). A 256-bit AES master key (`KeyProperties.KEY_ALGORITHM_AES`) is generated inside the hardware-backed Trusted Execution Environment (TEE) or StrongBox keymaster.
- **Cipher**: Uses authenticated encryption with `AES/GCM/NoPadding` (`GCMParameterSpec` with a 128-bit authentication tag and a randomized 12-byte initialization vector).
- **At Rest**: `SecureApiKeyStorage.kt` automatically encrypts the API key upon initial run and stores the encrypted ciphertext alongside its IV in private application preferences (`MODE_PRIVATE`).
- **In Memory**: The key is decrypted strictly in-memory when constructing network requests to the Gemini endpoint, preventing plaintext leakage to persistent storage or unencrypted backups.

### Security Limitations of Client-Side Encryption
While Android KeyStore encryption at rest prevents extraction from unrooted file systems and cold storage leaks, client-side encryption has fundamental architectural limitations:
1. **Device Rooting & Frida / Xposed Hooks**: On a compromised or rooted device, an attacker with root privileges or dynamic instrumentation tools (e.g., Frida, Ghidra, Xposed) can hook in-memory functions (such as `getDecryptedApiKey()` or `HttpURLConnection.setRequestProperty`) to intercept the plaintext API key.
2. **Memory Dumps**: When the key is in plaintext memory, an attacker with physical debugging access can inspect the process memory heap.

### Recommended Production Architecture: Backend Proxy & Firebase App Check
To eliminate client-side key exposure in production applications:
- **Backend Proxy Gateway**: The mobile client should authenticate the user and call an organization-managed backend server (e.g., Google Cloud Functions, Cloud Run, or custom API gateway). The backend securely holds the Gemini API key in Secret Manager and forwards prompts to Google AI Studio.
- **Firebase App Check / Play Integrity API**: Protect API endpoints using Google Play Integrity and Firebase App Check. App Check validates that requests originate exclusively from the genuine, untampered mobile application binary running on certified Android hardware, effectively blocking unauthorized scrapers and replay attacks even if client tokens are intercepted.

---

## 4. Architecture & Component Structure

### Architecture Pattern: MVVM + Unidirectional Data Flow (UDF)
```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (ChatScreen, ChatBubble, TopAppBar, InputBar)        │
└───────────────────────────┬────────────────────────────┘
                            │ User Actions (Events)
                            ▼
┌────────────────────────────────────────────────────────┐
│                      ChatViewModel                     │
│  (StateFlow<ChatUiState>, CoroutineScope, Input State) │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
              ▼                            ▼
┌───────────────────────────┐ ┌──────────────────────────┐
│    LocalChatRepository    │ │     GeminiRepository     │
│       (Room Database)     │ │   (REST v1beta Network)  │
│   - ChatDao / ChatMessage │ │ - gemini-3.6-flash       │
└───────────────────────────┘ └──────────────────────────┘
```

### Compose UI Components:
- **`ChatScreen.kt`**: Main container screen featuring a `Scaffold`, `TopAppBar` with message clear action, and an animated message list.
- **`LazyColumn`**: Efficient, virtualized scrolling list that renders messages lazily and automatically animates to the latest message.
- **`BoxWithConstraints`**: Ensures responsive, adaptive layout across standard phones, landscape mode, and large tablet displays by constraining maximum bubble and input widths (e.g., max 650dp centered).
- **`ChatBubble.kt`**: Distinct speech bubbles for user messages (aligned to end with primary container color) and Gemini responses (aligned to start with surface variant tint). Dynamic width wraps to message size up to 520dp.
- **`InputBar`**: Soft-keyboard friendly input row enhanced with `imePadding()` and `navigationBarsPadding()` so the text field smoothly hovers above the on-screen keyboard.
- **Speech-to-Text (`RecognizerIntent`)**: Integrated microphone button launching Android's speech recognition contract with graceful fallback if speech recognition is unavailable on the device.

### Data Persistence:
- **Room Database (`ChatDatabase.kt`, `ChatDao.kt`)**: Persists all user queries and AI responses locally with SQLite for full offline access and fast app restarts.
- **Preferences DataStore (`UserPreferencesRepository.kt`)**: Jetpack DataStore storing user preferences (user name, preferred AI tone, auto-scroll preferences) with transactional, asynchronous Flow updates.

---

## 5. Automated Unit Tests

Unit tests are written with **JUnit 4** and **`kotlinx-coroutines-test`** (`StandardTestDispatcher`, `runTest`, `setMain`/`resetMain`).

### Tested Behaviors in `ChatViewModelTest.kt`:
1. **Initial State Verification**: Ensures empty input text, no active loading state, and null error messages on startup.
2. **Input Handling**: Verifies `updateInput()` updates UI state reactively.
3. **Error Handling**: Verifies errors from failed network calls update `errorMessage` and that `clearError()` resets it.
4. **Blank Input Guard**: Confirms empty or whitespace-only inputs do not trigger network calls.
5. **Successful Conversation Flow**: Tests that sending a prompt triggers loading, clears input, dispatches network request, saves both user and AI messages to the repository, and ends loading.
6. **Chat History Clear**: Validates that calling `clearHistory()` wipes records from the DAO.

### Running the Unit Tests:
Run the tests from terminal or command prompt:
```bash
./gradlew.bat testDebugUnitTest
```
Or on Linux/macOS:
```bash
./gradlew testDebugUnitTest
```

---

## 6. Running on Emulator / Physical Device

1. Connect an Android device with USB debugging enabled or launch an Android Virtual Device (AVD, e.g. Pixel 10 Pro XL, API 34+).
2. Install and launch the application:
   ```bash
   ./gradlew.bat installDebug
   ```
3. Open the **N083HarshitRaiAssignment1** app from the launcher.
