# Gemini API Compose Starter — Enhanced Chat App

A Kotlin + Jetpack Compose Android application demonstrating Gemini AI text generation with modern UI enhancements, secure API key storage, local persistence, speech-to-text input, and adaptive layouts.

---

## Features

### 1. API-Key Security Flow
- **`local.properties` Configuration**: The API key is defined in `local.properties` (or environment variables) as `GEMINI_API_KEY=your_api_key_here` and compiled into `BuildConfig` without being committed to version control.
- **On-Device Encryption**: At startup, `SecureApiKeyStorage` encrypts the key using **Android Keystore + AES-256-GCM** and stores the ciphertext in an encrypted **Preferences DataStore**.
- **Dynamic Retrieval**: `GeminiRepositoryImpl` retrieves and decrypts the API key at runtime when instantiating `GenerativeModel`.

### 2. Speech-to-Text Integration
- Uses Android's `RecognizerIntent` with `rememberLauncherForActivityResult`.
- Allows voice input via a dedicated microphone button in the prompt bar, populating the recognized text directly into the prompt field.

### 3. Preferences DataStore
- Uses `UserPreferencesRepository` to store user settings asynchronously.
- Persists chat input mode preferences (single-line vs. multi-line mode) and restores them upon app restart.

### 4. Room Conversation History
- Uses **Room Database** (`AppDatabase`, `ChatMessageDao`, `ChatMessageEntity`) to store chat messages locally.
- Restores full conversation history when the application launches.

### 5. UI Enhancements & Adaptive Layouts
- **Material 3 Chat Bubbles**: Distinct shapes, alignment, and color schemes for user and assistant messages.
- **`LazyColumn` with Stable Keys**: Uses `message.id` for stable item tracking and performance.
- **Auto-Scroll**: Automatically scrolls to the newest message or loading bubble using `LazyListState` and `LaunchedEffect`.
- **Loading & Error Handling**: Displays an inline typing indicator during generation and shows user-friendly error messages via `SnackbarHost`.
- **State Hoisting**: `ChatUiState` as single source of truth exposed via `StateFlow` and collected lifecycle-aware using `collectAsStateWithLifecycle()`.
- **Adaptive Layout**: Adapts layout margins and content width across `Compact` (<600dp), `Medium` (600–840dp), and `Expanded` (≥840dp) window size classes using Material 3 breakpoints.

---

## Setup & Build Instructions

1. **Configure Gemini API Key**:
   Create or open `local.properties` in the root directory and add:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key
   ```
2. **Build the Project**:
   Run the following Gradle task or build via Android Studio:
   ```bash
   ./gradlew app:assembleDebug
   ```
3. **Run on Device / Emulator**:
   Deploy `app` to an Android device running API level 26 or higher.

---

## Production Security Limitation

> [!WARNING]
> Storing or processing API keys directly inside a client Android app presents inherent security risks. Decompilation, memory inspection, or root-level device access can expose client-side secrets.
> 
> **Production Best Practice**: In a production system, route requests through a secure backend proxy server that handles API key authorization, rate limiting, and user authentication, or restrict API keys strictly by package name and SHA-1 fingerprint in the Google Cloud Console.
