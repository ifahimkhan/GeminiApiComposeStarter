# GeminiApiComposeStarter — Conversational AI Android App

**Student:** Ritunjay Deo  
**Roll Number:** N023  
**Course:** Mobile Application Development (MAD) Lab Assignment 1  
**Institution:** Mukesh Patel School of Technology Management & Engineering (NMIMS), Mumbai  
**Fork Repository:** [AkkiSensei/GeminiApiComposeStarter](https://github.com/AkkiSensei/GeminiApiComposeStarter.git)  
**Instructor Upstream:** [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter.git)  
**Branch:** `N023-ritunjay-deo`  

---

## 1. Project Overview

`GeminiApiComposeStarter` is a production-grade, conversational Android application built with **Jetpack Compose**, **Material 3**, and Google's **Gemini Generative AI Android SDK**.

Originally a single-turn prompt/response starter, the application has been re-architected into a multi-turn conversational AI client supporting voice dictation, persistent message history, user preference storage, dark/light theme switching with dynamic color support, adaptive tablet/landscape layouts, Android KeyStore-backed AES-256-GCM credential encryption at rest, and R8 release minification.

---

## 2. Key Features

- **Multi-Turn Conversational Chat:** Full chat interface featuring Material 3 message bubbles, participant role distinction (User vs. Assistant), markdown formatting support (bold headers/spans), and unique, stable message identifiers.
- **Voice-to-Text Input:** Built-in microphone trigger using Android's system `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` to transcribe spoken speech directly into the input bar without auto-sending.
- **Credential Security:** Android KeyStore-backed AES-256-GCM encryption with fresh initialization vectors (IV) for encrypting the Gemini API key at rest in Preferences DataStore (`gemini_secure_prefs.preferences_pb`).
- **Offline Persistence (Room):** Local SQLite database powered by Room to store message history chronologically with deterministic tie-breaking, survivor of configuration changes and process termination.
- **User Preferences (DataStore):** Dedicated Preferences DataStore for managing application theme mode (`SYSTEM`, `LIGHT`, `DARK`) isolated from cryptographic security keys.
- **Adaptive Responsive Layouts:** Optimized for compact phones, medium foldables/tablets, and expanded large screens using `WindowWidthSizeClass`.
- **Automated Test Suite:** 93 automated tests (73 JVM unit tests + 20 connected instrumented tests) with 100% pass rate covering repository seams, retry mechanics, sealed error classification, Room persistence, DataStore, markdown formatting, key rotation, and Compose UI interactions.

---

## 3. Architecture & Tech Stack

The app adheres to modern Android Architecture Components and Unidirectional Data Flow (UDF):

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer                            │
│  MainActivity ──> ChatRoute ──> ChatScreen (Compose M3) │
│       ▲                                                 │
│       │ collects StateFlow<ChatUiState>                 │
│       │                                                 │
│  ChatViewModel (State Holder)                           │
└───────┬───────────────────────────────────┬─────────────┘
        │                                   │
        ▼                                   ▼
┌─────────────────────────┐   ┌───────────────────────────┐
│     Data Layer (AI)     │   │   Data Layer (Storage)    │
│  GeminiRepositoryImpl   │   │  ChatHistoryRepository    │
│           │             │   │       │ (Room SQLite)     │
│  ApiKeyProvider         │   │  UserPreferencesRepo      │
│  (CryptoManager +       │   │       │ (Preferences      │
│   DataStore AES-GCM)    │   │          DataStore)       │
└─────────────────────────┘   └───────────────────────────┘
```

- **Language:** Kotlin 2.0.21
- **UI Toolkit:** Jetpack Compose (BOM 2024.09.00) with Material 3 Design
- **Asynchronous Flow:** Kotlin Coroutines 1.9.0 & StateFlow / SharedFlow
- **AI SDK:** `com.google.ai.client.generativeai:generativeai:0.9.0`
- **Database:** AndroidX Room 2.6.1 with KSP
- **Key-Value Store:** AndroidX Preferences DataStore 1.1.1
- **Security:** AndroidKeyStore, AES-256-GCM (`Cipher`), 128-bit authentication tag
- **Target SDK:** Android 16 (API 36) | **Min SDK:** Android 8.0 (API 26)

---

## 4. Setup & Local Configuration

### Prerequisites
- Android Studio Otter 3+ or later (per Android docs for AGP 9)
- JDK 17 (required by AGP 9.0 and Gradle 9.1+)
- Gradle 9.1+ (managed via Gradle wrapper `gradle-9.3.1`)
- Android SDK Platform 36

### Step 1: Clone the Repository
```bash
git clone -b N023-ritunjay-deo https://github.com/AkkiSensei/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
```

### Step 2: Configure Gemini API Key
Obtain an API key from [Google AI Studio](https://aistudio.google.com/).

Create a file named `local.properties` in the project root directory (if not already present) and add your key:

```properties
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

> **Note:** `local.properties` is strictly ignored by Git (`.gitignore`) and will never be committed to source control.

### Step 3a: Configure the Pre-Commit Secret Scanner
The repository includes a pre-commit hook in `.githooks/` that blocks accidental key commits.
Enable it with a one-time command per clone:
```bash
git config core.hooksPath .githooks
```

### Step 3b: CI/CD Environment Variable Fallback
For headless builds or CI/CD pipelines (e.g. GitHub Actions), the build system automatically falls back to an environment variable if `local.properties` is missing or blank:

```bash
export GEMINI_API_KEY="YOUR_API_KEY_HERE"
```

The Gradle configuration in `app/build.gradle.kts` resolves the key safely:
```kotlin
val geminiApiKey: String = run {
    val keyFromProps = rootProject.file("local.properties").takeIf { it.exists() }?.let { file ->
        Properties().apply {
            file.inputStream().use { load(it) }
        }.getProperty("GEMINI_API_KEY")?.trim()
    }
    keyFromProps?.ifBlank { null }
        ?: System.getenv("GEMINI_API_KEY")?.trim()
        ?: ""
}
```

---

## 5. Security & Encryption Architecture

### Encryption-at-Rest Flow
1. **Key Generation:** On first launch, `CryptoManager` generates an AES-256 key inside the `AndroidKeyStore` (alias `gemini_api_key_alias`). All cryptographic operations (AES-GCM encryption/decryption) take place via the KeyStore provider without exposing raw master key material.
2. **Encryption:** `CryptoManager.encrypt()` uses `AES/GCM/NoPadding` with a fresh 12-byte random IV for every encryption call, producing a ciphertext and an authentication tag (128-bit).
3. **Storage:** `SecureApiKeyProvider` saves the base64-encoded ciphertext, IV, and a SHA-256 fingerprint of the BuildConfig key into Preferences DataStore (`gemini_secure_prefs.preferences_pb`). Plaintext is never stored in SharedPreferences, SQLite, or unencrypted DataStore files.
4. **Decryption:** The key is decrypted into memory strictly on demand within an IO coroutine dispatcher (`Dispatchers.IO`), then cached in memory for the process lifetime.
5. **Key rotation (§2.1):** `SecureApiKeyProvider` stores a SHA-256 fingerprint of the BuildConfig key. On each app launch, if the fingerprint of the current BuildConfig key differs from the stored fingerprint, the old ciphertext is discarded and the new key is encrypted and reseeded automatically — so changing `local.properties` and rebuilding always takes effect on the next launch.
6. **Recovery:** If ciphertext or IV is corrupted, `SecureApiKeyProvider` catches `AEADBadTagException` / `GeneralSecurityException` safely and re-seeds from BuildConfig.

### Client-Side Security Limitations & Considerations
> [!WARNING]
> **Client-Side Encryption Notice:**
> - The API key is baked into the APK as a `BuildConfig` string constant. R8 renames classes but does **not** encrypt string literals, so tools like `jadx` can recover the BuildConfig value from an APK. The DataStore ciphertext is seeded from that plaintext value, so the encryption defends against file-system access (e.g., non-rooted backups or ADB pull), not against APK decompilation by an attacker with a copy of the APK.
> - The KeyStore key is generated without `StrongBox` (`setIsStrongBoxBacked(true)`), so hardware-backed isolation is not guaranteed on all devices.
> - Code minification (R8) raises the barrier to reverse-engineering but does not provide absolute security.
> - **Production Recommendation:** For production apps, credentials should never be embedded in the client binary. Use Firebase AI Logic + App Check (Google's recommended path) or an authenticated backend proxy where keys are stored in server-side secret managers.

---

## 6. Room Chat History Persistence

- **Entity:** `ChatMessageEntity` records `id`, `text`, `participant` (`USER` or `ASSISTANT`), and `timestamp`.
- **DAO:** `ChatDao` provides reactive `Flow<List<ChatMessageEntity>>` queries sorted deterministically by `timestamp ASC, id ASC`.
- **Main-Safe Operations:** `ChatHistoryRepositoryImpl` wraps Room database calls and entity-to-domain mapping in `Dispatchers.IO`.
- **State Synchronization:** `ChatViewModel` observes the database flow and updates `ChatUiState.messages`. Both user prompts and assistant replies are committed to database upon generation.

---

## 7. Preferences DataStore & Dark Mode

- **Single Instance:** Managed via `Context.appDataStore` delegate in `AppDataStore.kt` using file `"gemini_secure_prefs"`.
- **Key Isolation:** User theme mode is stored under `KEY_THEME_MODE`, completely isolated from security keys `KEY_CIPHERTEXT` and `KEY_IV`.
- **Theme Modes:**
  - `ThemeMode.SYSTEM` (Default): Respects the system-wide dark/light theme setting.
  - `ThemeMode.LIGHT`: Forces Material 3 light color scheme.
  - `ThemeMode.DARK`: Forces Material 3 dark color scheme.
- **Dynamic Color:** Enabled on Android 12+ (API 31+) devices via `dynamicDarkColorScheme` and `dynamicLightColorScheme`.

---

## 8. Voice Input Architecture

- **Trigger:** A microphone icon button is positioned beside the send button in the input bar.
- **Contract:** Launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `rememberLauncherForActivityResult`.
- **Non-Destructive Behavior:** Spoken speech is appended to any existing prompt text separated by a space, allowing the user to review, edit, or append to their message before sending.
- **Error Handling:** If speech services are unavailable (e.g., minimal emulator images or disabled Google speech services), `onVoiceError()` displays a friendly notification without crashing.

---

## 9. Responsive Layouts

Using `WindowWidthSizeClass`, the chat screen adapts across device form factors:

| Form Factor | WindowWidthSizeClass | Bubble Max Width | Horizontal Padding | Container Constraint |
|:---|:---|:---|:---|:---|
| **Compact** (Phones) | `Compact` (< 600dp) | 300 dp | 16 dp | Full width |
| **Medium** (Foldables / Tablets portrait) | `Medium` (600dp – 840dp) | 440 dp | 24 dp | Max 640 dp centered |
| **Expanded** (Tablets landscape / Desktop) | `Expanded` (≥ 840dp) | 540 dp | 32 dp | Max 800 dp centered |

---

## 10. Build, Verification & Testing
 
### Retry & Failover Mechanics
- **Transient Overloaded / Capacity (503):** Automatically retried with a fixed delay of 1.0s for up to 2 attempts.
- **Model Fallback:** If the primary model (`gemini-3.8-flash`) remains overloaded after 2 attempts, or encounters model rate-limit quota exhaustion (429) or model unavailability (404), the repository automatically falls back to secondary model (`gemini-3-flash-preview`).
- **Non-Retryable Errors:** Authentication/API key invalidation, bad request parameters (400), content blocked policies, and disabled voice services fail immediately with user-friendly error banners without repeating doomed network calls.

### Running Unit Tests
```bash
.\gradlew.bat testDebugUnitTest --rerun
```
*Current test suite: **73 passing unit tests** (0 failures, 0 errors, 0 skipped) across `GeminiRepositoryTest` (21), `ChatViewModelTest` (27), `BoldMarkdownTest` (6), `ApiKeyProviderUnitTest` (7), `UserPreferencesRepositoryTest` (7), `ChatHistoryRepositoryTest` (3), and `ChatMessageEntityTest` (2).*

### Running Instrumented Device Tests
```bash
.\gradlew.bat connectedDebugAndroidTest
```
*Current test suite: **20 passing instrumented tests** (0 failures, 0 errors, 0 skipped) executed on active emulator across `ChatScreenTest` (9), `CryptoManagerTest` (5), `SecureApiKeyProviderTest` (3), `ChatDaoTest` (2), and `ExampleInstrumentedTest` (1).*
*Total verified automated test suite: **93 tests** (73 JVM unit + 20 connected instrumented).*

### Building Debug APK
```bash
.\gradlew.bat assembleDebug
```
*Output: `app/build/outputs/apk/debug/app-debug.apk`*

### Compiling Instrumented Tests
```bash
.\gradlew.bat assembleDebugAndroidTest
```
*Output: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`*

### Building Release APK with R8
```bash
.\gradlew.bat assembleRelease
```
*Output: `app/build/outputs/apk/release/app-release.apk` (debug-signed; Size: ~2.1 MB)*  
*Mapping files: `app/build/outputs/mapping/release/mapping.txt`*

---

## 11. ProGuard / R8 Rules

Release builds enforce `isMinifyEnabled = true`. The targeted keep rules in `app/proguard-rules.pro` include:

```proguard
# Preserve line numbers and source file names for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room persistence: preserve RoomDatabase implementations, Entity fields, and DAO definitions
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class **_Impl extends androidx.room.RoomDatabase {
    <init>();
}
-keep class com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity { *; }
-keep interface com.fahim.geminiApiComposeStarter.data.local.ChatDao { *; }
-keep class * implements com.fahim.geminiApiComposeStarter.data.local.ChatDao {
    <init>(...);
}

# User preferences: preserve ThemeMode enum entries and valueOf method for DataStore string persistence
-keep enum com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode { *; }

# Chat UI models used in StateFlow and Room mappings
-keep class com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage { *; }
-keep enum com.fahim.geminiApiComposeStarter.ui.chat.Participant { *; }
```

---

## 12. Manual Verification Checklist

The following manual verification tests are documented for on-device testing:

| # | Test Scenario | Procedure | Expected Result |
|---|---|---|---|
| 1 | **Live Gemini API** | Enter API key in `local.properties`, send "Hello Gemini". | Receives coherent generative AI response from `gemini-3.8-flash`. |
| 2 | **Multi-Turn Chat** | Ask a follow-up question. | Chat history displays alternating user and assistant bubbles. |
| 3 | **Voice Dictation** | Tap mic button, speak a prompt. | Speech is transcribed into input field without auto-sending. |
| 4 | **Room Process Death** | Send messages, terminate app process via ADB/task manager, reopen. | Previous chat history is immediately restored from Room database. |
| 5 | **DataStore Theme** | Select Dark theme, restart app. | Dark theme remains active upon app launch. |
| 6 | **Offline Graceful Failure** | Turn on Airplane Mode, send message. | Snackbar displays "Network connection error", offers Retry button. |
| 7 | **Responsive UI** | Rotate device to landscape or run on tablet. | Content centers ergonomically within adaptive width bounds. |
| 8 | **Release APK Launch** | Install `app-release.apk` via `adb install app/build/outputs/apk/release/app-release.apk`. | App boots without R8 reflection crashes. |

---

## 13. Known Limitations

1. **Client-Side API Key in APK (`BuildConfig`)**:
   The Gemini API key is supplied via `local.properties` and compiled into `BuildConfig.GEMINI_API_KEY`. While stored encrypted-at-rest in DataStore via KeyStore AES-256-GCM, any attacker with physical access to the APK file can decompile bytecode (e.g. via Jadx or apktool) to recover the initial build-time key literal. Production deployments should proxy requests through an authenticated backend server or use Firebase AI Logic with App Check.
2. **StrongBox Hardware Security Not Enforced**:
   `CryptoManager` uses the standard `AndroidKeyStore` provider. StrongBox Keymaster (`setIsStrongBoxBacked(true)`) is not requested, allowing compatibility across all standard devices and emulators that lack dedicated tamper-resistant Secure Element hardware.
3. **Single Global Conversation History**:
   The Room database maintains a single sequential timeline of messages. Multiple distinct conversation sessions, thread branching, and cloud synchronization are not implemented in this version.
4. **Virtual Emulator Audio Constraints**:
   While the speech recognition intent (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) and error handling are fully verified, live acoustic voice-to-text dictation requires an active speech engine and physical microphone input configured on the host or physical device.

---

## 14. License & Academic Integrity

This project is submitted as part of coursework for NMIMS MPSTME Mobile Application Development (Lab Assignment 1).  
Student Name: **Ritunjay Deo** | Roll Number: **N023**.  
Base code provided by instructor **Fahim Khan** (`ifahimkhan/GeminiApiComposeStarter`).
