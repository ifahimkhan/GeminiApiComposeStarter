# Gemini Assistant (GeminiApiComposeStarter)

An intelligent, secure, and modern Android conversational assistant built with **Jetpack Compose**, **Material 3**, **Android Room**, and the **Google Generative AI SDK**. Developed as part of Mobile Application Development Assignment 1.

---

## Table of Contents

1. [Features](#features)
2. [Technology Stack](#technology-stack)
3. [Architecture Overview](#architecture-overview)
4. [Project Structure](#project-structure)
5. [Setup & API Key Configuration](#setup--api-key-configuration)
   - [Local Configuration (`local.properties`)](#local-configuration-localproperties)
   - [Template File (`local.properties.example`)](#template-file-localpropertiesexample)
   - [Environment Variable Fallback](#environment-variable-fallback)
   - [Missing Key Graceful Handling](#missing-key-graceful-handling)
6. [Room Chat History Persistence](#room-chat-history-persistence)
7. [Voice Input / Speech-to-Text](#voice-input--speech-to-text)
8. [Security Architecture](#security-architecture)
   - [Android Keystore Key Protection](#android-keystore-key-protection)
   - [AES-256-GCM Authenticated Encryption](#aes-256-gcm-authenticated-encryption)
   - [Secure Credential Storage at Rest](#secure-credential-storage-at-rest)
   - [Realistic Client-Side Security Limitations](#realistic-client-side-security-limitations)
9. [R8 Minification & Release Builds](#r8-minification--release-builds)
10. [Testing](#testing)
    - [Unit Tests (`testDebugUnitTest`)](#unit-tests-testdebugunittest)
    - [Fake Repository Implementation](#fake-repository-implementation)
    - [Compose UI & Instrumented Tests](#compose-ui--instrumented-tests)
11. [Build Instructions](#build-instructions)

---

## Features

- **Modern Material 3 Interface**: Expressive chat bubbles, dynamic top bar, responsive layouts (max width constraints for tablets and large screens), and polished typography.
- **Dynamic Theming**: Full support for both Dark and Light themes with fluid transitions.
- **Empty Chat Starter**: Suggestion prompt chips (Summarize, Explain, Email, Code) for instant interaction.
- **Room Persistent Chat History**: All conversations and messages are persisted locally using SQLite via Room. Previous conversations are restored across app launches.
- **Multi-Conversation Management**: Create new chats via navigation drawer, switch between recent conversations, and clear history with confirmation dialogs.
- **Voice Input (STT)**: Microphone button integrates with Android `RecognizerIntent` to transcribe spoken words directly into the text input for editing prior to sending.
- **Resilient AI Generation**: Automatic fallback models (`gemini-2.5-flash`, `gemini-1.5-flash`) when encountering temporary server spikes or 503 high-demand conditions.
- **Animated Feedback**: Polished typing indicator during Gemini response generation, accompanied by an actionable error card supporting Retry and Dismiss.
- **Multi-Turn Conversational Memory**: Passes preceding chat history (`user` and `model` turns) to Gemini with alternating role formatting, allowing natural follow-up questions, contextual explanations, and continuity across sessions.
- **Hardware-Backed Cryptography**: AES-256-GCM authenticated encryption paired with the Android Keystore system protects sensitive credentials at rest.

---

## Technology Stack

- **UI**: Jetpack Compose (BOM 2024.09.00), Material 3 (`androidx.compose.material3`)
- **Language**: Kotlin 2.0.21
- **Architecture**: MVVM + Repository Pattern + Clean Architecture
- **State Management**: Kotlin Coroutines & `StateFlow` collected with lifecycle awareness (`collectAsStateWithLifecycle`)
- **Local Persistence**: Android Room 2.6.1 + KSP code generation
- **AI Integration**: Google Generative AI SDK (`com.google.ai.client.generativeai:0.9.0`)
- **Security**: Android Keystore + AES-256 in Galois/Counter Mode (GCM)
- **Minification**: R8 with custom ProGuard rules and resource shrinking
- **Testing**: JUnit 4, `kotlinx-coroutines-test`, `androidx.compose.ui.test`

---

## Architecture Overview

The application follows the recommended Android architecture guidelines, ensuring separation of concerns, testability, and deterministic state flow:

```
[ UI Layer (Jetpack Compose) ]
         │
         ▼  (User Actions / Events)
[ ChatViewModel (MVVM) ] ◄─── (ChatUiState StateFlow)
         │
         ▼
[ ChatRepository (Unified Interface) ]
   ├── Local Data Source: ChatMessageDao (Android Room SQLite)
   ├── Remote Data Source: GeminiRepositoryImpl (Google Generative AI SDK)
   └── Security Layer: SecureApiKeyStorage (Android Keystore + AES-256-GCM)
```

1. **UI Layer**: Composable screens (`ChatScreen`, `SettingsScreen`) render state emitted by `ChatViewModel`. Interactive elements dispatch lambdas without retaining business logic.
2. **ViewModel Layer**: Manages `ChatUiState`, coordinates conversation switching, executes coroutines in `viewModelScope`, and validates input.
3. **Repository Layer**: Coordinates local persistence in Room before and after dispatching generation requests to the Gemini client.
4. **Security Layer**: Protects API keys and sensitive tokens at rest via authenticated encryption backed by the Android Keystore.

---

## Project Structure

```
app/src/
├── main/
│   ├── java/com/fahim/geminiApiComposeStarter/
│   │   ├── MainActivity.kt                  # App entry point, Keystore storage wiring
│   │   ├── data/
│   │   │   ├── ChatRepository.kt            # Unified repository interface
│   │   │   ├── ChatRepositoryImpl.kt        # Room + Gemini coordination
│   │   │   ├── GeminiRepository.kt          # Remote AI generation interface
│   │   │   ├── GeminiRepositoryImpl.kt      # SDK caller with 503 fallback
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt           # RoomDatabase singleton
│   │   │   │   ├── ChatMessageDao.kt        # Reactive SQLite DAO queries
│   │   │   │   ├── ChatMessageEntity.kt     # Room entity with conversation index
│   │   │   │   └── RecentConversation.kt   # Query projection for drawer items
│   │   │   └── security/
│   │   │       ├── AesGcmCipher.kt          # AES-256-GCM authenticated cipher
│   │   │       ├── KeyStoreManager.kt       # Android Keystore key management
│   │   │       └── SecureApiKeyStorage.kt   # Encrypted credential storage at rest
│   │   └── ui/
│   │       ├── chat/
│   │       │   ├── ChatScreen.kt            # Main chat screen with Drawer & Scaffold
│   │       │   ├── ChatUiState.kt           # Immutable UI state models
│   │       │   ├── ChatViewModel.kt         # MVVM ViewModel coordinating logic
│   │       │   └── components/              # Modular UI components
│   │       │       ├── ChatDrawer.kt        # Navigation drawer (New Chat, Recents)
│   │       │       ├── ChatInputBar.kt       # Multiline input, Mic, Send button
│   │       │       ├── ChatMessageItem.kt   # User and Gemini message bubbles
│   │       │       ├── ChatTopBar.kt        # Menu, Title, Avatar, Settings action
│   │       │       ├── EmptyChatView.kt     # Welcome screen with suggestion chips
│   │       │       ├── ErrorCard.kt         # Error display with Retry & Dismiss
│   │       │       └── ThinkingMessageItem.kt # Animated 3-dot thinking indicator
│   │       ├── settings/
│   │       │   └── SettingsScreen.kt        # Settings screen (Theme, Clear History)
│   │       └── theme/                       # Material 3 colors, typography, theme
│   └── res/                                 # Drawables, mipmaps, strings, themes
├── test/                                    # Local JVM Unit Tests
│   └── java/com/fahim/geminiApiComposeStarter/
│       ├── MainDispatcherRule.kt            # Coroutine test rule
│       ├── data/
│       │   └── FakeChatRepository.kt        # Deterministic fake repository
│       ├── data/security/
│       │   └── AesGcmCipherTest.kt          # AES-256-GCM cipher unit tests
│       └── ui/chat/
│           └── ChatViewModelTest.kt         # ViewModel behavior unit tests
└── androidTest/                             # Instrumented Android Tests
    └── java/com/fahim/geminiApiComposeStarter/
        ├── ChatScreenTest.kt                # Compose UI interaction tests
        └── KeyStoreSecurityInstrumentedTest.kt # Android Keystore integration tests
```

---

## Setup & API Key Configuration

### Local Configuration (`local.properties`)

The Gemini API key is supplied via `local.properties` located in the root project directory. This file is explicitly excluded from version control in `.gitignore` to prevent credentials from ever being committed.

To configure your API key, add this line to `local.properties`:

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

### Template File (`local.properties.example`)

A committed template file named `local.properties.example` is provided in the repository root. It explains the expected structure using a safe placeholder:

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

### Environment Variable Fallback

In addition to `local.properties`, the Gradle build configuration (`app/build.gradle.kts`) automatically falls back to the system environment variable `GEMINI_API_KEY`:

```kotlin
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
    ?.trim()
    ?.ifEmpty { null }
    ?: System.getenv("GEMINI_API_KEY")?.trim().orEmpty()
```

This allows automated CI/CD runners (GitHub Actions, GitLab CI) to inject the credential without writing a `local.properties` file.

### Missing Key Graceful Handling

If neither `local.properties` nor the environment variable provides a key, the project still compiles and runs without crashing. When the user taps the send button without a configured API key:

- No network request is initiated.
- The UI displays a clear, friendly error banner:
  > *"GEMINI_API_KEY is missing. Add it to local.properties and rebuild."*

---

## Room Chat History Persistence

Persistent chat history is managed by Android Room:

- **Entity (`ChatMessageEntity`)**: Stores message ID (UUID), parent `conversationId`, participant (`USER` vs `GEMINI`), message text, display timestamp, and sorting epoch timestamp (`createdAt`). An index is maintained on `conversationId` for fast query performance.
- **DAO (`ChatMessageDao`)**: Exposes reactive Kotlin `Flow<List<ChatMessageEntity>>` for the active conversation, as well as a projection `Flow<List<RecentConversation>>` for listing historical sessions in the navigation drawer.
- **Database (`AppDatabase`)**: Implemented as a thread-safe singleton with destructive fallback migration for development.

---

## Voice Input / Speech-to-Text

Voice transcription uses Android's built-in `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`:

1. **Permission Check**: Checks `Manifest.permission.RECORD_AUDIO` at runtime. If not granted, launches the permission request contract with graceful denial handling.
2. **Speech Recognition**: Launches the system voice recognition dialog.
3. **Safe Text Population**: Upon receiving transcribed speech, the text is appended directly into the existing `TextField`.
4. **No Auto-Send**: The transcription is deliberately **not** sent immediately to the AI. The user retains full control to review, edit, or cancel the text before tapping the Send button.

---

## Security Architecture

### Android Keystore Key Protection

The cryptographic encryption key is generated and stored inside the **Android Keystore** system (`AndroidKeyStore` provider). Key generation uses `KeyGenParameterSpec`:

- Algorithm: `KeyProperties.KEY_ALGORITHM_AES`
- Key Size: **256 bits**
- Purposes: `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`
- Block Mode: `BLOCK_MODE_GCM`
- Padding: `ENCRYPTION_PADDING_NONE`

Because the key is maintained within the Keystore, the raw key material never enters the application process memory in plaintext and cannot be extracted from standard app storage.

### AES-256-GCM Authenticated Encryption

Data encryption is executed by `AesGcmCipher` using **AES-256-GCM** (Galois/Counter Mode):

- **Authenticated Encryption with Associated Data (AEAD)**: In addition to encrypting plaintext (confidentiality), GCM computes a 128-bit authentication tag over the ciphertext (integrity and authenticity). Any tampering or modification of the ciphertext immediately invalidates the tag and halts decryption.
- **Fresh Random IV/Nonce**: For every encryption operation, a cryptographically secure 12-byte (96-bit) IV is generated. This ensures that identical plaintexts never produce the same ciphertext.
- **Payload Structure**: The IV length, IV bytes, and ciphertext with tag are bundled together into a structured byte stream and encoded using RFC 4648 Base64.

### Secure Credential Storage at Rest

`SecureApiKeyStorage` leverages `KeyStoreManager` to store sensitive credentials in private `SharedPreferences` in encrypted ciphertext form. On app startup, credentials can be retrieved and validated without ever exposing them unencrypted on disk.

### Realistic Client-Side Security Limitations

> [!IMPORTANT]
> **Client-Side Mobile Security Notice**:
> While Android Keystore and AES-256-GCM protect secrets at rest against physical storage dumps and unauthorized apps, **client-side credentials cannot be considered 100% impenetrable against a determined attacker**.
>
> 1. Because the mobile client must ultimately make direct API requests, the API key must temporarily exist in process memory when constructing HTTP headers.
> 2. A reverse engineer with root access, a modified Android runtime, or dynamic instrumentation tools (e.g., Frida, Xposed) can hook cryptographic APIs or dump process memory to inspect the key.
> 3. **Production Best Practice**: For production commercial systems, mobile applications should never communicate directly with LLM provider endpoints using a master API key. Instead, the mobile client should authenticate against a **backend proxy server** (using Firebase Authentication, App Check, or OAuth2), which securely forwards sanitized prompts to Gemini using server-side secrets.

---

## R8 Minification & Release Builds

Release builds are configured with R8 code shrinking, obfuscation, and resource optimization in `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

The `app/proguard-rules.pro` file includes rules tailored for:
- Room Database (`RoomDatabase`, `@Dao`, `Migration`)
- Google Generative AI SDK & Ktor network engine
- Kotlinx Serialization & Coroutines continuation fields
- Security classes (`com.fahim.geminiApiComposeStarter.data.security.**`)

---

## Testing

The project is equipped with deterministic unit tests and UI tests that **do not require internet connectivity or a real Gemini API key**.

### Unit Tests (`testDebugUnitTest`)

Located in `app/src/test/java/com/fahim/geminiApiComposeStarter/`:

1. **`AesGcmCipherTest`** (6 tests):
   - Verifies encryption and decryption round-trip restores exact plaintext.
   - Verifies fresh random IV generation creates distinct ciphertexts for identical inputs.
   - Verifies tampering with ciphertext fails AEAD authentication tag verification.
   - Verifies decryption with an incorrect key fails authentication.
   - Verifies handling of empty strings and truncated payloads.
2. **`ChatViewModelTest`** (14 tests):
   - Verifies initial UI state, empty prompt validation, and prompt error clearing.
   - Verifies missing API key behavior displays friendly error without dispatching network calls.
   - Verifies successful generation flow with `FakeChatRepository`.
   - Verifies error handling, loading state clearing, retry logic, and error dismissal.
   - Verifies New Chat creation, conversation switching, and clear history.
   - Verifies Settings screen navigation and theme/history toggles.

### Fake Repository Implementation

`FakeChatRepository` implements `ChatRepository` entirely in memory with configurable results (`generateTextResult`). It does not invoke network APIs or SQLite, guaranteeing fast and reliable test execution.

### Compose UI & Instrumented Tests

Located in `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/`:

- **`ChatScreenTest`**: Tests empty chat view, input field entry, send button enabling/disabling, chat message rendering, thinking dots visibility, error card dismiss/retry, and settings screen layout.
- **`KeyStoreSecurityInstrumentedTest`**: Tests hardware-backed key generation, AES-256-GCM encryption/decryption in the Android OS runtime, and `SecureApiKeyStorage` persistence.

---

## Build Instructions

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 21 (configured in `JAVA_HOME` or Android Studio Gradle settings)
- Android SDK Platform 36 / Android 14+ (minSdk 26)

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
*Output: `app/build/outputs/apk/debug/app-debug.apk`*

### Build Minified Release APK
```bash
./gradlew assembleRelease
```
*Output: `app/build/outputs/apk/release/app-release-unsigned.apk`*
