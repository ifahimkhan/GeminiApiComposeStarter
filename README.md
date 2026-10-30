# Gemini API Compose Starter

An Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** that demonstrates a modern AI chat client integrated with the **Google Generative AI SDK** (`gemini-3.6-flash`).

---

## 1. Overview & Feature List

- **Secure Key Handling**: Hardware-backed AES-256-GCM encryption using the Android Keystore and Preferences DataStore (`secure_key_store`).
- **Jetpack Compose Chat UI**: Material 3 chat bubbles in a `LazyColumn` with distinct colors (`primaryContainer` for user, `secondaryContainer` for Gemini), rounded shapes, auto-scrolling, and bold markdown formatting.
- **Voice Input**: Integrated speech-to-text button (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) next to the prompt text field.
- **DataStore User Settings**: Persisted theme preferences (System Default / Light / Dark) and dynamic color toggles saved in DataStore Preferences (`user_preferences`).
- **Room Chat History**: Offline message history stored in Room Database (`chat_history.db`) with a "Clear chat history" action and confirmation dialog.
- **Responsive Layout**: Adapts layout across screen sizes using Compose `WindowSizeClass` (`Compact`, `Medium`, `Expanded`).
- **Automated Testing**: Unit test suite for `ChatViewModel` using hand-written fakes and instrumented Compose UI tests for `ChatScreen`.

---

## 2. Setup Guide

### Getting a Gemini API Key
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. In the project root, copy `local.properties.example` to `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
3. Add your key to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

> **Warning**: Never commit `local.properties` or real API keys to version control. The file is listed in `.gitignore` to prevent accidental commits.

### CI Environment Variable Fallback
For continuous integration or build pipelines, set the `GEMINI_API_KEY` environment variable:
```bash
export GEMINI_API_KEY=your_api_key_here
```
At build time, `app/build.gradle.kts` loads `local.properties` first, falls back to `System.getenv("GEMINI_API_KEY")`, and defaults to `""`.

---

## 3. Encryption Flow

On first run, `SecureKeyStore` generates an AES-256-GCM secret key in the Android Keystore (`gemini_key_alias`). It encrypts `BuildConfig.GEMINI_API_KEY` and persists the Base64 ciphertext and IV in DataStore Preferences (`secure_key_store`).

Whenever a request is made, `SecureKeyStore.getApiKey()` decrypts the ciphertext in memory at that exact moment. The plaintext key is passed directly to `GenerativeModel` and is never cached in fields, logged, or written to disk. If stored ciphertext is invalid or stale, `SecureKeyStore` clears the invalid entry and re-encrypts from `BuildConfig.GEMINI_API_KEY`.

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Build Time: local.properties / System.getenv             │
│    --> Embedded into BuildConfig.GEMINI_API_KEY             │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. First Run (SecureKeyStore):                              │
│    --> Generates AES-256-GCM Key in Android Keystore        │
│        (alias: "gemini_key_alias")                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Encryption at Rest:                                      │
│    --> Encrypts BuildConfig key using AES-256-GCM           │
│    --> Stores Base64 ciphertext & IV in DataStore           │
│        ("secure_key_store")                                 │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. On-Demand Memory Decryption:                             │
│    --> SecureKeyStore.getApiKey() decrypts key in memory    │
│        only when GenerativeModel is instantiated            │
│    --> Key is never logged, cached, or displayed            │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Architecture & Data Flow

The project follows **MVVM** with **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────┐
│ UI Layer: ChatScreen                                        │
│ - Stateless composables observing ChatUiState               │
│ - Sends user events (onSend, onPromptChange, etc.)          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ ViewModel: ChatViewModel                                    │
│ - Hoists ChatUiState as StateFlow                           │
│ - Manages presentation logic and dialog states              │
└──────────────┬───────────────┬───────────────┬──────────────┘
               │               │               │
               ▼               ▼               ▼
┌────────────────────┐ ┌───────────────┐ ┌────────────────────┐
│ GeminiRepository   │ │ChatHistoryRepo│ │UserPreferencesRepo │
└──────────────┬─────┘ └───────┬───────┘ └─────────┬──────────┘
               │               │                   │
               ▼               ▼                   ▼
┌────────────────────┐ ┌───────────────┐ ┌────────────────────┐
│ Gemini SDK / API   │ │ Room Database │ │ Preferences        │
│ (gemini-3.6-flash) │ │ (chat_history)│ │ DataStore          │
└────────────────────┘ └───────────────┘ └────────────────────┘
```

- **Dependency Injection**: Dependencies are manually instantiated in `MainActivity.kt` and injected through `ChatViewModel.factory(...)`.
- **Main-Safety**: All database reads/writes (`ChatHistoryRepositoryImpl`) and DataStore preferences operations run off the main thread (`Dispatchers.IO` / main-safe suspend functions).

---

## 5. Security Limits & Production Best Practices

- **Client-Side Limits**: Obfuscating and encrypting keys in the Android Keystore raises the bar against reverse engineering, but client-side keys on a rooted or compromised device can still be extracted by a determined attacker.
- **Production Recommendations**:
  1. Route Gemini API requests through a secure Backend-for-Frontend (BFF) server proxy.
  2. Enforce [Firebase App Check](https://firebase.google.com/docs/app-check) to verify request authenticity.
  3. Restrict API keys in Google Cloud Console by Android package name and SHA-1 fingerprint.
- **Code Minification**: Release builds enable R8 minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) with custom ProGuard rules protecting Generative AI SDK, DataStore, and Room.

---

## 6. Running Tests

Run all Gradle commands from the project root directory (where `gradlew.bat` and `gradlew` reside).

### Unit Tests
Unit tests run using `kotlinx-coroutines-test`, JUnit 4, and hand-written in-memory fakes (`FakeGeminiRepository`, `FakeChatHistoryRepository`, `FakeUserPreferencesRepository`).

```bash
# Windows
.\gradlew.bat :app:testDebugUnitTest

# macOS / Linux
./gradlew :app:testDebugUnitTest
```
- **Test Report Location**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Android Studio**: Right-click `app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt` -> **Run 'ChatViewModelTest'**.

### Instrumented Compose UI Tests
Compose UI tests verify `ChatScreen` rendering using `createComposeRule()`. These require a connected physical device or running Android emulator.

```bash
# Windows
.\gradlew.bat :app:connectedDebugAndroidTest

# macOS / Linux
./gradlew :app:connectedDebugAndroidTest
```
- **Test Report Location**: `app/build/reports/androidTests/connected/index.html`
- **Android Studio**: Right-click `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt` -> **Run 'ChatScreenTest'**.

---

## 7. Screenshots

| Chat Screen (Light) | Chat Screen (Dark) | Settings & Options |
| :---: | :---: | :---: |
| ![Light Mode Chat](docs/screenshots/chat_light.png) | ![Dark Mode Chat](docs/screenshots/chat_dark.png) | ![Settings Dialog](docs/screenshots/settings.png) |
