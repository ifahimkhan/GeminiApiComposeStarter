# Gemini AI Jetpack Compose Chat Application

**Course:** Mobile Application Development  
**Subject Code:** 702AI0E002  
**Assignment:** Lab Assignment-1  
**Student Roll Number:** N051  
**Base Repository:** [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## 1. Project Overview & Features

This project transforms the single-turn starter into a production-grade, conversational Android chat application built with **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Android Architecture Components (MVVM)**, **Room Database**, **Preferences DataStore**, **Android Keystore AES-256-GCM encryption**, and Google's **Gemini Generative AI SDK**.

### Key Implemented Features:
1. **Conversational Multi-Turn Chat UI (`LazyColumn`)**:
   - Distinct, styled Material 3 chat bubbles for User (`primaryContainer`) and Gemini (`surfaceVariant`).
   - Stable keys (`key = { message.id }`) for all message items to prevent unnecessary recompositions.
   - Smooth auto-scrolling to the latest message as new messages arrive.
   - Assistant avatar branding with custom vector art (`ic_assistant.xml`).
   - Formatted bold Markdown rendering (`**bold**`).
   - Welcome/empty-state card with conversation prompts.

2. **Secure API Key Architecture (Android Keystore + AES-256-GCM)**:
   - Root `local.properties` key injection with CI environment variable fallback (`System.getenv("GEMINI_API_KEY")`).
   - `local.properties` is strictly git-ignored; a `local.properties.example` template is provided.
   - Runtime encryption at rest using hardware-backed Android Keystore with an AES-256-GCM key and fresh randomized IVs.
   - Ciphertext and IV persisted in DataStore; key is decrypted strictly in volatile memory at the instant the repository is initialized.
   - Decrypted key is never logged, toasted, printed, or exposed to UI.
   - Release minification and R8 obfuscation enabled (`isMinifyEnabled = true`).

3. **Persistent Conversation History (Room Database)**:
   - All user prompts and Gemini responses are automatically saved to local SQLite via Room (`ChatMessageEntity`, `ChatMessageDao`, `AppDatabase`).
   - Database operations run strictly off the main thread (`Dispatchers.IO`).
   - Chat history seamlessly survives app process death, rotation, and device restarts.
   - Top app bar includes an action to clear conversation history.

4. **Persistent Preferences (Preferences DataStore)**:
   - User theme preferences (Dark Mode / Light Mode) persisted via Preferences DataStore (`UserPreferencesRepository`).
   - TopAppBar quick toggle allows switching between light and dark themes with instant persistence across restarts.

5. **Voice Input (Speech-to-Text)**:
   - Microphone button in prompt bar integrates with Android `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `rememberLauncherForActivityResult`.
   - Populates and appends recognized speech directly into the prompt text field for editing prior to sending.
   - Gracefully handles missing speech recognition services (`ActivityNotFoundException`) without crashing.

6. **Responsive Layouts**:
   - Adaptive sizing for phone portrait, phone landscape, and tablets using `BoxWithConstraints`.
   - Max-width capping on large screens prevents awkward full-screen message bubbles.
   - Edge-to-edge support with proper window insets and IME (keyboard) padding.

7. **Automated Testing Suite**:
   - Comprehensive unit test suite (`ChatViewModelTest`) using `kotlinx-coroutines-test` and fake repositories.
   - Compose UI test suite (`ChatScreenTest`) using `createComposeRule()`.

---

## 2. API Key Configuration & Security Architecture

### Configuration Steps

1. Obtain a Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
2. In the project root, open or create `local.properties` (this file is excluded from Git).
3. Add the following line:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
4. For Continuous Integration (CI) systems, you can alternatively set an environment variable:
   ```bash
   export GEMINI_API_KEY="your_actual_api_key_here"
   ```
   Gradle reads `local.properties` first, falling back to `System.getenv("GEMINI_API_KEY")` in `app/build.gradle.kts`:
   ```kotlin
   val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
       ?: System.getenv("GEMINI_API_KEY")).orEmpty().trim()
   ```

### Encryption Flow Diagram

```
+--------------------------------------------------------------+
|                    First Launch Bootstrap                    |
|                                                              |
|   BuildConfig.GEMINI_API_KEY                                 |
|               |                                              |
|               v                                              |
|   CryptoManager (Android Keystore)                           |
|       - Generates 256-bit AES Key in hardware keystore       |
|       - Encrypts API Key with AES/GCM/NoPadding + Random IV  |
|               |                                              |
|               v                                              |
|   SecureApiKeyStorage (Preferences DataStore)                |
|       - Stores: [ciphertext_base64] & [iv_base64]            |
|       - (Plaintext key is discarded)                         |
+--------------------------------------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                    Runtime Initialization                    |
|                                                              |
|   SecureApiKeyStorage.getDecryptedApiKey()                   |
|       - Reads ciphertext & IV from DataStore                 |
|       - Android Keystore AES-256-GCM decrypts in memory      |
|               |                                              |
|               v (Volatile memory only)                       |
|   GeminiRepositoryImpl(apiKey = decryptedKey)                |
|       - GenerativeModel created                              |
|       - Decrypted key is never logged, toasted, or committed |
+--------------------------------------------------------------+
```

### Limitations of Client-Side Encryption & Production Alternatives
Client-side encryption using the Android Keystore significantly raises the bar against passive inspection and reverse engineering. Combined with R8 minification (`isMinifyEnabled = true`), decompiling the APK will not reveal plaintext strings.

However, **client-side secrets on an untrusted device can never be completely hidden from a determined attacker** who has root access, runtime instrumentation (e.g., Frida, Xposed), or a memory debugger attached to the process.

#### Production Alternatives:
1. **Backend API Proxy / Gateway (Industry Standard)**:
   - The mobile application authenticates users via OAuth2/OpenID (e.g., Firebase Authentication).
   - The app communicates with a secure backend server (e.g., Cloud Functions or a REST API).
   - The backend server validates authentication and rate limits before injecting the Gemini API key from a secure secret manager (e.g., Google Secret Manager) to call Gemini. The API key is never distributed to client devices.
2. **Firebase App Check with Restricted API Keys**:
   - Attests application authenticity using device attestation providers (Play Integrity API on Android).
   - Rejects unauthorized requests from spoofed clients or bots, ensuring only genuine instances of your application can invoke Google AI endpoints.

---

## 3. How to Run the Application in Android Studio

### Prerequisites
- **Android Studio:** Ladybug (2024.2+) or Meerkat (2025.1+) / Hedgehog+.
- **JDK:** Java 17 (bundled with Android Studio or Microsoft JDK 17).
- **Android SDK:** Platform 36 (Android 16 / Vanilla Ice Cream / Tiramisu+ compatible, minSdk 26).
- **Virtual Device:** Android Emulator with API 26 or higher (e.g., Pixel 8).

### Step-by-Step Instructions
1. Launch Android Studio.
2. Select **File > Open...** and navigate to the project directory:
   `GeminiApiComposeStarter-master`
3. Allow Gradle to sync. (The project uses Gradle 9.5 and Android Gradle Plugin 9.0).
4. Verify `local.properties` contains your `GEMINI_API_KEY`.
5. Select the **app** run configuration and your preferred emulator or physical device.
6. Click **Run** (`Shift + F10`) or click the green Play icon.

---

## 4. How to Run Automated Tests

### Unit Tests
The unit test suite verifies `ChatViewModel` state transitions, Room and DataStore repository operations, voice inputs, theme toggles, and Gemini error handling.

To run unit tests via terminal:
```bash
./gradlew testDebugUnitTest
```
Or in Android Studio:
- Navigate to `app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt`
- Right-click the class name and click **Run 'ChatViewModelTest'**.

**Results:**
- 10/10 tests passing (0 failures, 0 errors, 0 skipped).

### Compose UI / Instrumented Tests
The Compose UI tests verify screen composition, prompt input, send button state, message rendering, loading indicators, and snackbar error cards.

To run on a connected device or running emulator:
```bash
./gradlew connectedAndroidTest
```
Or in Android Studio:
- Navigate to `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt`
- Right-click the class name and click **Run 'ChatScreenTest'**.

---

## 5. User Interface Screenshots

| Screen / Feature | Description | Placeholder / Preview |
|---|---|---|
| **Welcome / Empty State** | Displayed on clean launch with welcome card and prompt instructions | `[ Screenshot: Welcome Card with Gemini Assistant Icon ]` |
| **Active Conversation** | User messages (`primaryContainer`, right-aligned) & Gemini messages (`surfaceVariant`, left-aligned with icon) | `[ Screenshot: Multi-turn Chat Conversation ]` |
| **Voice Input Active** | Microphone button launches Android Speech Recognizer | `[ Screenshot: Android Speech-to-Text Input ]` |
| **Dark Theme** | Material 3 Dark Color Scheme toggleable via top bar | `[ Screenshot: Dark Mode Theme ]` |
| **Error Handling** | Material 3 Snackbar displaying network/API error | `[ Screenshot: Error Snackbar Notification ]` |

---

## 6. Git Workflow, Branching & Submission Instructions

### Step 1: Initialize Git and Create Assignment Branch
Ensure your branch name begins with your roll number (e.g., `B067`):
```bash
git init
git checkout -b B067-assignment-1
```

### Step 2: Verify `local.properties` is NOT Staged
Check Git status to ensure no secret API keys are tracked:
```bash
git status
```
*Confirm that `local.properties` does NOT appear under "Changes to be committed".*

### Step 3: Stage and Commit Changes
```bash
git add .
git commit -m "Complete Assignment 1: Secure API key flow, Room conversation history, DataStore preferences, voice input, responsive Compose UI, and automated tests"
```

### Step 4: Configure Remote and Push Branch
Add your GitHub fork as `origin`:
```bash
git remote add origin https://github.com/<your-github-username>/GeminiApiComposeStarter.git
git push -u origin B067-assignment-1
```

### Step 5: Create Pull Request
1. Open your fork on GitHub.
2. Click **Compare & pull request**.
3. Set:
   - **Base repository:** `ifahimkhan/GeminiApiComposeStarter`
   - **Base branch:** `master`
   - **Head repository:** `<your-github-username>/GeminiApiComposeStarter`
   - **Compare branch:** `B067-assignment-1`
4. Provide a description with your roll number and the list of implemented features.
