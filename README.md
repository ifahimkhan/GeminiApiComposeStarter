# N068 — Gemini Chat Assignment

An Android chat application built with Kotlin, Jetpack Compose, and the Gemini API.

## Features

- Send text prompts and display Gemini responses.
- Display conversations in a `LazyColumn` with stable message IDs.
- Automatically scroll to new messages.
- Show loading indicators (`CircularProgressIndicator`) and error messages in Snackbars.
- Prevent duplicate requests while a response is loading.
- Enter prompts using speech recognition (`RecognizerIntent`).
- Choose System, Light, or Dark theme via Material 3 filter chips.
- Save theme preferences across app restarts using Jetpack Preferences DataStore.
- Save completed conversations locally using Room database.
- Restore saved conversations automatically when the app starts.
- Adapt layout spacing and message bubble widths to window size using `WindowWidthSizeClass` (phone, landscape, tablet).

## Architecture

The application strictly separates UI, state management, and data access layers:

- **ChatScreen:** Displays UI state, renders responsive chat bubbles, theme selector chips, and forwards user actions.
- **ChatViewModel:** Exposes unidirectional state via `StateFlow<ChatUiState>`, validates inputs, coordinates Gemini text generation, and manages local storage.
- **GeminiRepository / GeminiRepositoryImpl:** Interfaces with the Google Generative AI SDK, decrypting the API key on-demand.
- **UserPreferencesRepository:** Persists user theme preferences (`SYSTEM`, `LIGHT`, `DARK`) using Jetpack Preferences DataStore.
- **ChatHistoryRepository:** Persists conversation message history locally using Room database (`ChatDatabase`, `ChatMessageDao`, `ChatMessageEntity`).
- **ApiKeyStorage:** Encrypts and decrypts the Gemini API key using AES-256-GCM via Android Keystore and stores the ciphertext and initialization vector in DataStore.

The UI observes ViewModel state using lifecycle-aware collection (`collectAsStateWithLifecycle`). Asynchronous operations use Kotlin Coroutines and Flows.

## Setup

1. Clone this repository and open its root folder in Android Studio.
2. Ensure you have the Android SDK (API 36) installed.
3. Configure the Gradle JDK to Java 21 (or compatible JDK 17+).
4. Add your Gemini API key to the project root `local.properties` file:

   ```properties
   GEMINI_API_KEY=YOUR_ACTUAL_API_KEY
   ```

   Preserve any existing `sdk.dir` entry.

5. Sync the project with Gradle files.
6. Select an emulator or connected Android device.
7. Run the `app` configuration.

The build also supports the `GEMINI_API_KEY` environment variable as a fallback when the local property is unavailable.

> [!NOTE]
> Do not commit `local.properties` or any real API key to version control. The repository includes `local.properties.example` with placeholders only.

## Using the App

1. **Text Prompting:** Type a message into the text field and tap the Send button.
2. **Speech Recognition:** Tap the microphone icon to launch speech recognition. Speak your message, review the recognized text in the input field, and tap Send.
3. **Theme Selection:** Tap the System, Light, or Dark chips at the top to change the application theme. Your selection persists across app launches.
4. **Offline Conversation History:** Completed question-and-answer exchanges are saved in Room and restored when reopening the app. If a network request fails, your prompt is restored to the input box so you can retry.

## Tests

### Local ViewModel Unit Tests

