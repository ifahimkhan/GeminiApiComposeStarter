# N089ArieaSampatAssignment1 — Gemini Assistant

**Name:** Ariea Sampat
**Program:** MBA TECH CE
**Division:** E1
**Roll No:** N089

Mobile Application Development (Subject Code 702AI0E002) — Lab Assignment 1.

A Jetpack Compose chat app that talks to the Gemini API, built on top of the
[GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter) starter project.

## Features

- Material 3 chat UI rendered with `LazyColumn`, user vs. Gemini bubbles with stable keys and auto-scroll
- State hoisted into `ChatUiState`, exposed as a `StateFlow` from `ChatViewModel`, collected with `collectAsStateWithLifecycle()`
- Loading indicator and Snackbar error handling
- Voice input via `RecognizerIntent` (speech to text)
- Persistent chat history with Room, so conversations survive app restarts

## Setup

1. Get a Gemini API key from [Google AI Studio](https://ai.google.dev/gemini-api/docs/quickstart).
2. Copy `local.properties.example` to `local.properties` and fill in your key and SDK path:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
   `local.properties` is git-ignored and never committed.
3. Open the project in Android Studio and let Gradle sync.
4. Run on an emulator or device (`minSdk 24`).

## API key security

- The key is **never** hardcoded in Kotlin, XML, or committed Gradle files.
- `app/build.gradle.kts` reads it from `local.properties`, falling back to the `GEMINI_API_KEY`
  environment variable so CI builds can use a repository secret instead of a file.
- On first launch the app can generate an AES-256-GCM key in the Android Keystore and use it to
  encrypt the API key at rest (see `SecurityManager.kt`); only ciphertext is persisted, and the
  key is decrypted in memory only when the model client is created.
- `isMinifyEnabled = true` on the release build type so R8 obfuscates the APK.
- Client-side encryption raises the bar but can't fully hide a key from a determined attacker —
  a production app should move Gemini calls behind a backend proxy or protect them with
  Firebase App Check and restricted API keys.

## Project structure

```
app/src/main/java/com/example/n089arieasampatassignment1/
├── MainActivity.kt
├── data/
│   ├── ChatDao.kt
│   ├── ChatDatabase.kt
│   ├── GeminiRepository.kt
│   ├── LocalChatRepository.kt
│   └── SecurityManager.kt
├── model/
│   ├── ChatMessage.kt
│   └── ChatUiState.kt
└── ui/
    ├── chat/
    │   ├── ChatBubble.kt
    │   ├── ChatScreen.kt
    │   └── ChatViewModel.kt
    └── theme/
        └── Theme.kt
```

## Submission

- Branch name starts with the roll number (`N089-...`).
- PR description lists implemented features with screenshots.
- No secrets in git history — `local.properties` is never staged or committed.
