# Gemini Pulse (Jetpack Compose)

**Mobile Application Development (Subject Code: 702AI0E002)**  
**School of Technology Management & Engineering, SVKM's NMIMS, Mumbai [2024-25]**  
**Lab Assignment 1**

- **Student Name**: Yashvi Sharma
- **Roll Number**: N095
- **Branch**: `N095_A1_MAD`
- **Repository**: [yashvii29/N095_YashviSharma](https://github.com/yashvii29/N095_YashviSharma)

---

## Project Overview
Gemini Pulse is a modern conversational AI client for Android powered by Google's Gemini Flash model (`gemini-3.6-flash`). The application is engineered following Clean Architecture and MVI (Model-View-Intent) design patterns using modern Jetpack Compose. It includes hardware-backed AES-256-GCM encryption for API key security at rest, local SQLite persistence via Room, reactive settings management via Preferences DataStore, and multi-modal speech recognition.

---

## Key Highlights & Features

### 1. Modern Jetpack Compose UI
- **LazyColumn with Stable UUID Keys**: Messages are keyed using unique IDs (`key = { it.id }`) to guarantee optimal rendering performance and prevent unnecessary recomposition cycles.
- **Auto-Scroll Behavior**: Reactive `LaunchedEffect` smoothly animates message history to the bottom on new user queries and incoming model tokens.
- **Clean Minimalist Interface**: Distraction-free design with a streamlined top app bar and an uncluttered conversation flow.
- **Animated Thinking Indicator**: Custom progress indicator bubble (`Gemini Pulse is responding…`) while inference is in flight.
- **Adaptive Layout**: Responsively scales with `BoxWithConstraints` (max-width capped at 720dp on larger screens/tablets/landscape).
- **Error Feedback**: Graceful error handling using Material 3 `SnackbarHost` and error card indicators.

### 2. Speech-to-Text Voice Input
- Native speech recognition powered by Android's `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and Compose's `rememberLauncherForActivityResult`.
- Allows spoken prompts to be transcribed directly into the query field with runtime permission checks.

### 3. Local Persistence & State Management
- **Room SQLite Database (`gemini_chat_database`)**: Automatically caches conversation records (`ChatMessageEntity`) using Room 2.7.0, ensuring chat history survives app closures, process death, and phone restarts.
- **Preferences DataStore (`UserPreferencesRepository`)**: Persists user settings and model configuration (`gemini-3.6-flash`) using asynchronous Kotlin Flows.

---

## Security Architecture & KeyStore Encryption at Rest

```
┌────────────────────────────────────────────────────────┐
│                   local.properties                     │
│         (Git-ignored, build-time compilation)          │
└───────────────────────────┬────────────────────────────┘
                            │ BuildConfig.GEMINI_API_KEY
┌───────────────────────────▼────────────────────────────┐
│                    On First Launch                     │
│       KeyStoreManager.kt (AndroidKeyStore Provider)    │
│            AES-256-GCM Hardware-Backed Master Key      │
└───────────────────────────┬────────────────────────────┘
                            │ Encrypt Key + 12-byte IV
┌───────────────────────────▼────────────────────────────┐
│              EncryptedSharedPreferences                │
│             (Persisted at rest as ciphertext)          │
└───────────────────────────┬────────────────────────────┘
                            │ Decrypt strictly in-memory
┌───────────────────────────▼────────────────────────────┐
│                 GeminiRepositoryImpl                   │
│         GenerativeModel("gemini-3.6-flash", apiKey)    │
│      (Zero logs, zero toasts, never exposed to UI)     │
└────────────────────────────────────────────────────────┘
```

---

## Architecture

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── data/
│   ├── local/
│   │   ├── ChatDao.kt
│   │   ├── ChatDatabase.kt
│   │   └── ChatMessageEntity.kt
│   ├── model/
│   │   └── ChatMessage.kt
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt
│   ├── security/
│   │   ├── KeyStoreManager.kt
│   │   └── SecureApiKeyStorage.kt
│   ├── GeminiRepository.kt
│   └── GeminiRepositoryImpl.kt
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   ├── ChatUiState.kt
│   │   └── ChatViewModel.kt
│   ├── text/
│   │   └── BoldMarkdown.kt
│   └── theme/
└── MainActivity.kt
```

---

## Build & Test Instructions

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 / JDK 21
- Android SDK API 36

### Setup
1. Clone the repository and checkout branch:
   ```bash
   git clone https://github.com/yashvii29/N095_YashviSharma.git
   cd N095_YashviSharma
   git checkout N095_A1_MAD
   ```
2. Create `local.properties` in the project root:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
3. Run Unit Tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Build APK:
   ```bash
   ./gradlew assembleDebug
   ```
