# Gemini Chat — Jetpack Compose (Lab Assignment 1)

An Android chat client for Google Gemini built with Jetpack Compose and Material 3, extending
[ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter).

## Features

| Area | What was implemented |
|---|---|
| Chat UI | `LazyColumn` of Material 3 bubbles (user right / Gemini left), stable keys, auto-scroll to newest message, empty state |
| State | `ChatUiState` exposed as `StateFlow` from `ChatViewModel`, collected with `collectAsStateWithLifecycle()`; composables are stateless and receive events through `ChatActions` |
| Responsive | `WindowSizeClass` → phone (full width, settings dialog), medium (centred 840dp column), expanded tablet (permanent settings pane); short landscape heights shrink the input |
| Loading / errors | "Gemini is thinking…" bubble with `CircularProgressIndicator`; `Snackbar` with **Retry** when a request fails |
| Dark mode | `isSystemInDarkTheme()` + Material 3 dynamic colour, overridable (System / Light / Dark) in settings |
| Voice input | Speech-to-text via `RecognizerIntent` + `rememberLauncherForActivityResult` |
| Personalisation | Preferences DataStore: theme, dynamic colour, response style (Concise / Balanced / Detailed → Gemini system instruction) |
| History | Room database; the conversation survives restarts and is sent to Gemini as multi-turn context; clear-chat button |
| Security | Key only in `local.properties` / env var, AES-256-GCM encryption at rest with an Android Keystore key, R8 obfuscation, backup exclusion, gitleaks |
| Tests | ViewModel unit tests (coroutines-test + fakes), Compose UI tests, Keystore instrumented test |

## 1. Where to put the API key

1. Create a key in [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Copy `local.properties.example` to `local.properties` in the project root (Android Studio usually
   creates `local.properties` already — then just add the line):

   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

3. Sync Gradle and run. Gradle reads the value and exposes it as `BuildConfig.GEMINI_API_KEY`.

`local.properties` is git-ignored — **never commit it**, and never paste the key into Kotlin, XML
or `build.gradle.kts`. Run `git status` before every commit and make sure it is not listed.

**CI:** when `local.properties` has no key, `app/build.gradle.kts` falls back to the
`GEMINI_API_KEY` environment variable. In GitHub add it under
*Settings → Secrets and variables → Actions → New repository secret*; the workflow in
`.github/workflows/android.yml` passes it to Gradle.

## 2. How the encryption flow works

```
local.properties ──(Gradle)──▶ BuildConfig.GEMINI_API_KEY
                                   │  first launch (GeminiApp.onCreate)
                                   ▼
             SecureApiKeyStore.ensureEncrypted()
                 • KeystoreCipher creates an AES-256-GCM key inside AndroidKeyStore
                   (KeyGenParameterSpec, key material never leaves the Keystore)
                 • encrypts the API key with a fresh random 12-byte IV
                 • stores ONLY Base64(IV) + Base64(ciphertext) in DataStore "secure_prefs"
                                   │
                                   ▼  first message sent
             GeminiRepositoryImpl.modelFor()
                 • calls SecureApiKeyStore.decryptApiKey()
                 • plaintext exists only in memory while GenerativeModel is created
                 • never logged, toasted or shown in the UI
```

Extra details:

* A SHA-256 fingerprint of the build key is stored so that changing the key in
  `local.properties` re-encrypts it on the next launch.
* `secure_prefs` is excluded from cloud backup and device transfer (the Keystore key is
  device-bound). If decryption ever fails (e.g. Keystore wiped), the ciphertext is discarded and
  re-created.
* Release builds use `isMinifyEnabled = true` and `isShrinkResources = true`, so R8 shrinks and
  obfuscates the APK.
* Debug logs only print the exception type, never request details.

## 3. Limits of client-side protection (and what production would do)

Encryption at rest and R8 **raise the bar but cannot hide the key**: the key is still compiled
into `BuildConfig`, so anyone who decompiles the APK or hooks the running app (Frida, a rooted
device) can recover it. A production app should never ship the key:

1. **Backend proxy (preferred).** The app calls your own server (Cloud Run / Cloud Functions);
   the server holds the Gemini key in Secret Manager, authenticates the user (Firebase Auth /
   OAuth), applies rate limits and quotas, and forwards the request to Gemini.
2. **Firebase AI Logic + App Check.** Call Gemini through Firebase AI Logic so the key stays on
   Google's side, and enforce **Firebase App Check** with the Play Integrity provider so only
   genuine, unmodified installs of your app can make requests.
3. **Restrict the API key** in Google Cloud Console → Credentials: allow only the Generative
   Language API, restrict it to Android apps with this package name + signing-certificate
   SHA-1, set quotas/budget alerts, and rotate it if it leaks.

## 4. Running the tests

```bash
# ViewModel unit tests (JVM, no device needed)
./gradlew testDebugUnitTest

# Compose UI tests + Keystore test (start an emulator first)
./gradlew connectedDebugAndroidTest

# Release build with R8 (checks obfuscation doesn't break anything)
./gradlew assembleRelease
```

In Android Studio you can also right-click `app/src/test` or `app/src/androidTest` →
**Run 'Tests in …'**. Reports: `app/build/reports/tests/` and
`app/build/reports/androidTests/connected/`.

| Test class | Covers |
|---|---|
| `ChatViewModelTest` | empty prompt validation, send success, loading state, duplicate-send guard, history + response style passed to Gemini, failure → error + retry, retry without duplicates, missing key, voice text, clear history, preferences, settings dialog |
| `ChatScreenTest` | empty state, user/model bubbles, loading indicator + disabled send, typing/send callbacks, mic callback, prompt error, error snackbar + Retry, tablet settings pane |
| `KeystoreCipherTest` | AES-GCM round trip, fresh IV per encryption |

## 5. Performance notes

* `LazyColumn` items use stable `key = message.id` and `contentType`, so new messages don't
  recreate existing bubbles.
* `ChatUiState`, `ChatMessage`, `UserPreferences` and `ChatActions` are `@Immutable`, so Compose
  can skip unchanged composables; the `**bold**` parsing is cached with `remember(text)`.
* The theme only observes `preferences` (`map { }.distinctUntilChanged()`), so chat updates don't
  recompose the theme.
* Network calls run on `Dispatchers.IO`; Room and DataStore are asynchronous; only the last
  20 messages are sent as context to keep requests small.
* Verify with **Layout Inspector → Show recomposition counts** while sending messages: existing
  bubbles should stay at 0 recompositions.

## 6. Optional: secret scanning before commits

```bash
pip install pre-commit
pre-commit install      # gitleaks now runs on every git commit
```

The CI workflow also runs gitleaks on every push and pull request.

## Project structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── GeminiApp.kt                 Application: builds AppContainer, encrypts key on first launch
├── MainActivity.kt              WindowSizeClass + theme from preferences
├── di/AppContainer.kt           manual dependency injection
├── security/                    KeystoreCipher (AES-256-GCM), SecureApiKeyStore (DataStore)
├── data/
│   ├── GeminiRepository(Impl)   multi-turn Gemini calls on Dispatchers.IO
│   ├── local/                   Room entity, DAO, database, ChatHistoryRepository
│   ├── prefs/                   Preferences DataStore repository
│   └── model/                   ChatMessage, UserPreferences
└── ui/
    ├── chat/                    ChatScreen, ChatViewModel, ChatUiState, ChatActions
    │   └── components/          MessageBubble, TypingBubble, SettingsContent
    └── theme/                   Material 3 theme with dynamic colour
```
