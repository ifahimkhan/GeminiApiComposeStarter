# Gemini Chat (Jetpack Compose)

A Material 3 chat app built on the Gemini Jetpack Compose starter. Chat bubbles, saved history,
voice input, adaptive layouts, an API key that is encrypted at rest, and unit + UI tests.

> **AI assistance disclosure:** parts of this project (code, tests and this README) were written
> with the help of an AI assistant (Claude by Anthropic), as permitted for this assignment. I
> reviewed, built and tested the result myself.

## 1. Where to put the API key

1. Create a key in [Google AI Studio](https://ai.google.dev/gemini-api/docs/quickstart).
2. Copy `local.properties.example` to `local.properties` in the project root (it is git-ignored)
   and set:

   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
3. Build and run. Gradle reads the key at build time and exposes it as `BuildConfig.GEMINI_API_KEY`.

**CI builds:** if `local.properties` has no key, `app/build.gradle.kts` falls back to the
`GEMINI_API_KEY` environment variable, so use a repository secret instead of a file.

The key must never appear in Kotlin, XML or committed Gradle files. Before each commit run
`git status` and check that `local.properties` is not staged. To enforce this automatically,
install the optional hook: `cp scripts/pre-commit .git/hooks/pre-commit`.

## 2. How the key is protected

| Layer | What it does |
|---|---|
| **Not in VCS** | `local.properties` is git-ignored; only `local.properties.example` (placeholder) is committed. |
| **Encrypted at rest** | `security/KeystoreCipher` creates an **AES-256-GCM** key inside the **Android Keystore** (`KeyGenParameterSpec`). |
| **Ciphertext only on disk** | `security/ApiKeyStore` stores only `Base64(IV + ciphertext)` in a dedicated DataStore file (`secure_prefs`). |
| **Decrypted in memory only** | `GeminiRepositoryImpl` asks `ApiKeyStore.getApiKey()` the first time a request is made and hands the value straight to `GenerativeModel`. It is never logged, toasted or shown. |
| **Excluded from backup** | `backup_rules.xml` / `data_extraction_rules.xml` exclude the ciphertext file (the Keystore key does not move between devices, so a restored copy would be useless). |
| **Obfuscated release** | `isMinifyEnabled = true` (R8) for the release build type. |

**Encryption flow**

```
first launch:   BuildConfig key --KeystoreCipher.encrypt--> IV + ciphertext --> DataStore
each session:   DataStore --> KeystoreCipher.decrypt --> key (in memory) --> GenerativeModel
```

If the stored ciphertext cannot be decrypted (Keystore key wiped, corrupt data) or the key in
`local.properties` changed, it is simply re-encrypted from the build-time key.

### Limits of client-side protection

Encryption at rest raises the bar but cannot fully hide a key from a determined attacker. The
key still ships inside the APK at build time (via `BuildConfig`), and a rooted device or a
debugger attached to the running process can read the decrypted value from memory. In a
production app I would:

- **Move Gemini calls behind a backend proxy.** The app authenticates the user and calls
  *your* server, which holds the real key and forwards requests. The key never reaches the client.
- **Or use Firebase AI Logic with Firebase App Check**, so requests are accepted only from
  genuine, unmodified installs of the app (Play Integrity), and keep the API key restricted
  (package name + SHA-1, and only the Generative Language API).
- Set quotas and budget alerts, and rotate the key if it is ever exposed.

## 3. Features

- **Chat bubbles** in a `LazyColumn` with stable keys (Room row id), auto-scroll to the newest message.
- **State hoisting:** all UI state lives in `ChatUiState`, exposed as a `StateFlow` from
  `ChatViewModel`, collected with `collectAsStateWithLifecycle()`. Composables below `ChatRoute` are stateless.
- **Responsive layout:** `WindowSizeClass` picks the bubble width; the content column is capped at
  840dp so tablets and landscape stay readable.
- **Loading and errors:** `CircularProgressIndicator` bubble while waiting, Snackbar on failure
  (the prompt is restored so you can retry).
- **Dark mode + dynamic colour:** follows the system by default, or choose Light / Dark and toggle
  Material You colours from the top-bar menu.
- **Voice input:** mic button launches `RecognizerIntent` through `rememberLauncherForActivityResult`.
- **Personalisation:** theme choice and dynamic-colour setting stored in **Preferences DataStore**.
- **Persistent history:** conversations stored in **Room** and restored after a restart ("Clear chat" in the menu).

## 4. Architecture

```
ui/chat      ChatScreen (stateless UI) - ChatViewModel - ChatUiState
data         GeminiRepository(+Impl), ChatHistoryRepository (Room), SettingsRepository (DataStore)
data/local   MessageEntity, MessageDao, ChatDatabase
security     SecretCipher, KeystoreCipher, ApiKeyStore
AppContainer manual dependency wiring, created once in GeminiApplication
```

## 5. Running the tests

```bash
# Unit tests (ChatViewModel with a fake GeminiRepository, kotlinx-coroutines-test)
./gradlew testDebugUnitTest

# Instrumented tests (Compose UI tests with createComposeRule + Keystore round trip);
# needs a running emulator or device
./gradlew connectedDebugAndroidTest
```

## 6. Performance notes

- Network calls run on `Dispatchers.IO`; Room and DataStore are main-safe suspend/Flow APIs.
- Message rows use stable keys and immutable data classes so unchanged bubbles are skipped
  during recomposition. Recomposition counts can be checked with Android Studio's Layout Inspector.

## 7. Voice input on the emulator

The emulator needs Google's speech services (use a "Google Play" system image). If no
recogniser is installed the app shows "Speech recognition is not available on this device."