Run from the project root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.fahim.geminiApiComposeStarter.ChatViewModelTest"
```

The eight ViewModel unit tests cover:

1. **Startup history loading:** Verifies stored messages from Room are loaded into UI state upon launch.
2. **Empty prompt validation:** Confirms empty or whitespace prompts trigger validation errors and prevent network requests.
3. **Missing API key prevention:** Ensures requests are blocked if the API key is unconfigured.
4. **Successful reply & exchange saving:** Validates that Gemini responses append both user and assistant messages and commit them to Room storage.
5. **Loading state & duplicate send prevention:** Confirms loading flags are set and duplicate requests during in-flight operations are blocked.
6. **Failure handling & prompt recovery:** Verifies that failed network requests restore the original prompt to the input field and remove incomplete turns.
7. **Storage failure resilience:** Ensures API replies remain displayed even if local Room saving fails, showing a non-fatal warning.
8. **History load failure handling:** Confirms sending is safely disabled if conversation history fails to load.

These tests use a fake `GeminiRepository` and MockK mocks for DataStore and Room repositories.

Recorded result: **8 tests passed, 0 failures, 0 skipped.**

### Instrumented Compose UI Tests

Start an emulator or connect an Android device, then run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Compose UI tests in `ChatScreenTest.kt` verify:
- Empty chat welcome screen and disabled send button.
- Typing in the input field enables the send button and passes text.
- Conversation displays user and assistant bubbles with proper styling.
- Loading state shows the indeterminate progress indicator and disables input actions.
- Dark theme selection chip invokes the callback.
- Microphone button invokes the voice input callback.
- Error messages display in the Snackbar.

Recorded result: **8 tests passed (including `ExampleInstrumentedTest`), 0 failures.**

## API-Key Security

The app implements Android Keystore encryption:
- Generates an **AES-256-GCM** key in Android Keystore on first launch.
- Encrypts the Gemini API key and stores the ciphertext and initialization vector (IV) in a private DataStore (`secure_api_key.preferences_pb`).
- Decrypts the key only in memory when instantiating the `GenerativeModel`.
- Excludes the encrypted key preferences file from Android Auto Backup and device transfer via `backup_rules.xml` and `data_extraction_rules.xml`.
- Enables code shrinking, resource shrinking, and R8 obfuscation (`isMinifyEnabled = true`, `isShrinkResources = true`) for release builds.

### Security Limitations in Production

Client-side key protection has inherent limitations:
- A key supplied through `BuildConfig` is compiled into the APK. Keystore encryption and R8 obfuscation mitigate casual discovery, but an attacker with root access or memory inspection tools can recover the key.
- **Production Recommendation:** Mobile applications should not store master API keys directly on client devices. Instead, requests should route through an authenticated backend proxy (or Firebase App Check with restricted API keys) that manages authorization, rate limiting, and quotas.

## Performance Inspection

Android Studio Layout Inspector was used to inspect Compose recomposition counts:

- **Device:** Pixel 9 (Android Emulator)
- **Typing a prompt:** Only the `PromptBar` / `OutlinedTextField` recomposes with keystrokes; the message list and top bar remain stable.
- **Scrolling the conversation:** `LazyColumn` item reuse operates efficiently with stable keys (`key = { message.id }`); bubbles recompose only as they enter the visible viewport.
- **Optimizations Made:**
  - Stable IDs applied to each `ChatMessage` to avoid re-rendering existing items when new messages arrive.
  - State hoisting and callbacks passed to child composables to minimize unnecessary parent recompositions.

## Submission Evidence

| Evidence | Screenshot |
|---|---|
| Light theme | [View](submission/screenshots/light-theme.png) |
| Dark theme | [View](submission/screenshots/dark-theme.png) |
| Landscape | [View](submission/screenshots/landscape.png) |
| Tablet | [View](submission/screenshots/tablet.png) |
| Voice input | [View](submission/screenshots/voice-input.png) |
| Restored history | [View](submission/screenshots/saved-history.png) |
| Unit tests | [View](submission/screenshots/unit-tests.png) |
| Instrumented tests | [View](submission/screenshots/ui-tests.png) |
| Layout Inspector | [View](submission/screenshots/layout-inspector.png) |
| Release build | [View](submission/screenshots/release-build.png) |

## Current Limitations

- Speech recognition requires a device or emulator with a functional Google Speech Recognition service installed.
- Conversation history is stored and displayed locally; current requests send individual prompts rather than multi-turn conversational history to Gemini.
- Clearing app data removes local conversation history and theme preferences.
