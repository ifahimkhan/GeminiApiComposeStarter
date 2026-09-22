# Gemini Chat

> A blue, Material 3 chat app for Android that talks to Google's Gemini API, built with Jetpack Compose.
> Type or speak a message, get a reply in a chat bubble, and pick up the conversation after a restart.

Built for the *Mobile Application Development* lab (SVKM's NMIMS, School of Technology Management & Engineering),
starting from the `GeminiApiComposeStarter` project.

## At a glance

- **Language / UI:** Kotlin, Jetpack Compose, Material 3
- **Architecture:** ViewModel + `StateFlow`, one-way data flow, repositories behind interfaces
- **Storage:** Room (chat history) and Preferences DataStore (theme)
- **AI:** Google Generative AI SDK, with automatic fallback between Gemini models
- **Android:** min SDK 26, target SDK 36
- **Tests:** 23 JVM unit tests and 6 Compose UI tests

## Quick start

1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Open the project in Android Studio, choose **Trust Project**, and let Gradle sync.
3. Add the key to `local.properties` in the project root (no quotes, no spaces around `=`):
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   GEMINI_API_KEY=your_key_here
   ```
4. Press **Run** on an emulator or a phone.

`local.properties` is git-ignored, so the key never reaches the repository.
The project has no `gradle-wrapper.jar`; use Android Studio's Gradle sync, or run Gradle 9.5.0 yourself.

## What you can do in the app

**Chat**
- Send a message and read Gemini's reply in a bubble. Your bubbles are blue and on the right; Gemini's are light blue with an avatar on the left. Each has a timestamp.
- The list follows the conversation: it scrolls to the newest message.
- A "Gemini is thinking..." bubble with a spinner shows while a reply is on its way.
- If a request fails, a snackbar explains why ("Gemini is busy", "The request timed out") and offers **Retry**.
- Long-press any message to copy it.

**Voice**
- Tap the mic and speak; the recognised text is added to the message box (uses `RecognizerIntent`).

**Look and feel**
- Blue theme in light and dark. The moon/sun button switches between them; until you choose, the app follows the phone's setting. Your choice is remembered.
- Layout adapts to the screen size (phone, tablet, landscape) through `WindowSizeClass`.

**Your data**
- The conversation is saved on the device and is still there after the app restarts.
- **Clear conversation** lives in the top-right menu. It clears at once and shows an **Undo** snackbar.

## Behind the scenes

### Where things live

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
  MainActivity.kt               wires everything together
  data/
    GeminiRepository(.Impl).kt  the Gemini call, with model fallback
    ChatMessage.kt              one bubble
    ChatHistoryRepository.kt    saved chat (Room)
    SettingsRepository.kt       saved theme (DataStore)
    local/ChatDatabase.kt       Room entity, DAO, database
  ui/
    chat/                       ChatScreen, ChatComponents, ChatViewModel, ChatUiState
    text/BoldMarkdown.kt        renders **bold** in replies
    theme/                      Color, Theme, Type
```

### How a message travels

```
screen --event--> ChatViewModel --> ChatHistoryRepository (Room)
                       |                       ^
                       v                       |
                GeminiRepository --> reply ----+
                       |
                       v
                  ChatUiState (StateFlow) --> screen redraws
```

### Model fallback

The app asks the models below in order and uses the first one that answers:

1. `gemini-3.6-flash`
2. `gemini-3.5-flash`
3. `gemini-3.1-flash-lite`

It moves on when a model is overloaded (503), rate limited, unavailable, slow (45 seconds per model) or returns an
empty reply. It stops straight away for a bad API key, a blocked prompt or an unsupported region, because every
model would fail the same way.

## Testing

```bash
./gradlew :app:testDebugUnitTest          # 23 unit tests
./gradlew :app:connectedDebugAndroidTest  # 6 UI tests, needs an emulator or phone
RUN_LIVE_TESTS=1 ./gradlew :app:testDebugUnitTest --tests "*GeminiLiveFallbackTest*"   # opt-in, calls the real API
```

- **Unit tests:** `ChatViewModelTest` uses fake repositories and `kotlinx-coroutines-test` (validation, success, failure,
  retry, loading, missing key, clear and undo, voice text, theme, error messages). `GeminiRepositoryFallbackTest`
  covers the model fallback.
- **UI tests:** `ChatScreenTest` uses `createComposeRule()`.
- Running the connected tests uninstalls the app from the device afterwards, which also clears its saved chat.

## Known issues

- [ ] **Long replies and auto-scroll:** the list scrolls when a message is added, but not when a reply replaces the
  "thinking" bubble, so the end of a very long reply may stay off screen. The UI test
  `longReply_replacingThinkingBubble_scrollsToTheBottom` shows this and currently fails.
- [ ] **Voice on the emulator:** the emulator needs its virtual microphone set to use the host audio (Extended controls,
  Microphone) and microphone permission for Android Studio in macOS. Voice input has not been confirmed end to end; a
  real phone is the reliable way to check it.
- [ ] **No conversation memory:** every message is sent to Gemini on its own, so Gemini does not see earlier messages.
- [ ] **Formatting:** replies render `**bold**` only; headings, bullet lists and dividers show as raw symbols.
- [ ] **Screen sizes:** the tablet and landscape layouts are built but have not been tested.
- [ ] **API key in the app:** `local.properties` keeps the key out of git, but it is compiled into the built app through
  `BuildConfig`. For a real product, keep the key on your own server and let the app call that.

## Built with

Kotlin 2.2 - Jetpack Compose (BOM 2024.09) and Material 3 - AGP 9.3.3 and Gradle 9.5.0 - Room 2.8 with KSP -
Preferences DataStore 1.2 - Google AI client SDK `generativeai` 0.9.0
