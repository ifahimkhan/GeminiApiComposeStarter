# Pull Request: Mobile Application Development Assignment 1

**Student:** Ritunjay Deo  
**Roll Number:** N023  
**Branch:** `N023-ritunjay-deo`  
**Target:** `master`  

---

## Summary

This pull request completes the requirements for Mobile Application Development Lab Assignment 1. The original starter application has been expanded into a fully featured conversational AI client built with Jetpack Compose (Material 3), Google's Gemini Generative AI SDK, Android KeyStore-backed AES-256-GCM credential encryption at rest, Room offline persistence, Preferences DataStore theme settings, speech dictation intent support, and R8 release minification.

---

## Implemented Features

- [x] **Secure API Key Handling**: Key resolved from `local.properties` (strictly git-ignored) with environment variable fallback (`GEMINI_API_KEY`) for CI/CD builds.
- [x] **Hardware-Backed Encryption at Rest**: `CryptoManager` generates an AES-256 key in `AndroidKeyStore`. Ciphertext and unique 12-byte random IV are saved in Preferences DataStore.
- [x] **In-Memory Decryption**: Key is decrypted strictly on-demand on an IO coroutine dispatcher and held in memory for process lifetime.
- [x] **Multi-Turn Conversational Memory**: Conversation history is mapped to alternating user/model turns and passed to `GenerativeModel.startChat()` to maintain full multi-turn context.
- [x] **Material 3 Chat UI**: Clean chat bubbles distinguishing user and assistant turns, with bold Markdown styling support (`**text**`).
- [x] **Stable List Identifiers & Auto-Scroll**: `LazyColumn` uses unique message IDs (`key = { it.id }`) and automatically scrolls to new messages via `LaunchedEffect`.
- [x] **StateFlow & State Hoisting**: `ChatViewModel` exposes immutable `StateFlow<ChatUiState>`. Composables are stateless and hoisted to `ChatRoute`.
- [x] **Lifecycle-Aware State Collection**: State collected via `collectAsStateWithLifecycle()` to prevent background resource leaks.
- [x] **Adaptive Responsive Layouts**: Uses `WindowWidthSizeClass` (Compact, Medium, Expanded) to optimize bubble widths and centered containers across phones, foldables, and landscape orientations.
- [x] **Error Handling & Loading UX**: Non-blocking thinking indicator during generation, Material 3 Snackbar for transient error messages with one-tap Retry action, and in-flight request cancellation on clear chat.
- [x] **Voice Input Dictation**: `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launcher integrated beside composer. Speech appends non-destructively to existing text without auto-sending.
- [x] **Room Offline Persistence**: Local SQLite database (`gemini_chat.db`) records message history chronologically, surviving application process termination.
- [x] **Preferences DataStore**: Manages application theme mode (`SYSTEM`, `LIGHT`, `DARK`) in `gemini_secure_prefs`, isolated from encryption keys.
- [x] **R8 Release Hardening**: Minified release build (`isMinifyEnabled = true`) with targeted ProGuard keep rules for Room DAOs, DataStore enums, and UI state models.
- [x] **Automated Testing Suite**: 93 automated tests (73 JVM unit tests + 20 connected instrumented tests) with 100% pass rate.

---

## Architecture

The application follows the recommended Android Architecture Components guide and Unidirectional Data Flow (UDF):

```
UI Layer (ChatRoute / ChatScreen / Material 3)
      ▲
      │ collects StateFlow<ChatUiState>
      ▼
State Holder (ChatViewModel)
      ▲
      │ coroutines / Flow
      ▼
Data Layer
  ├── GeminiRepository (Gemini Generative AI SDK + Fallback)
  ├── ApiKeyProvider (AndroidKeyStore AES-256-GCM + DataStore)
  ├── ChatHistoryRepository (Room SQLite Database)
  └── UserPreferencesRepository (Preferences DataStore)
