# Gemini API Compose Chat — Lab Assignment 1

Built on top of [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter) for
**Mobile Application Development (702AI0E002)**.

## Setup

1. Clone this branch and open the project in Android Studio (Ladybug or newer, AGP 9.0 / Kotlin 2.0).
2. Get a Gemini API key from [Google AI Studio](https://ai.google.dev/gemini-api/docs/quickstart).
3. In the project root, copy the example file and fill in your key:
   ```bash
   cp local.properties.example local.properties
   ```
   Then edit `local.properties`:
   ```
   GEMINI_API_KEY=your_actual_key_here
   ```
   `local.properties` is git-ignored — it will never be committed. **Do not** paste the key
   into any `.kt` file, `strings.xml`, or `build.gradle.kts`.
4. Sync Gradle and run on an emulator or device (minSdk 26).

### CI builds

If there's no `local.properties` on the build machine (e.g. a CI runner), `app/build.gradle.kts`
falls back to the `GEMINI_API_KEY` environment variable, which should be set from a repository
secret rather than a checked-in file.

## How the key is protected

1. **Build time**: `local.properties` (or the CI env var) is read once by Gradle and baked into
   `BuildConfig.GEMINI_API_KEY`. It is never a string literal anywhere in source.
2. **First app launch**: `UserPreferencesRepository.resolveApiKey()` takes that raw value,
   generates an AES-256-GCM key inside the Android Keystore (`ApiKeyVault`, using
   `KeyGenParameterSpec` — the key material never leaves the Keystore/TEE), encrypts the API
   key with it, and persists only the resulting **ciphertext + IV** in Preferences DataStore.
3. **Every subsequent launch**: the ciphertext is decrypted in memory, only at the point a
   `GenerativeModel` is constructed (`GeminiRepositoryImpl.getOrCreateModel()`), and is never
   logged, toasted, or displayed.
4. **Release builds**: `isMinifyEnabled = true` turns on R8, so class/field names and the
   decrypted-key code path aren't trivially readable from a decompiled release APK.

### Limits of this approach

Client-side encryption raises the bar for casually extracting the key from a rooted device or
a decompiled APK, but it can't fully hide a key from a sufficiently determined attacker — the
app still has to decrypt it to make API calls, so the plaintext exists in the process's memory
at call time. A production app would instead keep the Gemini key server-side and proxy calls
through a thin backend (or gate direct client calls with Firebase App Check + an API key
restricted to the app's signing certificate and package name), so the key is never shipped to
the client at all.

## Features implemented

- **Chat UI**: `LazyColumn` of Material 3 bubbles (user right-aligned, Gemini left-aligned),
  stable-keyed by message id, auto-scrolls to the latest turn.
- **State hoisting**: all UI state lives in `ChatUiState` behind a `StateFlow` in
  `ChatViewModel`; every composable below `ChatScreen` is stateless.
- **Responsive layout**: `WindowSizeClass` caps content width and padding on medium/expanded
  windows (tablets, landscape) instead of stretching bubbles edge to edge.
- **Loading/error states**: an inline "Gemini is thinking…" bubble while waiting, and a
  Snackbar for API/network errors.
- **Dark mode**: follows the system theme by default, with a top-bar toggle
  (system → light → dark) whose choice is remembered via DataStore.
- **Voice input**: mic button launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`
  (`rememberLauncherForActivityResult`), with a runtime `RECORD_AUDIO` permission request.
- **Persistence**: chat history is stored in Room (`chat_messages` table) and survives app
  restarts; the theme preference is stored in Preferences DataStore.

## Tests

- **Unit tests** (`app/src/test`): `ChatViewModelTest` covers empty-prompt validation, the
  missing-API-key path, a successful send (user + model turns appended, prompt cleared),
  a failed send (error surfaced, user turn still recorded), and voice-result handling — all
  against a `FakeGeminiRepository` and an in-memory `FakeChatMessageDao`, using
  `kotlinx-coroutines-test`.
  ```bash
  ./gradlew test
  ```
- **Compose UI tests** (`app/src/androidTest`): `ChatScreenTest` verifies messages render as
  bubbles and that typing + tapping send invokes the right callbacks, via `createComposeRule()`.
  ```bash
  ./gradlew connectedAndroidTest
  ```

## Submission checklist

- [x] Branch name starts with roll number (`C011-...`)
- [x] `local.properties` never staged/committed; `local.properties.example` provided instead
- [x] No API key in any `.kt`/`.xml`/`.gradle.kts` file or in git history
- [ ] PR description with screenshots — add after running the app and capturing screens
