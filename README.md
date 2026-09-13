# GeminiApiComposeStarter

An Android chat application built with Jetpack Compose that talks to Google's Gemini API. The app supports multi-turn conversations with memory, persistent chat history, voice input, and a small user-preference toggle — all backed by modern Android architecture components.

---

## Key Features

- **Multi-turn chat with Gemini** — sends the full prior conversation with every request so replies stay context-aware.
- **Material 3 chat UI** — left/right message bubbles for Gemini and the user, with the assistant icon and bold-markdown rendering.
- **Automatic scrolling** — the message list always lands on the newest bubble.
- **Loading state** — a centred "Thinking…" indicator blocks duplicate sends while a request is in flight.
- **Error handling** — network, timeout, and missing-key failures surface as user-friendly Snackbars without crashing the app.
- **Prompt validation** — empty or whitespace-only prompts are rejected inline.
- **Voice input** — speak a prompt via Android's `SpeechRecognizer`; the transcript lands in the input field, editable before sending.
- **Persistent chat history** — messages survive app restarts via Room.
- **Compact bubbles preference** — user toggle stored with DataStore Preferences.
- **Responsive layout** — adapts to phones, tablets, landscape, and the on-screen keyboard.
- **Unit tests** — `ChatViewModel` tests using fake repositories (no real API calls).

---

## Technologies & Libraries

| Layer | Library |
|---|---|
| UI | Jetpack Compose, Material 3, Compose BOM |
| Language | Kotlin 2.2.10 |
| Build | Android Gradle Plugin 9.x, KSP 2.3.6 |
| Architecture | ViewModel, StateFlow, State Hoisting, Manual DI via `ViewModelProvider.Factory` |
| Async | Kotlin Coroutines, Flow |
| Networking / AI | `com.google.ai.client.generativeai` (Google Generative AI SDK) |
| Persistence (history) | Room 2.8.0 (Entity + DAO + Database) |
| Persistence (preferences) | DataStore Preferences 1.2.1 |
| Speech | Android `SpeechRecognizer` API |
| Testing | JUnit 4, `kotlinx-coroutines-test` |

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
│   └── local/
│       ├── ChatDatabase.kt
│       ├── ChatMessageDao.kt
│       ├── ChatMessageEntity.kt
│       ├── PreferencesSettingsRepository.kt
│       └── RoomChatHistoryRepository.kt
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
        └── Type.kt
```

---

## Requirements

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 11** or newer
- **Android SDK** — compileSdk 36, minSdk 26, targetSdk 36
- **Android device or emulator** — Android 8.0 (API 26) or higher
- **Internet connection** on the device
- **Gemini API key** from [Google AI Studio](https://aistudio.google.com/app/apikey)

---

## Setup in Android Studio

1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd GeminiApiComposeStarter
   ```
2. Open Android Studio → **File → Open** → select the project folder.
3. Wait for the initial Gradle sync to complete.
4. Configure the API key (next section).
5. Connect a device or start an emulator.

---

## Configure the Gemini API Key

The key is read at build time from a `local.properties` file at the project root and injected into the app as `BuildConfig.GEMINI_API_KEY`. `local.properties` is already listed in `.gitignore` and **must never be committed**.

1. Open (or create) `local.properties` at the project root.
2. Add your key on its own line:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
3. Save the file and run **File → Sync Project with Gradle Files**.

If the key is missing, the app still builds and runs — it will display a Snackbar explaining that `GEMINI_API_KEY` is missing.

---

## Build & Run

### From Android Studio
- Click the green **Run ▶** button, or use **Build → Make Project** (`Ctrl/Cmd + F9`) to build without installing.

### From the command line
```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```
On Windows, use `gradlew.bat` instead of `./gradlew`.

---

## Running Tests

Unit tests live under `app/src/test/` and use fake repositories — no real Gemini API call is made.

### From Android Studio
- Right-click `app/src/test/java/.../ChatViewModelTest.kt` → **Run 'ChatViewModelTest'**.
- Or click the ▶ icon in the gutter next to the class or an individual `@Test`.

### From the command line
```bash
# All unit tests
./gradlew test

# Only the ViewModel test class
./gradlew testDebugUnitTest --tests "com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModelTest"
```
HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`.

---

## Chat History (Room)

Chat messages are persisted with Room so they survive app restarts.

- **Entity** — `ChatMessageEntity` (`id`, `text`, `author`, `timestamp`).
- **DAO** — `ChatMessageDao` with `getAll()`, `insert()`, and `clear()`.
- **Database** — `ChatDatabase` is a singleton (`@Volatile` + double-checked lock) so only one instance exists per process.
- **Wiring** — `ChatViewModel` observes the history through the `ChatHistoryRepository` interface, loads saved messages on `init`, and persists every new user and Gemini message. All database calls happen on background dispatchers via `suspend` functions.

---

## Preferences (DataStore)

A single user preference is stored with **DataStore Preferences** — the "Compact bubbles" toggle exposed from the settings gear next to the prompt input.

- **Interface** — `SettingsRepository` (exposes `compactBubbles: Flow<Boolean>` and `setCompactBubbles()`).
- **Implementation** — `PreferencesSettingsRepository` uses a `preferencesDataStore` named `user_preferences`.
- **Consumption** — `ChatViewModel` collects the Flow on `init` and mirrors the value into `ChatUiState.compactBubbles`; the UI recomposes automatically.

DataStore is used only for lightweight key–value preferences. Chat messages always stay in Room.

---

## Voice Input

Voice input uses Android's `SpeechRecognizer` API directly (not `ACTION_RECOGNIZE_SPEECH` as a launched Activity), giving proper error callbacks.

- **Permission** — `RECORD_AUDIO` is declared in the manifest and requested at runtime via `rememberLauncherForActivityResult`.
- **Behaviour** — tapping the microphone button prompts for permission (first time) and then opens the system recogniser. The recognised text is **appended** to whatever is already in the prompt field and remains editable before sending.
- **Error handling** — permission denial, no speech engine, no match, and timeout all surface as Snackbar messages without crashing the app.

### Testing the microphone on an emulator
Open the emulator's **Extended Controls (…) → Microphone** and enable **"Virtual microphone uses host audio input"**. The host machine's microphone must not be muted.

---

## Security

- The Gemini API key is loaded from `local.properties`, a file that is **excluded from version control** via `.gitignore`.
- The key is injected at build time as `BuildConfig.GEMINI_API_KEY` and is **never hardcoded** in source.
- The key is not printed to logs, not displayed in the UI, and not transmitted anywhere except Google's Gemini endpoint.
- If you fork or clone this repository, add your own `local.properties` with your own key. Do not commit it.

To confirm `local.properties` is ignored:
```bash
git check-ignore -v local.properties
```

---

## License

This project was built as part of an academic assignment. Refer to the repository for any licensing terms.