# Gemini Chat — Android Jetpack Compose Assignment

An Android chat application built with Jetpack Compose that connects to Google's Gemini API. It supports multi-turn conversations with memory, persistent chat history, multiple conversation threads with a sidebar drawer, voice input, dark/light theme toggling, and secure on-device storage of the API key using AES-256-GCM encryption via Android Keystore.

---

## Features

### Chat
- Multi-turn conversation with Gemini (full history sent with each request)
- Material 3 chat bubbles — user vs Gemini with distinct styling
- `LazyColumn` with stable keys for efficient list rendering
- Auto-scroll to the newest message on every update
- Bold markdown (`**text**`) rendered natively in replies
- Prompt validation (empty / whitespace rejected inline)

### Conversation management
- Sidebar drawer listing all past conversations (most recent first)
- "New chat" button to start a fresh thread
- Tap a conversation to load it, long-press or tap trash to delete
- Auto-generated title from the first 40 characters of the opening prompt
- All conversations persist across app restarts via Room

### Input
- Text field with multi-line support and rounded Material 3 styling
- Microphone button that launches speech-to-text via `RecognizerIntent`
- Recognized transcript appended to the field, editable before sending
- Runtime permission handling with graceful Snackbar errors

### Appearance
- Light / Dark / System theme selector in the TopAppBar settings menu
- Material 3 dynamic color on Android 12+
- "Compact bubbles" toggle for denser message layout
- Both preferences persist across restarts via DataStore

### Reliability
- Loading overlay while Gemini is responding
- Duplicate-send guard (Send and mic disabled during a request)
- Friendly error Snackbar for network, timeout, and missing-key cases
- No crashes on API or network failure

### Security
- API key loaded from `local.properties` (git-ignored) or `GEMINI_API_KEY` env var
- AES-256-GCM encryption at rest via Android Keystore
- Only ciphertext stored on disk; decryption happens in memory on launch
- Release build minified with R8 (`isMinifyEnabled = true`)
- Key never logged, displayed, or committed

---

## Technologies

| Layer | Library / Tool |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3, Compose BOM |
| Architecture | ViewModel, StateFlow, State Hoisting, manual DI via `ViewModelProvider.Factory` |
| Async | Kotlin Coroutines, Flow |
| AI | `com.google.ai.client.generativeai` (Gemini SDK) |
| Persistence | Room 2.8.0 (messages + conversations) |
| Preferences | DataStore Preferences 1.2.1 |
| Security | Android Keystore, AES-256-GCM, R8 minification |
| Speech | Android `SpeechRecognizer` / `RecognizerIntent` |
| Testing | JUnit4, kotlinx-coroutines-test |
| Build | AGP 9.x, KSP 2.3.6 |

---

## Project Structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt
├── data/
│   ├── ChatHistoryRepository.kt
│   ├── GeminiRepository.kt
│   ├── GeminiRepositoryImpl.kt
│   ├── SettingsRepository.kt
│   ├── local/
│   │   ├── ChatDatabase.kt
│   │   ├── ChatMessageDao.kt
│   │   ├── ChatMessageEntity.kt
│   │   ├── ConversationDao.kt
│   │   ├── ConversationEntity.kt
│   │   ├── PreferencesSettingsRepository.kt
│   │   └── RoomChatHistoryRepository.kt
│   └── security/
│       ├── CryptoManager.kt
│       ├── SecureApiKeyStore.kt
│       └── SecureKeyDataStore.kt
└── ui/
    ├── chat/
    │   ├── ChatScreen.kt
    │   ├── ChatUiState.kt
    │   └── ChatViewModel.kt
    ├── icons/
    │   └── MicIcon.kt
    ├── text/
    │   └── BoldMarkdown.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        ├── ThemeMode.kt
        └── Type.kt
