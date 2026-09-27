# Gemini Orbit AI — Mobile Application Development Lab (Assignment 1)

**Student Name:** Abhay Telrandhe  
**Roll Number:** N104  
**Branch / Target:** `N104-MAD`  

---

## 📌 Project Overview
**Gemini Orbit AI** is a native Android conversational AI application developed with modern **Jetpack Compose** and Google's **Generative AI SDK (Gemini 1.5 Flash)**. It features a cosmic cyberpunk design language, offline message persistence via Room, hardware-backed AES-256-GCM encryption, and real-time voice speech transcription.

---

## ✨ Key Features
- **Conversational AI Core**: Powered by Google Generative AI SDK (`gemini-1.5-flash`) for responsive, multi-turn assistant capabilities.
- **Deep Space Orbit UI**: Modern Material 3 dark palette with electric cyan (`#00E5FF`) and ultraviolet accents, rounded gradient user bubbles, and clean model response cards.
- **Voice-to-Text Input**: Built-in Android `SpeechRecognizer` integration with live animated audio pulse waves and runtime microphone permission handling.
- **Offline Message Persistence**: Local caching using **Room Database** (`Room 2.7.0` + KSP + Kotlin Coroutine Flows).
- **Hardware-Isolated Security**: Cryptographic protection using Android's **Hardware KeyStore** with **AES-256-GCM** encryption (`KeyStoreManager.kt`).
- **Interactive Tools**: Single-tap copy-to-clipboard for bot responses and session purge controls.

---

## 🏗️ Architecture & Package Structure
The app adheres strictly to **Clean Architecture** and **MVVM** with Unidirectional Data Flow (UDF):

```
com.fahim.geminiApiComposeStarter/
├── MainActivity.kt               # Entrypoint with Edge-to-Edge and DB injection
├── data/
│   ├── GeminiRepository.kt       # Repository interface for testing
│   ├── GeminiRepositoryImpl.kt   # Gemini SDK API integration
│   ├── local/
│   │   ├── AppDatabase.kt        # Room database builder
│   │   ├── ChatDao.kt            # Reactive message query operations
│   │   └── ChatMessageEntity.kt  # Local database entity
│   ├── model/
│   │   └── ChatMessage.kt        # Domain model
│   └── security/
│       └── KeyStoreManager.kt    # Hardware KeyStore AES-256-GCM cryptography
└── ui/
    ├── chat/
    │   ├── ChatScreen.kt         # Jetpack Compose UI layout & interactions
    │   ├── ChatUiState.kt        # Immutable UI state holder
    │   └── ChatViewModel.kt      # StateFlow-driven presentation logic
    ├── theme/
    │   ├── Color.kt              # Cosmic Orbit theme palette
    │   ├── Theme.kt              # Material 3 Dynamic Theme configuration
    │   └── Type.kt               # Typography specifications
    └── voice/
        └── VoiceSpeechManager.kt # SpeechRecognizer voice input wrapper
```

---

## 🚀 Setup & Execution

### 1. Prerequisites
- **Android Studio**: Ladybug / Meerkat (or newer)
- **JDK**: Java 17 / 21
- **Android SDK**: Min SDK 26, Target SDK 36

### 2. Configure Gemini API Key
Add your Gemini API key in `local.properties` (this file is excluded from git for security):
```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

### 3. Build & Run
Run the unit test suite:
```bash
./gradlew testDebugUnitTest
```

Build the debug APK:
```bash
./gradlew assembleDebug
```

---

## 🧪 Testing & Verification
- **Unit Tests**: Full coverage for `ChatViewModelTest` (verifying default state, user prompts, error propagation, voice result processing, and session clearing).
- **Instrumentation & Build**: Clean Gradle compilation and APK generation verified.
