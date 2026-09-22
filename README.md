# Silas Gemini Chat

A bilingual-ready (English UI) chat app built on the Gemini API, developed as
**Mobile Application Development — Lab Assignment 1**.

- **Author:** Silas · **n091** (MBA Tech CE, E1)
- **Stack:** Kotlin, Jetpack Compose (Material 3), Gemini API, Room, DataStore,
  Android Keystore.

## Features

- **Text chat with Gemini** via a `LazyColumn` of Material 3 chat bubbles with
  auto-scroll and stable item keys.
- **Voice input** using `RecognizerIntent` (speech-to-text).
- **Persistent history** with Room, so conversations survive app restarts.
- **Remembered preferences** (your name) with Preferences DataStore.
- **Responsive layout** via `WindowSizeClass` (phone / tablet / orientation).
- **Loading & error states** — progress indicator and Snackbar on failure.
- **Dark mode + dynamic colour** (Material 3, Android 12+).
- **Encrypted API key** with AES-256-GCM in the Android Keystore.

## Where the API key goes

1. Copy `local.properties.example` to `local.properties` (it is **git-ignored**).
2. Set your key:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
3. Gradle reads `local.properties` at build time and exposes the value as
   `BuildConfig.GEMINI_API_KEY`. If the file has no key, it falls back to the
   `GEMINI_API_KEY` environment variable (for CI).

> Never paste the key into Kotlin files, `strings.xml`, or `build.gradle.kts`.

## How the encryption flow works

1. On first launch the app generates an **AES-256-GCM** key inside the **Android
   Keystore** (`KeyGenParameterSpec`, non-exportable).
2. The build-time API key is encrypted with that key; only the **ciphertext and
   IV** are persisted (Base64) in a Preferences DataStore. The plaintext key is
   never written to disk.
3. The plaintext is decrypted **in memory only**, at the moment the
   `GenerativeModel` is created, and is never logged or displayed.

### Production hardening

Client-side encryption raises the bar but cannot fully hide a key from a
determined attacker. A production app should instead:

- move Gemini calls behind a **backend proxy** that holds the key server-side, or
- protect the endpoint with **Firebase App Check** and a **restricted API key**
  (locked to your app's package name / SHA-256 certificate).

The release build also runs **R8** (`isMinifyEnabled = true`) so the key is not
trivially readable from decompiled code.

## Running the tests

```bash
# Unit tests (ChatViewModel with a fake repository, kotlinx-coroutines-test)
./gradlew :app:testDebugUnitTest

# Instrumented Compose UI tests (needs an emulator/device)
./gradlew :app:connectedDebugAndroidTest

# Build the debug APK
./gradlew :app:assembleDebug
```

## Project structure

```
app/src/main/java/com/silas/geminichat/
├── MainActivity.kt            # entry point, WindowSizeClass
├── GeminiApplication.kt       # holds the AppContainer
├── di/AppContainer.kt         # manual dependency injection
├── data/
│   ├── GeminiRepository.kt    # interface (testable)
│   ├── GeminiRepositoryImpl.kt
│   ├── ChatHistoryRepository.kt
│   ├── secure/ApiKeyManager.kt     # Keystore + DataStore encryption
│   ├── local/                      # Room (entity, DAO, database)
│   └── prefs/                      # Preferences DataStore
├── model/ChatMessage.kt
└── ui/
    ├── chat/                  # ChatScreen, ChatViewModel, ChatUiState
    └── theme/                 # Color, Theme, Type
```
