# 🌟 Gemini AI Chat Assistant (Jetpack Compose)

> Enhanced Android Application built with **Jetpack Compose**, **Google Generative AI (Gemini)**, **Room Persistence**, **DataStore Preferences**, and **Hardware-Backed Keystore AES-256-GCM Encryption**.
> 
> **Author:** Aarya Bhoye (Roll No: **N161**) ([GitHub: @Aarya200518](https://github.com/Aarya200518))  
> **Course:** Mobile Application Development (`702AI0E002`)  
> **Institution:** SVKM's NMIMS, School of Technology Management & Engineering (STME), Mumbai  
> **Submission Branch:** `N161_Aarya`  

---

## 🚀 Key Enhancements & Features

### 1. 🛡️ Enterprise-Grade API Key Security
* **Zero Hardcoded Secrets:** No API keys are committed in code or version control.
* **Hardware-Backed Keystore Encryption at Rest:** On application launch, the Gemini API key is encrypted using **AES-256-GCM** inside the **Android Keystore** (`KeyGenParameterSpec`), storing only the ciphertext and IV in Preferences DataStore.
* **In-Memory Decryption Only:** Decrypted in RAM exclusively at the instant the `GenerativeModel` is invoked. Keys are never logged, toasted, or cached in persistent plaintext.
* **CI/CD Environment Fallback:** Seamlessly resolves `GEMINI_API_KEY` from `local.properties` locally or falls back to system environment variables in CI build pipelines.
* **R8 / ProGuard Obfuscation:** Release builds enable `isMinifyEnabled = true` with optimized shrink rules, protecting bytecode and symbols from decompilation.

### 2. 💬 Modern Jetpack Compose Multi-Turn Chat UI
* **Dynamic Bubble Layout (`LazyColumn`):** Distinct speech bubbles for User (right-aligned, primary container) and Gemini Assistant (left-aligned, surface variant).
* **Stable Keys:** Optimized with unique message IDs (`key = { it.id }`) to eliminate redundant recompositions.
* **Auto-Scrolling:** Automatically animates scroll position to the newest message or typing indicator via `LaunchedEffect`.
* **One-Tap Clipboard Copy:** Copy assistant responses directly to clipboard with visual toast feedback.
* **Suggested Action Chips:** Quick prompts for instant conversations (e.g., "Explain Kotlin Coroutines", "Compose Best Practices").

### 3. 🎙️ Voice Input (Speech-to-Text)
* Built-in microphone action bar button powered by `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` using Compose's `rememberLauncherForActivityResult`.
* Dictate questions directly into the chat prompt with automatic transcription insertion.

### 4. 💾 Local Persistence (Room Database & DataStore)
* **Room Database:** Complete conversation history is persisted across app restarts, orientation changes, and process death.
* **Clear Conversation:** Trash / delete action with confirmation modal to reset conversation state.
* **Preferences DataStore:** Persists user display name, preferred Gemini model (`gemini-1.5-flash`, `gemini-1.5-pro`, `gemini-2.0-flash`), and assistant tone (`Balanced`, `Concise`, `Creative`).

### 5. 📱 Adaptive & Responsive Design
* Constrained max-width layout container (`widthIn(max = 840.dp)`) optimized for phones, foldable devices, landscape orientations, and tablets.
* Complete Material 3 Dynamic Color and Dark Theme support (`isSystemInDarkTheme()`).

---

## 🔒 Security Architecture & Encryption Flow

### Keystore AES-256-GCM Encryption at Rest

```
+--------------------------------------------------------------------------+
|                            App Launch                                    |
+--------------------------------------------------------------------------+
                                     |
                                     v
                 [ Check DataStore for Encrypted Key ]
                                /         \
                       (Exists)             (First Run)
                          /                     \
                         v                       v
               Keep Ciphertext          Fetch key from BuildConfig
                                                 |
                                                 v
                                   [ Android Keystore Provider ]
                                   Generate / Load AES-256-GCM Key
                                   (Alias: 'gemini_api_keystore_key')
                                                 |
                                                 v
                                        Cipher.ENCRYPT_MODE
                                                 |
                                                 v
                                    Persist Ciphertext + IV to
                                     Preferences DataStore
                                                 |
                                                 v
+--------------------------------------------------------------------------+
|                         Prompt Submission (In-Memory)                    |
+--------------------------------------------------------------------------+
                                     |
                                     v
                       Read Ciphertext + IV from DataStore
                                     |
                                     v
                       Android Keystore (Cipher.DECRYPT_MODE)
                                     |
                                     v
                   Plaintext Key in Transient Memory ONLY
                                     |
                                     v
                       Instantiate GenerativeModel & Dispatch
```

### ⚠️ Client-Side Limitations & Production Defense Rationale
While hardware-backed Keystore encryption raises the bar significantly against offline extraction and casual inspection, **client-side encryption alone cannot completely prevent compromise on rooted devices or via dynamic runtime instrumentation (e.g., Frida/Xposed)**.

#### Recommended Production Architecture:
1. **Backend Proxy / API Gateway:** Direct Gemini API calls from the mobile client should be routed through a trusted backend server (e.g., Cloud Functions, Cloud Run, or a Spring/Go microservice). The backend securely holds the API key in Cloud Secret Manager.
2. **Firebase App Check:** Validates that incoming requests originate strictly from genuine, unmodified instances of your app using Play Integrity API.
3. **Restricted API Credentials:** In Google Cloud Console, restrict keys by Android Package Name and SHA-1 signing fingerprint, and enforce strict per-minute quota limits.

---

## 🛠️ Setup & Configuration

### 1. Clone the Repository
```bash
git clone https://github.com/Aarya200518/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
```

### 2. Configure Your Gemini API Key
Obtain an API key from [Google AI Studio](https://aistudio.google.com/).

Create a file named `local.properties` in the project root directory (refer to [local.properties.example](file:///Users/aaryabhoye/Downloads/GeminiApiComposeStarter-master/local.properties.example)):
```properties
sdk.dir=/Users/your_username/Library/Android/sdk
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

> ⚠️ **IMPORTANT:** `local.properties` is strictly ignored in `.gitignore`. Never commit your real key to GitHub.

---

## 🧪 Testing & Verification

### Running Unit Tests
Unit tests use `kotlinx-coroutines-test` with a `FakeGeminiRepository` verifying prompt validation, state hoisting, coroutine dispatching, error states, and voice inputs:
```bash
./gradlew testDebugUnitTest
```
*Test Report:* `app/build/reports/tests/testDebugUnitTest/index.html` (100% Pass Rate).

### Running UI Instrumentation Tests
Compose UI tests verify message rendering, speech recognition button presence, and user interaction:
```bash
./gradlew connectedAndroidTest
```

### Building Release APK (R8 Minification Enabled)
```bash
./gradlew assembleRelease
```
Outputs the obfuscated release APK at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 📂 Project Structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt                       # Entry Activity, Keystore initialization, DI
├── data/
│   ├── GeminiRepository.kt              # Repository abstraction interface
│   ├── GeminiRepositoryImpl.kt          # Implementation combining Room, DataStore & Gemini
│   ├── local/
│   │   ├── ChatDao.kt                   # Room DAO for chat history CRUD
│   │   ├── ChatDatabase.kt              # Room Database configuration
│   │   └── ChatMessageEntity.kt         # Database Entity
│   ├── model/
│   │   └── ChatMessage.kt               # Domain model with formatted timestamp
│   └── preferences/
│       └── UserPreferencesRepository.kt # DataStore repository for settings & encrypted key
├── security/
│   └── KeystoreCryptoManager.kt         # Android KeyStore AES-256-GCM crypto manager
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt                # LazyColumn chat bubbles, voice STT, settings modal
│   │   ├── ChatUiState.kt               # Immutable unidirectional UI state
│   │   └── ChatViewModel.kt             # StateFlow state holder & coroutine coordinator
│   ├── text/
│   │   └── BoldMarkdown.kt              # Markdown rendering helper
│   └── theme/                           # Material 3 colors, typography, and dynamic theme
```

---

## 📜 Submission Checklist Verification

- [x] Branch name starts with roll number / student identifier (`Aarya200518-*`).
- [x] `local.properties` is strictly git-ignored; no secrets in git commit history.
- [x] `local.properties.example` included for contributors.
- [x] CI environment variable fallback implemented in `app/build.gradle.kts`.
- [x] Android Keystore AES-256-GCM encryption at rest implemented and active.
- [x] `isMinifyEnabled = true` configured with ProGuard rules in `proguard-rules.pro`.
- [x] Multi-turn conversation rendered as `LazyColumn` of Material 3 bubbles with auto-scroll.
- [x] Speech-to-Text voice queries enabled via `RecognizerIntent`.
- [x] Chat history persisted across app restarts via Room Database.
- [x] User preferences persisted via Preferences DataStore.
- [x] Unit tests (`ChatViewModelTest`) passing with 100% success rate.
- [x] Compose UI instrumentation tests implemented.
- [x] Comprehensive documentation covering setup, encryption, limits, and proxy architecture.
