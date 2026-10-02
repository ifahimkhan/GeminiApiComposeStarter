# Gemini Chat (Jetpack Compose)

An Android chat app for Google's Gemini API, built with Jetpack Compose and Material 3.
It started from [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter) and was extended for Lab Assignment 1.

## Features

| Area | What's implemented |
|---|---|
| Chat UI | `LazyColumn` of Material 3 bubbles (user on the right, Gemini on the left) with stable keys, auto-scroll to the newest message, selectable text and `**bold**` rendering |
| State | All screen state lives in `ChatUiState`, exposed as a `StateFlow` from `ChatViewModel` and collected with `collectAsStateWithLifecycle()`. Composables are stateless. |
| Responsive layout | `WindowSizeClass`: content is capped at 840 dp on tablets and wide screens, bubbles are narrower there, and the input shrinks to one line in phone landscape |
| Loading and errors | A typing bubble with a `CircularProgressIndicator` while waiting. On failure a Snackbar shows a safe message, the unanswered turn is rolled back and the prompt is restored for retry. |
| Theming | Light, dark or follow-system mode (`isSystemInDarkTheme()`), plus a Material You dynamic colour toggle |
| Voice input | Mic button launches `RecognizerIntent` through `rememberLauncherForActivityResult`. It is hidden when no recogniser is installed. |
| History | Conversations are stored in **Room** and survive restarts. The last 20 turns are sent to Gemini as context. |
| Preferences | Theme mode and dynamic colour are stored in **Preferences DataStore** |
| Security | API key encrypted at rest with Android Keystore AES-256-GCM, R8 in release, no raw errors or logs in release, cleartext HTTP disabled, 4,000-character prompt cap |

## 1. Where to put the API key

1. Get a key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Copy `local.properties.example` to `local.properties` in the project root, or edit the one Android Studio created, and add:

   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

3. Rebuild. Gradle reads the value and exposes it as `BuildConfig.GEMINI_API_KEY`.

`local.properties` is git-ignored. **Never** put the key in Kotlin, XML or any committed Gradle file.
Before committing, run `git status` and check that `local.properties` is not staged.

**CI:** when `local.properties` has no key, `app/build.gradle.kts` falls back to the `GEMINI_API_KEY` environment variable.
In GitHub Actions this comes from a repository secret (*Settings → Secrets and variables → Actions → `GEMINI_API_KEY`*).
See `.github/workflows/android.yml`.

