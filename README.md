# Gemini Chat (Jetpack Compose)

A chat app for Android built with Jetpack Compose and Material 3. You type (or speak) a message, the app sends it to
Google's Gemini API, and the reply appears as a chat bubble. The conversation is saved on the device, so it is still
there after the app restarts.

Built for the *Mobile Application Development* lab (SVKM's NMIMS, School of Technology Management & Engineering),
starting from the `GeminiApiComposeStarter` project.

## Features

| Area | What the app does |
|---|---|
| Chat UI | `LazyColumn` of Material 3 bubbles with stable keys (message id) and auto-scroll to the newest message. Your messages are pink-to-purple gradient bubbles on the right; Gemini's are lavender bubbles with an avatar on the left; every bubble has a timestamp. |
| State hoisting | All UI state lives in `ChatUiState`, exposed as a `StateFlow` by `ChatViewModel` and collected with `collectAsStateWithLifecycle()`. The composables are stateless. |
| Responsive layout | `WindowSizeClass` limits the content and bubble widths on medium and expanded screens (tablets, landscape). |
| Loading and errors | A "Gemini is thinking..." bubble with a `CircularProgressIndicator`, and a `Snackbar` with a **Retry** action and friendly messages when Gemini is busy or a request times out. |
| Theme | Pink and purple palette in light and dark. A toolbar button cycles system / light / dark and the choice is remembered. |
| Voice input | The mic button launches `RecognizerIntent` through `rememberLauncherForActivityResult`; the recognised text is added to the message box. |
| Saved data | **Room** stores the chat history; **Preferences DataStore** stores the theme choice. |
| Extras | Suggestion chips on the empty screen, long-press a message to copy it, clear the chat (with confirmation). |
| Model fallback | If the main model is overloaded, slow or unavailable, the app tries the next model automatically (see below). |

## Getting started

**Requirements:** Android Studio (recent), an Android SDK, and a Gemini API key from
[Google AI Studio](https://aistudio.google.com/apikey).

1. Clone the repository and open the folder in Android Studio. Choose **Trust Project** when asked, then let Gradle sync.
2. In the project root, create or edit `local.properties` (Android Studio usually creates it with your `sdk.dir`) and add your key:
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   GEMINI_API_KEY=your_key_here
   ```
   No quotes and no spaces around `=`.
3. Pick an emulator or a phone and press **Run**.

`local.properties` is listed in `.gitignore`, so your key is not committed. Never put the key in a tracked file.

> The project has no `gradle-wrapper.jar`, so use Android Studio's Gradle sync and build, or run Gradle 9.5.0 yourself.

## How it is built

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt              wires Room, DataStore, the ViewModel and edge-to-edge bars
├── data/
│   ├── GeminiRepository.kt      interface (so the ViewModel can be tested with a fake)
│   ├── GeminiRepositoryImpl.kt  calls Gemini, with model fallback
│   ├── ChatMessage.kt           one bubble
│   ├── ChatHistoryRepository.kt interface + Room implementation
│   ├── SettingsRepository.kt    ThemeMode saved in DataStore
│   └── local/ChatDatabase.kt    Room entity, DAO and database
└── ui/
    ├── chat/                    ChatScreen, ChatComponents, ChatViewModel, ChatUiState
    ├── text/BoldMarkdown.kt     renders **bold** in replies
    └── theme/                   Color, Theme, Type
```

The data flows one way: the screen sends events to `ChatViewModel`, the ViewModel updates `ChatUiState` (and saves to
Room/DataStore), and the screen redraws from the new state.

### Model fallback

The app tries these models in order and uses the first one that answers:

1. `gemini-3.6-flash`
2. `gemini-3.5-flash`
3. `gemini-3.1-flash-lite`

It moves on when a model returns a 503 (overloaded), hits a rate limit, times out (45 s per model), is unavailable, or
returns an empty reply. It stops immediately on a bad API key, a blocked prompt or an unsupported region, because every
model would fail the same way.

## Tests

| Command | What it runs |
|---|---|
| `./gradlew :app:testDebugUnitTest` | 21 JVM tests: `ChatViewModelTest` (fake repositories + `kotlinx-coroutines-test`) and the model-fallback tests |
| `./gradlew :app:connectedDebugAndroidTest` | 6 Compose UI tests with `createComposeRule()`; needs a running emulator or device |
| `RUN_LIVE_TESTS=1 ./gradlew :app:testDebugUnitTest --tests "*GeminiLiveFallbackTest*"` | Opt-in test that calls the real Gemini API |

Running the connected tests uninstalls the app from the emulator afterwards, which also clears its saved chat.

## Known issues and limitations

- **Auto-scroll with long replies:** the list scrolls when a message is added, but not when a reply replaces the
  "thinking" bubble, so the end of a very long reply may not be scrolled into view. The UI test
  `longReply_replacingThinkingBubble_scrollsToTheBottom` documents this and currently fails.
- **Voice input on the emulator:** the Google speech screen opens, but the emulator needs its virtual microphone set to
  use the host's audio input (Extended controls, Microphone) and microphone permission for Android Studio on macOS.
  Voice input has not been confirmed end to end; a real phone is the reliable way to test it.
- **No conversation memory:** each message is sent to Gemini on its own, so Gemini does not see earlier messages in the chat.
- **Formatting:** replies render `**bold**` only. Headings, bullet lists and dividers appear as raw symbols.
- **Layouts:** the tablet and landscape layouts are implemented but have not been tested.
- **The API key is compiled into the app.** `local.properties` keeps it out of git, but it is embedded in the built app
  through `BuildConfig`. For a real product, keep the key on your own server and let the app call that instead.

## Tech stack

Kotlin 2.2, Jetpack Compose (BOM 2024.09) with Material 3, AGP 9.3.3 / Gradle 9.5.0, Room 2.8 with KSP,
Preferences DataStore 1.2, the Google AI client SDK (`generativeai` 0.9.0), min SDK 26, target SDK 36.