```

- **UI Layer**: Composable functions observe state from `ChatViewModel` via `collectAsStateWithLifecycle()`. UI events (send, retry, clear, voice, theme change) are forwarded to the ViewModel.
- **ViewModel**: Manages business logic, optimistic UI updates, active request jobs, and coordinates between repositories.
- **Data Layer**: Repositories isolate data sources. Network calls and database queries execute on `Dispatchers.IO`.

---

## Security

1. **Development Key Isolation**: The API key is stored in `local.properties`, which is excluded from Git via `.gitignore`. A template file `local.properties.example` is provided for new checkouts.
2. **Encryption at Rest**: On first launch, the app encrypts the key using `AES/GCM/NoPadding` with a 128-bit authentication tag and a fresh 12-byte random IV. The encrypted payload is stored in Preferences DataStore.
3. **Android KeyStore**: Master key generation and cryptographic operations occur within the system `AndroidKeyStore` provider.
4. **Key Rotation & Safe Reseeding**: The SHA-256 fingerprint of the configured key is tracked. If the key is updated or removed in `local.properties`, the stored ciphertext is reseeded or purged automatically.
5. **Client-Side Security Scope**: Client-side encryption protects against unauthorized local file-system access (e.g., non-rooted device backups). As with all client applications embedding credentials in `BuildConfig`, production systems should proxy requests through an authenticated backend.

---

## Testing

All 93 automated tests pass cleanly:

- **JVM Unit Tests (73 tests)**:
  ```bash
  .\gradlew.bat testDebugUnitTest --rerun
  ```
  Covers Gemini repository error classification, retry loops, model fallback, multi-turn history mapping, ViewModel UI state transitions, in-flight request cancellation, KeyStore rotation, and markdown formatting.

- **Connected Device Tests (20 tests)**:
  ```bash
  .\gradlew.bat connectedDebugAndroidTest
  ```
  Executed on Android Emulator (`Pixel_10_Pro`, API 36). Covers Compose UI screen states, KeyStore round-trip encryption/decryption, fresh IV verification, corrupted ciphertext rejection, and Room DAO database transactions.

- **Static Analysis (Lint)**:
  ```bash
  .\gradlew.bat lintDebug lintRelease
  ```
  0 errors and 0 warnings.

---

## Runtime Verification

Live runtime verification was performed on an active emulator (`Pixel_10_Pro`, API 36):

1. **Live Gemini Generation**: Tested prompt submission; coherent AI responses were received and rendered on both debug and minified release APKs.
2. **Multi-Turn Context**: Submitted `"What is 2 + 2?"` -> received `"2 + 2 = 4"`. Followed up with `"Multiply that by 10"` -> model correctly understood the context and returned `"4 * 10 = 40"`.
3. **Voice Intent**: Tapped microphone button; system `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` dialog opened cleanly.
4. **Room Process-Death Persistence**: Sent multiple messages, terminated process via `adb shell am force-stop`, and relaunched; full message history was restored.
5. **Theme Switching**: Toggled between System default, Light, and Dark modes; preference persisted across process restarts.
6. **R8 Release APK**: Built and installed `app-release.apk` (`isMinifyEnabled = true`); verified cold launch, Room persistence, and live Gemini request (`"What is 5 + 5?"` -> `"5 + 5 = 10"`).

---

## Screenshots

The following submission screenshots are located in `docs/screenshots/`:

| Screenshot | Description | File |
| :--- | :--- | :--- |
| **Initial Screen** | Empty chat state with greeting and input controls | [`01-chat-home.png`](docs/screenshots/01-chat-home.png) |
| **Gemini Response** | Successful AI generative response bubble | [`02-gemini-response.png`](docs/screenshots/02-gemini-response.png) |
| **Multi-Turn Conversation** | Multi-turn dialogue maintaining contextual state | [`03-multiturn-chat.png`](docs/screenshots/03-multiturn-chat.png) |
| **Dark Theme** | Material 3 dark color scheme | [`04-dark-mode.png`](docs/screenshots/04-dark-mode.png) |
| **Voice Input Dialog** | System speech recognition intent dialog | [`05-voice-input.png`](docs/screenshots/05-voice-input.png) |
| **Adaptive Layout** | Responsive landscape layout with constrained container | [`06-landscape.png`](docs/screenshots/06-landscape.png) |

---

## Known Limitations

1. **Virtual Emulator Audio Input**: The speech recognition intent and error recovery are verified. However, live acoustic voice-to-text dictation requires physical microphone hardware.
2. **Third-Party Free-Tier Rate Limits**: The Gemini API free tier enforces a rate limit (20 requests per minute). The app implements retry logic and fallback model switching to handle transient capacity limits.
3. **Client-Side Key Distribution**: In production applications, client-side API keys should be replaced by an authenticated backend proxy or Firebase AI Logic with App Check.

---

## Notes

- Build was compiled against `compileSdk 36` using Android Gradle Plugin 9.0.0 and Kotlin 2.0.21.
- Release build uses debug certificate signing strictly for convenience during academic evaluation and automated testing on standard emulator images.