**Secret scanning:** CI runs [gitleaks](https://github.com/gitleaks/gitleaks) on every push.
To scan locally before each commit, run `pip install pre-commit && pre-commit install`.

## 2. How the encryption flow works

```
 build time                         first launch                              first message
 ──────────                         ────────────                              ─────────────
 local.properties ──► BuildConfig ──► SecureApiKeyStore.provision()           GeminiRepositoryImpl
                                        │ AES-256-GCM key generated inside      │ needs a model
                                        │ the Android Keystore (never leaves    ▼
                                        │ secure hardware)                    SecureApiKeyStore.decrypt()
                                        │ encrypt(apiKey) → IV ‖ ciphertext     │ plaintext only in memory
                                        ▼                                       ▼
                                      DataStore "secure_api_key"             GenerativeModel(apiKey)
                                      (Base64 ciphertext + SHA-256
                                       fingerprint, no plaintext)
```

- `KeystoreCipher` ([source](app/src/main/java/com/fahim/geminiApiComposeStarter/data/security/KeystoreCipher.kt)) generates a 256-bit AES key with `KeyGenParameterSpec` (GCM mode, no padding, randomised IV required).
  Each encryption uses a fresh 12-byte IV, and the 128-bit GCM tag detects tampering.
- `SecureApiKeyStore` ([source](app/src/main/java/com/fahim/geminiApiComposeStarter/data/security/SecureApiKeyStore.kt)) persists **only** the ciphertext.
  A SHA-256 fingerprint lets it re-encrypt when you change the key in `local.properties`.
  If the Keystore key is lost, for example after a restore to a new device, it throws away the stale blob and encrypts again.
- `GeminiRepositoryImpl` calls `decrypt()` lazily, only when the `GenerativeModel` is first created.
  The decrypted key is never logged, shown or written anywhere.
- The ciphertext file is excluded from cloud backup and device transfer. It would be useless elsewhere because the Keystore key stays on the device.
- Release builds run R8 (`isMinifyEnabled = true`, `isShrinkResources = true`), so the code is obfuscated.

### Limits: this is not enough for production

Client-side protection only raises the bar. The key is still compiled into `BuildConfig`, so anyone who decompiles the APK or hooks the running app can recover it.
A production app should never ship a long-lived API key. Instead:

1. **Backend proxy (preferred).** The app calls your own server, for example Cloud Run or Cloud Functions.
   The server holds the Gemini key in a secret manager, authenticates users (for example Firebase Auth ID tokens), applies per-user rate limits and quotas, and forwards the request to Gemini.
   The key never reaches the device.
2. **Firebase AI Logic + App Check.** Call Gemini through the Firebase AI Logic SDK, which replaces the deprecated `com.google.ai.client.generativeai` SDK used here.
   Enforce **App Check** with the Play Integrity provider, so only genuine, unmodified copies of your app on real devices can make requests. No Gemini key is embedded in the app.
3. **Restricted API keys.** If a key must exist, restrict it in Google Cloud Console to the *Generative Language API* only, with Android app restrictions (package name and signing-certificate SHA-1).
   Set quota caps and budget alerts, and rotate the key if it leaks.

## 3. Running the app and the tests

Requirements: Android Studio (AGP 9, JDK 17+) and an emulator or device running API 26+.

```bash
./gradlew installDebug                 # build and install

./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # instrumented tests (needs a running emulator/device)
./gradlew lintDebug                    # Android Lint
```

| Suite | Location | Covers |
|---|---|---|
| `ChatViewModelTest` | `app/src/test` | Validation, prompt cap, success and failure paths, rollback, loading state and duplicate sends, context history, voice input, clear history, preferences. Uses `kotlinx-coroutines-test` and fake repositories. |
| `SecureApiKeyStoreTest` | `app/src/test` | No plaintext persisted, round trip, idempotent provisioning, key rotation, recovery from an undecryptable blob |
| `ChatScreenTest` | `app/src/androidTest` | Compose UI tests with `createComposeRule()`: empty state, bubbles, send callbacks, loading indicator, prompt error, Snackbar, mic button |
| `KeystoreCipherTest` | `app/src/androidTest` | Real Android Keystore: round trip, random IV, tamper detection |

## Project structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── GeminiApp.kt / AppContainer.kt    Application + manual dependency wiring
├── MainActivity.kt                   Theme + WindowSizeClass, hosts ChatRoute
├── data/
│   ├── GeminiRepository(Impl).kt     Gemini calls (Dispatchers.IO), safe error mapping
│   ├── ChatHistoryRepository.kt      Room-backed history
│   ├── UserPreferencesRepository.kt  DataStore-backed preferences
│   ├── local/                        Room entity, DAO, database
│   ├── model/                        ChatMessage, UserPreferences
│   └── security/                     KeystoreCipher, SecureApiKeyStore
└── ui/
    ├── chat/                         ChatScreen, MessageBubble, ChatViewModel, ChatUiState
    ├── text/BoldMarkdown.kt
    └── theme/
```

## Performance notes

- Network calls run on `Dispatchers.IO` inside the repository. The main thread only updates state.
- `ChatUiState`, `ChatMessage` and `UserPreferences` are `@Immutable`, and list items have stable `key`s and `contentType`s.
  Unchanged bubbles therefore skip recomposition, which you can check with Layout Inspector's recomposition counts.
- Markdown parsing for each bubble is cached with `remember(message.text)`.
