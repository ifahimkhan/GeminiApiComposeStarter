# Gemini Spark (Jetpack Compose)

**Mobile Application Development (Subject Code: 702AI0E002)**  
**School of Technology Management & Engineering, SVKM's NMIMS, Mumbai [2024-25]**  
**Lab Assignment 1**

- **Student Name**: Vanshika Sharda
- **Roll Number**: N093
- **Branch**: `N093_MAD`
- **Repository**: [vanshikasharda30-sys/N093_MAD](https://github.com/vanshikasharda30-sys/N093_MAD)

---

## Project Overview
Gemini Spark is an AI assistant Android client developed with Kotlin and Jetpack Compose. Engineered with Clean Architecture principles and MVI, it integrates Google's `gemini-3.6-flash` model, hardware-backed AES-256-GCM encryption for API key security at rest, local SQLite persistence via Room, Preferences DataStore, and speech-to-text voice recognition.

---

## Key Highlights & Features

### 1. Modern Jetpack Compose UI
- **LazyColumn with Stable UUID Keys**: Conversation records are keyed with unique UUIDs (`key = { it.id }`) preventing recomposition thrashing and ensuring 60fps scrolling.
- **Auto-Scroll Engine**: Reactive `LaunchedEffect` smoothly animates chat history to the bottom on new queries and model answers.
- **Custom Aesthetic**: Unique sharp-corner bubble styling with dedicated avatar indicators.
- **Animated Thinking Indicator**: Custom progress indicator bubble (`Gemini Spark is thinking…`) during background model computation.
- **Adaptive Layout**: Scales cleanly via `BoxWithConstraints` (max-width 720dp on larger screens/tablets/landscape).
- **Error Handling**: Graceful error snackbar and prompt field error states.

### 2. Speech-to-Text Voice Input
- Built-in speech recognition using Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and Compose's `rememberLauncherForActivityResult`.
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
   git clone https://github.com/vanshikasharda30-sys/N093_MAD.git
   cd N093_MAD
   git checkout N093_MAD
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
