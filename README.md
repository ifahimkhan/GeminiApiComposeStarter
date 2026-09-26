# Gemini API Compose Starter

A Jetpack Compose chat UI backed by the Gemini API, rendering the conversation as a
scrolling list of Material 3 bubbles. The conversation persists across app restarts,
the theme (dark/light) is remembered per device, and prompts can be typed or spoken.

## Setup

### 1. Get a Gemini API key

Create a key in [Google AI Studio](https://aistudio.google.com/app/apikey).

### 2. Provide the key to the build

Pick one of the two options below — the build checks `local.properties` first, then
falls back to the `GEMINI_API_KEY` environment variable.

**Local development:** create `local.properties` in the project root (already
git-ignored; see `local.properties.example`) with:

```
GEMINI_API_KEY=your_api_key_here
```

**CI:** set a `GEMINI_API_KEY` repository secret / environment variable. No
`local.properties` file is needed on CI runners.

The key is never hardcoded in Kotlin, XML or a committed Gradle file — `app/build.gradle.kts`
reads it into `BuildConfig.GEMINI_API_KEY` at build time only.

## How the key is protected at rest

`BuildConfig.GEMINI_API_KEY` is the key's *build-time* source, but the app doesn't call
the Gemini API with that value directly. On first launch, `SecureApiKeyStore`
(`app/src/main/java/.../data/SecureApiKeyStore.kt`) seeds an `EncryptedSharedPreferences`
store whose master key is an AES-256-GCM key generated and held inside the Android
Keystore (`MasterKey.KeyScheme.AES256_GCM`). From then on, only ciphertext is persisted
on disk — both the preference value and its key name are encrypted. The key is decrypted
back into memory only at the moment `MainActivity` constructs the `GenerativeModel`; it
is never logged, toasted or displayed.

Release builds also set `isMinifyEnabled = true`, so R8 obfuscates the shipped APK and
the key isn't trivially readable from decompiled code (see `app/proguard-rules.pro` for
the keep rules this required for the Gemini SDK and Tink).

### Known limits

Client-side encryption raises the bar but can't fully hide a key from a determined
attacker with a rooted device or a debugger attached to the app's process — the key has
to exist in plaintext in memory at the moment it's used. A production app should instead
keep Gemini calls behind a backend proxy that holds the real key, and/or restrict the
client-side key with [Firebase App Check](https://firebase.google.com/docs/app-check)
so it's only usable from an attested build of this app.

## Persistence and preferences

- **Chat history** (`ChatHistoryRepository` / `ChatHistoryRepositoryImpl`) is stored as a
  JSON array in Preferences DataStore and is the single source of truth for the
  conversation shown on screen — `ChatViewModel` collects it directly instead of holding
  its own copy, so the conversation survives process death and app restarts.
- **Theme preference** (`UserPreferencesRepository`) remembers a dark/light override in
  DataStore, toggled from the switch in the top bar; when unset it follows the system
  setting.
- Room was the library named in the assignment for chat history, but its KSP annotation
  processor does not currently work with this project's AGP 9 build (`kspDebugKotlin`
  fails with an internal KSP error under AGP's built-in Kotlin compilation), and falling
  back to the classic standalone Kotlin Gradle plugin conflicts with AGP 9's own Kotlin
  integration (`Cannot add extension with name 'kotlin'`). DataStore avoids needing an
  annotation processor at all, so it was used instead.

## Voice input

The mic button next to Send launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via
`rememberLauncherForActivityResult`, and fills the prompt field with the top recognized
result. If no speech-recognition activity is available on the device, it surfaces an
error Snackbar instead of crashing. The `<queries>` entry in `AndroidManifest.xml` is
required for the app to see the recognizer at all under API 30+ package visibility.

## Running the app

```
./gradlew installDebug
```

(or open the project in Android Studio and run the `app` configuration).

## Tests

- `app/src/test/.../ui/chat/ChatViewModelTest.kt` — unit tests against fake repositories
  (`FakeGeminiRepository`, `FakeChatHistoryRepository`, `FakeUserPreferencesRepository`),
  covering empty-prompt validation, the missing-key path, a successful send, a failed
  send, voice-input-unavailable, and the dark-mode toggle.
- `app/src/androidTest/.../ui/chat/ChatScreenTest.kt` — Compose UI tests with
  `createComposeRule()` covering the empty-conversation placeholder, message rendering,
  the prompt-error message, and typing plus sending.

Run them with:

```
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # needs a running emulator or device
```