```

---

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 11+
- Android SDK: compileSdk 36, minSdk 26, targetSdk 36
- Physical device or emulator running Android 8.0+
- Internet connection on the device
- Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

---

## Setup

### 1. Clone and open

```bash
git clone https://github.com/<your-username>/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
```

Open Android Studio → **File → Open** → select the project folder. Let Gradle sync finish.

### 2. Configure the Gemini API key

The key is loaded at build time from `local.properties` (project root) and injected as `BuildConfig.GEMINI_API_KEY`. This file is git-ignored and must never be committed.

Create `local.properties` if it doesn't exist and add:

```properties
GEMINI_API_KEY=your_api_key_here
```

Then **File → Sync Project with Gradle Files**.

If you prefer using an environment variable (useful for CI), you can set `GEMINI_API_KEY` in your shell instead — the build falls back to it when `local.properties` has no value.

`local.properties.example` is committed as a placeholder template for contributors.

### 3. Run

- Emulator or device connected
- Click **Run ▶**

Or from the command line:

```bash
./gradlew installDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

---

## How the API key is secured

1. **Build time** — Gradle reads `GEMINI_API_KEY` from `local.properties` (or the env var) and injects it as `BuildConfig.GEMINI_API_KEY`. The key is never written into any `.kt`, `.xml`, or `.kts` file.

2. **First launch** — `SecureApiKeyStore` takes the plaintext key, encrypts it with an AES-256-GCM key generated inside the **Android Keystore** (hardware-backed where available), and writes only the Base64-encoded `IV + ciphertext` into a dedicated Preferences DataStore (`secure_keys.preferences_pb`).

3. **Subsequent launches** — the ciphertext is read from DataStore, decrypted in memory, and handed directly to `GeminiRepositoryImpl`. The plaintext exists only in RAM for the duration of the process. It is never logged, displayed in the UI, written to a file, or sent anywhere except Google's Gemini endpoint.

4. **Release build** — R8 minification and obfuscation are enabled so the class names and string constants aren't trivially readable from a decompiled APK.

### Production security limitations

Client-side encryption raises the bar but **cannot** fully hide a key from a determined attacker. Anyone with a rooted device or the ability to instrument the app at runtime can recover the plaintext key once it has been decrypted in memory.

For a production application:
- Move Gemini calls behind a **backend proxy** that holds the real key server-side.
- Or protect the client with **Firebase App Check** and use **restricted API keys** scoped to specific Android apps and APIs.

This project uses client-side encryption as required by the assignment — it is a student demonstration, not a production security model.

---

## Room — chat history

- **`ChatMessageEntity`** — `id`, `conversationId`, `text`, `author` (USER/GEMINI), `timestamp`
- **`ConversationEntity`** — `id`, `title`, `createdAt`, `updatedAt`
- **`ChatMessageDao`** — `getForConversation()`, `insert()`, `clearForConversation()`
- **`ConversationDao`** — `observeAll()`, `insert()`, `updateTitle()`, `touch()`, `delete()`
- **`ChatDatabase`** — singleton, version 2, `fallbackToDestructiveMigration()`

All DAO methods are `suspend` functions, so Room automatically dispatches them off the main thread.

The `ChatViewModel` observes the conversation list via Flow, loads messages on selection, and persists every new user and Gemini message. History survives app restarts.

---

## Voice input

Voice input is launched with `RecognizerIntent` via `rememberLauncherForActivityResult`, satisfying the assignment's requirement to use the launcher-based API rather than a directly-instantiated `SpeechRecognizer`.

Flow:
1. User taps the mic button.
2. `RECORD_AUDIO` permission is requested (first time only) via another `rememberLauncherForActivityResult`.
3. `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` fires into the system speech app.
4. The returned transcript is **appended** to the prompt field, editable before sending.
5. Permission denial, cancellation, and no-match cases surface as Snackbars without crashing.

### Testing on the emulator
Enable host audio input: emulator **Extended Controls (…) → Microphone → "Virtual microphone uses host audio input"**. Unmute the host machine's physical microphone.

---

## License

Academic assignment submission. Refer to the repository for licensing terms.