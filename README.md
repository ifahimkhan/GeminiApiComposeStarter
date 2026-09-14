# Gemini AI Compose Chat App

A modern Android AI chat application built with **Kotlin** and **Jetpack Compose**, integrating the **Gemini API** for AI-powered responses. Includes persistent chat history, voice input, dark mode, secure API-key handling, a responsive UI, and automated testing.

---

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Gemini API Key Setup](#gemini-api-key-setup)
- [API Key Security](#api-key-security)
- [Security Limitation](#security-limitation)
- [Chat Architecture](#chat-architecture)
- [Conversation History](#conversation-history)
- [Preferences](#preferences)
- [Voice Input](#voice-input)
- [User Interface](#user-interface)
- [Responsive Design](#responsive-design)
- [Loading & Error Handling](#loading--error-handling)
- [Testing](#testing)
- [Performance Optimization](#performance-optimization)
- [Release Build Security](#release-build-security)
- [How to Run the Project](#how-to-run-the-project)
- [Git Workflow](#git-workflow)
- [Secret Safety Checklist](#secret-safety-checklist)
- [Pull Request](#pull-request)
- [Author](#author)
- [Assignment Reference](#assignment-reference)

---

## Features

- 🤖 Gemini AI-powered chat
- 💬 Material 3 chat-bubble interface
- ⚡ Real-time loading indicator
- ❌ Error handling for failed API requests
- 🎤 Voice input using Android Speech Recognition
- 🗂️ Multiple conversation history
- 💾 Persistent chat history using Room Database
- ⚙️ User preferences using Preferences DataStore
- 🌙 Dark mode support
- 📱 Responsive layout for different screen sizes
- 🔐 Secure API-key storage using Android Keystore
- 🔒 AES-256-GCM encryption for API-key data at rest
- 🛡️ R8 minification for release builds
- 🧪 Unit tests for `ChatViewModel`
- 🖥️ Jetpack Compose UI tests
- 🚀 Network operations performed using coroutines

---

## Technologies Used

| Technology             | Purpose                          |
| ----------------------- | --------------------------------- |
| Kotlin                  | Application development           |
| Jetpack Compose         | UI development                    |
| Material 3              | Modern UI components              |
| Gemini API              | AI-generated responses            |
| Room                    | Persistent conversation history   |
| Preferences DataStore   | User preferences                  |
| Android Keystore        | Secure encryption-key storage     |
| AES-256-GCM             | API-key encryption                |
| Coroutines               | Asynchronous operations           |
| StateFlow                | UI state management               |
| RecognizerIntent          | Voice-to-text input                |
| JUnit                    | Unit testing                      |
| Compose Testing           | UI testing                        |
| R8                        | Release obfuscation               |


## Project Structure


app/
└── src/
    ├── main/
    │   └── java/com/fahim/geminiApiComposeStarter/
    │       ├── data/
    │       │   ├── ApiKeyManager.kt
    │       │   ├── GeminiRepository.kt
    │       │   ├── GeminiRepositoryImpl.kt
    │       │   ├── ChatHistoryRepository.kt
    │       │   ├── PreferencesManager.kt
    │       │   └── local/
    │       │       ├── ChatDatabase.kt
    │       │       ├── ChatMessageDao.kt
    │       │       ├── ChatMessageEntity.kt
    │       │       ├── ConversationDao.kt
    │       │       └── ConversationEntity.kt
    │       │
    │       ├── ui/
    │       │   ├── chat/
    │       │   │   ├── ChatScreen.kt
    │       │   │   ├── ChatUiState.kt
    │       │   │   └── ChatViewModel.kt
    │       │   └── theme/
    │       │       └── Theme.kt
    │       │
    │       └── MainActivity.kt
    │
    ├── test/
    │   └── java/com/fahim/geminiApiComposeStarter/
    │       └── ui/chat/ChatViewModelTest.kt
    │
    └── androidTest/
        └── java/com/fahim/geminiApiComposeStarter/
            └── ui/chat/ChatScreenTest.kt



## Gemini API Key Setup

The Gemini API key must **never** be hardcoded into Kotlin files, XML resources, or committed Gradle files. It is stored locally in `local.properties`, which is excluded from version control.

**1. Get a Gemini API key** — generate one in [Google AI Studio](https://aistudio.google.com/).

**2. Add it to `local.properties`** (project root):

properties
GEMINI_API_KEY=your_api_key_here

**3. Never commit the key.** It must not appear in:

*.kt
*.xml
build.gradle.kts
README.md
Git history / GitHub



## API Key Security

### 1. Local configuration
The key is read from `local.properties` at build time. For CI environments, it can instead be supplied via an environment variable (`GEMINI_API_KEY`), so the secret is never stored in the repository.

### 2. Android Keystore
The app uses the Android Keystore to generate and protect the encryption key used to encrypt the API key. This key never lives in application source code.

### 3. AES-256-GCM encryption
The Gemini API key is encrypted at rest using AES-256-GCM rather than stored as plaintext.

**Encryption flow:**


Gemini API key
      ↓
AES-256-GCM encryption (key protected by Android Keystore)
      ↓
Encrypted value stored locally
      ↓
Decrypted only when needed, immediately before use
      ↓
Passed to GenerativeModel
      ↓
Gemini API request


The decrypted key is never logged, displayed on screen, or shown in a Toast — satisfying the assignment's requirement for AES-256-GCM encryption with Android Keystore-backed key storage and encrypted persistence.


## Security Limitation

Client-side encryption protects the API key at rest on the device, but it **cannot fully hide a client-embedded key from a sufficiently determined attacker** who has root access or can inspect the running app. For production, Gemini API calls should be routed through a secure backend:


Android App → Secure Backend → Gemini API

Alternatively, API key restrictions and Firebase App Check add further protection without a full backend rewrite. This matches the assignment's recommendation to consider a backend proxy or Firebase App Check with restricted keys for production use.


## Chat Architecture

ChatScreen → ChatViewModel → GeminiRepository → Gemini API


**Persistent history:**

ChatViewModel → ChatHistoryRepository → Room Database → Conversation / Message tables


**UI state:**

ChatUiState → StateFlow → collectAsStateWithLifecycle() → Compose UI

This keeps the UI stateless and cleanly separates UI, business logic, and data layers.


## Conversation History

Room persists each conversation, storing:

- Conversation ID, title, creation time, and last-updated time
- User messages and Gemini responses
- Message timestamps

Because this is backed by Room, conversations survive an app restart, satisfying the assignment's persistence requirement.

## Preferences

Preferences DataStore stores app settings — currently the **dark mode** preference — so it persists across launches.

## Voice Input

Voice input uses Android's `RecognizerIntent`, launched via the Activity Result API (`rememberLauncherForActivityResult`):

Tap microphone → request mic permission → speech recognition
      → recognized text → prompt input field → send to Gemini



## User Interface

Built with Jetpack Compose and Material 3.

**Chat screen**
- Messages rendered in a `LazyColumn` with stable keys
- Distinct alignment/styling for user vs. Gemini bubbles
- Automatic scroll-to-latest
- Loading indicator and inline error messages

**Dark mode**
Controlled by the app theme plus the stored DataStore preference.



## Responsive Design

The layout adapts across phones, larger screens, and both portrait/landscape orientations using Compose adaptive techniques and `WindowSizeClass` where applicable.


## Loading & Error Handling

While a response is generating:

Gemini is thinking...


On failure, a specific message is shown, e.g.:

- `Network error. Please check your internet connection.`
- `Gemini API authentication failed. Check your API key.`
- `Gemini API quota exceeded. Please try again later.`


## Testing

### Unit tests
`ChatViewModel` is tested with JUnit, `kotlinx-coroutines-test`, and a fake `GeminiRepository`, covering:

- Empty prompt validation
- Successful Gemini response
- API failure handling
- Loading state
- Missing API key handling

./gradlew test


### Compose UI tests
Written with `createComposeRule()`, verifying key UI elements:

- App title
- New chat action
- User and Gemini messages
- Loading and error states

./gradlew connectedAndroidTest


Requires a connected emulator or physical device.



## Performance Optimization

- Coroutines keep network calls off the main thread
- `StateFlow` for efficient, targeted state updates
- `LazyColumn` with stable message IDs for smooth scrolling
- Room for lightweight persistent local storage
- Layout Inspector used to check for unnecessary recompositions



## Release Build Security

isMinifyEnabled = true


R8 minification and resource shrinking are enabled for release builds, making the APK harder to reverse-engineer and reducing its footprint.



## How to Run the Project

1. **Clone the repository**

   git clone <repository-url>

2. **Open in Android Studio**
3. **Configure the API key** in `local.properties`:
   properties
   GEMINI_API_KEY=your_api_key_here

4. **Sync Gradle** — `File → Sync Project with Gradle Files`
5. **Run** on an emulator or physical device



## Git Workflow

Create a branch prefixed with your roll number:

git checkout -b <ROLL_NUMBER>-gemini-chat


Before committing, confirm `local.properties` is **not** staged:

git status


Then:

git add .
git commit -m "Complete Gemini AI Compose assignment"
git push origin <ROLL_NUMBER>-gemini-chat

Finally, open a Pull Request.

## Secret Safety Checklist

- [ ] API key is not hardcoded anywhere
- [ ] `local.properties` is not staged or committed
- [ ] API key does not appear in Git history
- [ ] `local.properties.example` contains only a placeholder
- [ ] README does not contain the real API key
- [ ] `BuildConfig` sources the key from `local.properties` / environment
- [ ] Release build has R8 enabled

Double-check with `git status` and search the repo for accidental secrets before opening the PR — the diff and history must contain neither the real key nor `local.properties`.

**`local.properties.example`:**
properties
GEMINI_API_KEY=your_api_key_here


*(placeholder only — never commit a real key here)*



## Pull Request

Suggested PR description:

## Implemented Features
- Gemini API integration
- Material 3 chat UI
- Persistent Room chat history
- Preferences DataStore
- Voice input
- Dark mode
- Loading and error states
- Responsive UI
- Secure API key encryption
- Unit tests
- Compose UI tests
- R8 release minification

## Testing
- ChatViewModel unit tests
- Compose UI tests
- Voice input tested on Android emulator
- Chat history tested across app restarts
- API error handling tested

## Security
- API key stored locally, excluded from Git
- AES-256-GCM encryption via Android Keystore
- R8 enabled for release builds

## Screenshots
[Add screenshots here]



## Author

**Shloka Mamania**
Mobile Application Development
SVKM's NMIMS University — School of Technology Management & Engineering


## Assignment Reference

Developed for the Mobile Application Development Lab Assignment-1: enhancing the Gemini Jetpack Compose starter app with secure API-key management, Compose UI improvements, voice input, persistent storage, testing, performance optimization, and Git/PR submission practices.

> **Before submitting:** don't paste a real API key anywhere in this file — keep `GEMINI_API_KEY=your_api_key_here` as a placeholder, and confirm `local.properties` never shows up in `git status`.