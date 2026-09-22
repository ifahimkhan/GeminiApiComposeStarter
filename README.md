# Gemini Chat — MAD Assignment 1

**Author:** Vaishnavi · **Roll No:** N085

A Jetpack Compose Android chat app that talks to the Google Gemini API. Built for
*Mobile Application Development* Lab Assignment 1 (Subject Code `702AI0E002`), using the
[GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter) repository
as a reference.

## Features

- **Multi-turn conversation** — messages rendered as a `LazyColumn` of Material 3 chat bubbles
  (user vs. Gemini) with stable keys and auto-scroll to the latest message.
- **State hoisting** — all UI state lives in `ChatUiState`, exposed as a `StateFlow` from
  `ChatViewModel` and collected with `collectAsStateWithLifecycle()`.
- **Persistent history (Room)** — past conversations survive app restarts via a Room database.
- **Preferences (DataStore)** — the user's display name is remembered with Preferences DataStore
  (editable from the header).
- **Secure API key** — the key is encrypted at rest with an AES-256-GCM key in the Android
  Keystore; only the ciphertext + IV are persisted, and the plaintext is decrypted in memory
  only when the `GenerativeModel` is built (never logged or displayed).
- **Responsive layout** — `WindowSizeClass` + adaptive modifiers adapt to phones and tablets.
- **Loading & error states** — an inline "Gemini is typing…" indicator and a snackbar for errors.
- **Dark mode** — `isSystemInDarkTheme()` + Material 3 dynamic colour.
- **Voice input** — speech-to-text via `RecognizerIntent` (mic button next to the input).
- **R8 obfuscation** — `isMinifyEnabled = true` on release builds.
- **Tests** — unit tests for `ChatViewModel` (kotlinx-coroutines-test + fakes) and Compose UI
  tests for the chat screen (`createComposeRule()`).

## Architecture

```
data/
  GeminiRepository.kt        # abstraction over the Gemini call (unit-testable)
  GeminiRepositoryImpl.kt    # lazy key resolution + generateContent
  ApiKeyStore.kt             # AES-256-GCM Keystore encryption of the API key
  local/                     # Room: Message entity, ChatDao, AppDatabase
  prefs/                     # AppPreferences interface + DataStore implementation
ui/
  chat/                      # ChatScreen, ChatViewModel, ChatUiState
  theme/                     # Material 3 theme (light/dark/dynamic)
```

## Setup

1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. Create `local.properties` in the project root (it is git-ignored) and add:

   ```
   GEMINI_API_KEY=your_api_key_here
   ```

   See `local.properties.example`. The key is exposed as `BuildConfig.GEMINI_API_KEY` and never
   hardcoded in Kotlin, XML, or committed Gradle files.
3. Open the project in Android Studio, sync, and run on an emulator.

For CI, the build falls back to the `GEMINI_API_KEY` environment variable when `local.properties`
has no key.

## Running the tests

- Unit tests: `./gradlew :app:testDebugUnitTest`
- Instrumented UI tests (device/emulator required): `./gradlew :app:connectedDebugAndroidTest`

## Security notes

Client-side encryption raises the bar but cannot fully hide a key from a determined attacker
(the key is still baked into the APK via BuildConfig). A production app should move Gemini calls
behind a backend proxy or protect them with Firebase App Check and restricted API keys.
