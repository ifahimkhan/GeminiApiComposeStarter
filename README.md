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

Originally a single-turn prompt/response starter, the application has been re-architected into a multi-turn conversational AI client supporting voice dictation, persistent message history, user preference storage, dark/light theme switching with dynamic color support, adaptive tablet/landscape layouts, hardware-backed AES-256-GCM credential encryption at rest, and R8 release minification.

---

## 2. Key Features

- **Multi-Turn Conversational Chat:** Full chat interface featuring Material 3 message bubbles, participant role distinction (User vs. Assistant), markdown formatting support (bold headers/spans), and unique, stable message identifiers.
- **Voice-to-Text Input:** Built-in microphone trigger using Android's system `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` to transcribe spoken speech directly into the input bar without auto-sending.
- **Hardware-Backed Credential Security:** Android KeyStore-backed AES-256-GCM encryption with fresh initialization vectors (IV) for encrypting the Gemini API key at rest in Preferences DataStore.
- **Offline Persistence (Room):** Local SQLite database powered by Room to store message history chronologically with deterministic tie-breaking, survivor of configuration changes and process termination.
- **User Preferences (DataStore):** Dedicated Preferences DataStore for managing application theme mode (`SYSTEM`, `LIGHT`, `DARK`) isolated from cryptographic security keys.
- **Adaptive Responsive Layouts:** Optimized for compact phones, medium foldables/tablets, and expanded large screens using `WindowWidthSizeClass`.
- **R8 Release Hardening:** Full release minification (`isMinifyEnabled = true`) with targeted keep rules for Room and DataStore enums.
- **Automated Test Suite:** 38 unit tests covering repository error mapping, Room entity conversions, DataStore isolation, and ViewModel conversational logic, accompanied by instrumented test suites.

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
- Android Studio Ladybug / Meerkat or later
- JDK 11 or later
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

### Step 3: CI/CD Environment Variable Fallback
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
1. **Key Generation:** On first launch, `CryptoManager` generates an AES-256 key inside the hardware-backed `AndroidKeyStore`. The master key material never leaves hardware boundaries.
2. **Encryption:** `CryptoManager.encrypt()` uses `AES/GCM/NoPadding` with a fresh 12-byte random IV for every encryption call, producing a ciphertext and an authentication tag (128-bit).
3. **Storage:** `SecureApiKeyProvider` saves the base64-encoded ciphertext and IV into Preferences DataStore (`gemini_secure_prefs`). Plaintext is never stored in SharedPreferences, SQLite, or unencrypted DataStore files.
4. **Decryption:** The key is decrypted into memory strictly on demand within an IO coroutine dispatcher (`Dispatchers.IO`).
5. **Recovery:** If ciphertext or IV is corrupted, `SecureApiKeyProvider` catches `AEADBadTagException` / `GeneralSecurityException` safely and re-seeds from BuildConfig.

### Client-Side Security Limitations & Considerations
> [!WARNING]
> **Client-Side Encryption Notice:**
> - Client-side encryption prevents casual extraction of stored credentials from device storage (e.g., via non-rooted backups or physical file extraction).
> - However, **any secret embedded in a client application binary or BuildConfig is inherently vulnerable to reverse-engineering by a determined attacker with root access or dynamic memory instrumentation (e.g., Frida).**
> - Code minification (R8) raises the barrier to reverse-engineering but does not provide absolute security.
> - **Production Recommendation:** For production mobile applications, client apps should never communicate directly with AI model provider APIs using raw API keys. Instead, traffic should route through an authenticated backend proxy (e.g., Firebase App Check + Cloud Functions / Cloud Run) where credentials remain securely stored in server-side secret managers.

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
  - `ThemeMode.DARK`: Forces Material 3 dark color scheme with high contrast.
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

### Running Unit Tests
```bash
.\gradlew.bat testDebugUnitTest --rerun
```
*Current test suite: **38 passing unit tests** across GeminiRepository, ChatViewModel, Room entities, and DataStore.*

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
*Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (Size: ~2.07 MB)*  
*Mapping files: `app/build/outputs/mapping/release/mapping.txt`*

---

## 11. ProGuard / R8 Rules

Release builds enforce `isMinifyEnabled = true`. The targeted keep rules in `app/proguard-rules.pro` include:

```proguard
# Preserve line numbers and source file names for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room persistence: preserve Entity fields and DAO interface definitions for SQLite queries
-keep class com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity { *; }
-keep interface com.fahim.geminiApiComposeStarter.data.local.ChatDao { *; }

# User preferences: preserve ThemeMode enum entries and valueOf method for DataStore string persistence
-keep enum com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode { *; }
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
| 8 | **Release APK Launch** | Install `app-release-unsigned.apk` via adb. | App boots without R8 reflection crashes. |

---

## 13. License & Academic Integrity

This project is submitted as part of coursework for NMIMS MPSTME Mobile Application Development (Lab Assignment 1).  
Student Name: **Ritunjay Deo** | Roll Number: **N023**.  
Base code provided by instructor **Fahim Khan** (`ifahimkhan/GeminiApiComposeStarter`).
