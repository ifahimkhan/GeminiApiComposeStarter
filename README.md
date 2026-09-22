# Gemini AI Jetpack Compose Chat App

**Student Roll Number:** N111  
**Assignment:** Mobile Application Development Lab Assignment-1  
**Branch:** `N111-lab-assignment-1`  
**Base Repository:** [GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## 1. Project Overview

This project is a modern, production-grade Android conversational chat application built with **Jetpack Compose**, **Google Generative AI SDK (Gemini)**, **Room Database**, **Preferences DataStore**, and **Android Keystore AES-256-GCM Encryption**. It follows the official Android Architecture Guidelines with clean unidirectional data flow (UDF) and separation of concerns:

$$\text{Compose UI (ChatScreen)} \longrightarrow \text{ChatViewModel} \longrightarrow \text{Repository Layer} \longrightarrow \text{Gemini SDK / Room DB / DataStore}$$

---

## 2. Features Implemented

- **Gemini Chat Integration**: Interactive conversational AI integration using the official Google Generative AI SDK (`gemini-2.5-flash`).
- **Secure API-Key Handling**:
  - API keys are never hardcoded in Kotlin code, XML layouts, strings, or comments.
  - Keys are loaded from `local.properties` during Gradle build configuration (with `System.getenv("GEMINI_API_KEY")` fallback) and exposed via `BuildConfig.GEMINI_API_KEY`.
  - `local.properties` is strictly tracked in `.gitignore`. A safe template `local.properties.example` is provided.
- **AES-256-GCM Android Keystore Encryption**:
  - Hardware-backed key generation inside `AndroidKeyStore`.
  - On first app launch, `SecureApiKeyManager` encrypts the Gemini API key using AES-256-GCM via `KeystoreCryptoManager`.
  - Only the initialization vector (IV) and encrypted ciphertext are persisted in private SharedPreferences.
  - Decryption occurs purely in-memory when constructing network requests, discarding plaintext keys immediately.
- **Material 3 Chat UI**:
  - Polished Material 3 design with distinct color schemes for user messages (`primaryContainer`) and Gemini responses (`surfaceVariant`).
  - Formatted text and Markdown bold styling support.
  - Message timestamps formatted with localized times.
- **LazyColumn Conversation List**:
  - Efficient virtualized list utilizing stable UUID keys to prevent unnecessary recompositions and list jank.
  - Auto-scrolls smoothly to the latest message whenever new messages arrive.
- **Loading & Error States**:
  - Animated thinking bubble with a Material 3 `CircularProgressIndicator` during Gemini response generation.
  - Duplicate prompt submission prevention while active requests are executing.
  - Interactive `ErrorCard` with **Retry** and **Dismiss** actions.
  - Input field validation preventing empty or whitespace-only submissions with inline error helper text.
- **Responsive Layouts**:
  - Responsive window sizing supporting compact phones, medium unfolded screens, and expanded tablets or landscape orientations.
  - Intelligent horizontal margin scaling and max-width capping for wide screens.
- **Dark Mode & Theming**:
  - Complete Material 3 Dynamic Color support.
  - Explicit user theme switching dialog supporting **System Default**, **Light Theme**, and **Dark Theme**.
- **Voice Input (Speech-to-Text)**:
  - Microphone button seamlessly integrated into the message input bar.
  - Uses Android `RecognizerIntent` launched via `rememberLauncherForActivityResult`.
  - Robust handling of microphone permissions, unavailable speech recognition services, and empty speech results.
  - Inserts recognized speech directly into the input text field without auto-sending.
- **Preferences DataStore**:
  - Jetpack Preferences DataStore persisting user theme preferences and auto-scroll settings reactively with coroutine error fallbacks (`data/preferences/UserPreferencesRepository.kt`).
- **Room Database Chat History**:
  - Structured Room persistence under `data/local/` (`ChatDatabase`, `ChatMessageDao`, `ChatMessageEntity`).
  - Repository abstraction layer (`ChatHistoryRepository`, `ChatHistoryRepositoryImpl`) offloading all queries and database operations to `Dispatchers.IO`.
  - Chat conversations survive app restarts and process recreation.
  - Top app bar menu action to clear chat history with a confirmation dialog.
- **Comprehensive Unit Testing**:
  - 31 unit tests across `ChatViewModelTest`, `ChatHistoryRepositoryImplTest`, `GeminiRepositoryImplTest`, and `AesGcmCryptoTest` using `kotlinx-coroutines-test` and deterministic fake repositories.
- **Compose UI Instrumented Testing**:
  - 8 Compose UI tests (`ChatScreenTest`) using `createComposeRule()` verifying UI rendering, typing, button states, message lists, thinking bubbles, error handling, and voice input.

---

## 3. Setup Instructions

### Prerequisites
- Android Studio Hedgehog / Ladybug or newer
- Android SDK (API Level 36 compile SDK, API 26 minimum SDK)
- Java Development Kit (JDK 17 or higher; JDK 21+ supported)
- A valid Google Gemini API key from [Google AI Studio](https://aistudio.google.com/)

### Step 1: Clone the Repository & Checkout Branch
```bash
git clone https://github.com/ifahimkhan/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
git checkout N111-lab-assignment-1
```

### Step 2: Configure `local.properties`
In the project root directory, create a file named `local.properties` (or copy from `local.properties.example`):
```properties
sdk.dir=C\\:\\\\Users\\\\<username>\\\\AppData\\\\Local\\\\Android\\\\Sdk
GEMINI_API_KEY=your_actual_api_key_here
```

> **IMPORTANT**: If `local.properties` is omitted, the Gradle build configuration falls back to the system environment variable:
> ```bash
> export GEMINI_API_KEY="your_actual_api_key_here"
> ```

### Step 3: Build & Run the App
Open the project in Android Studio and click **Run**, or build from the command line:
```bash
# On Windows (PowerShell)
.\gradlew.bat assembleDebug

# On macOS/Linux
./gradlew assembleDebug
```

---

## 4. Security Architecture & Encryption Flow

```
+-----------------------------------------------------------------------------------+
| 1. Build Phase                                                                    |
|    local.properties / System.getenv("GEMINI_API_KEY")                             |
|    ↳ Injected into BuildConfig.GEMINI_API_KEY (git-ignored, never committed)      |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 2. First Application Launch (MainActivity)                                        |
|    SecureApiKeyManager.initializeFromBuildConfig(...)                             |
|    ↳ Android KeyStore generates hardware-backed AES-256-GCM secret key            |
|    ↳ KeystoreCryptoManager encrypts plaintext API key                             |
|    ↳ Only IV (12 bytes) and Ciphertext are stored in private SharedPreferences    |
|    ↳ Plaintext key is discarded from memory immediately                          |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 3. Runtime API Request (GeminiRepositoryImpl)                                     |
|    ApiKeyProvider.getApiKey()                                                     |
|    ↳ KeystoreCryptoManager decrypts ciphertext using KeyStore key in-memory       |
|    ↳ Ephemeral GenerativeModel instance created strictly for the network call     |
|    ↳ Plaintext key is never logged, printed, or persisted in unencrypted form     |
+-----------------------------------------------------------------------------------+
```

### Why `local.properties` Must Never Be Committed
- `local.properties` contains developer-specific SDK locations and secret API credentials.
- It is explicitly listed in `.gitignore`.
- Committing API keys to version control exposes credentials to automated scrapers, resulting in quota exhaustion, unauthorized account billing, or project revocation.
- A template file (`local.properties.example`) is provided for developers without committing real keys.

---

## 5. Running Tests

### Unit Tests
To run all 31 unit tests (covering ViewModel state transitions, Room repository persistence, Keystore encryption, and repository fallback behavior):
```bash
# Windows
.\gradlew.bat testDebugUnitTest

# macOS/Linux
./gradlew testDebugUnitTest
```
*HTML Test Report Location:* `app/build/reports/tests/testDebugUnitTest/index.html`

### Compose UI Instrumented Tests
To verify and compile Compose UI instrumented tests (`ChatScreenTest`):
```bash
# Compile and assemble Android test APK
.\gradlew.bat compileDebugAndroidTestKotlin assembleDebugAndroidTest

# Run on an attached physical device or running Android emulator
.\gradlew.bat connectedDebugAndroidTest
```

---

## 6. Production Security Limitations & Best Practices

While client-side Keystore encryption protects secrets from at-rest inspection on unrooted devices, mobile client applications inherently have security boundaries:

1. **Client-Side Vulnerabilities**:
   - On rooted devices or emulators, dynamic memory inspection (e.g., Frida, memory dumping, debuggers) can intercept keys when decrypted in memory.
   - Reverse engineering and runtime hooking tools can bypass client-side encryption checks.
2. **Recommended Production Architecture**:
   - **Backend API Gateway / Proxy**: In production, mobile applications should never communicate directly with LLM provider endpoints using a master API key. Instead, client requests should authenticate against a trusted backend service (e.g., Firebase Functions, Cloud Run, Spring Boot) using user tokens (e.g., Firebase Auth / OAuth2). The backend server stores the Gemini API key securely in Secret Manager and forwards requests.
   - **Google Cloud API Key Restrictions**: If direct API calls are necessary, restrict the API key in Google Cloud Console to specific Android package names (`com.fahim.geminiApiComposeStarter`) and SHA-1 signing certificate fingerprints.
   - **App Check / Play Integrity**: Implement Firebase App Check and Google Play Integrity API to ensure only genuine, untampered versions of the app installed via Google Play can access API resources.

---

## 7. Pull Request Screenshots

Attach the following screenshots to the submission pull request:

| # | Feature / Screen | Description | Screenshot Placeholder |
| :-: | :--- | :--- | :---: |
| **1** | **Main Chat Conversation** | User and Gemini messages rendered in Material 3 bubbles with stable keys and timestamps. | `![Chat Conversation](screenshots/01_chat_conversation.png)` |
| **2** | **Voice Input (STT)** | Microphone button active, permission request dialog, and transcribed prompt inserted into input field. | `![Voice Input](screenshots/02_voice_input.png)` |
| **3** | **Loading & Error States** | Thinking bubble with CircularProgressIndicator and ErrorCard with Retry / Dismiss actions. | `![Loading and Error](screenshots/03_loading_error_state.png)` |
| **4** | **Theme Selection & Dark Mode** | Theme selection dialog (System / Light / Dark) and active Dark Theme rendering. | `![Dark Mode](screenshots/04_theme_dark_mode.png)` |
| **5** | **Responsive Layout** | Chat UI adapted for Expanded / Landscape or Tablet screen dimensions. | `![Responsive Layout](screenshots/05_responsive_tablet.png)` |
| **6** | **Test Execution Results** | Terminal / Android Studio view showing all 31 unit tests and Compose test tasks passing. | `![Test Results](screenshots/06_test_results.png)` |
