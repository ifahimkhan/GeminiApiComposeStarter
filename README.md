# GeminiApiComposeStarter — enhanced chat client

A Jetpack Compose chat client for the Gemini API, built on top of
[ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter).

## Setup

### 1. Get a Gemini API key

Create a key in [Google AI Studio](https://aistudio.google.com/app/apikey).

### 2. Configure it locally

Copy the example file and fill in your key:

```bash
cp local.properties.example local.properties
```

```properties
GEMINI_API_KEY=your_api_key_here
```

`local.properties` is listed in `.gitignore` — it is never committed. Gradle reads it at
build time (`app/build.gradle.kts`) and exposes the value as `BuildConfig.GEMINI_API_KEY`.
The key never appears as a string literal in any Kotlin file, XML resource, or committed
Gradle file.

### 3. CI / environment fallback

When `local.properties` has no key (e.g. on a CI runner), the build falls back to the
`GEMINI_API_KEY` environment variable, so CI can inject a repository secret without a
file on disk:

```kotlin
val geminiApiKey: String =
    (localProperties.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY"))
        ?.trim().orEmpty()
```

### 4. Run

Open in Android Studio and run on an emulator or device (`minSdk 26`), or:

```bash
./gradlew installDebug
```

## Encryption flow (`security/ApiKeyStore.kt`)

The key read from `BuildConfig.GEMINI_API_KEY` is sealed on first launch and only the
ciphertext is ever persisted:

1. On first use, `ApiKeyStore` generates a 256-bit AES key **inside the Android Keystore**
   via `KeyGenParameterSpec` (`PURPOSE_ENCRYPT/DECRYPT`, `BLOCK_MODE_GCM`,
   `ENCRYPTION_PADDING_NONE`, key size 256). The raw key material never leaves the
   hardware-backed (or software, on older devices) keystore.
2. The plaintext API key is encrypted with `AES/GCM/NoPadding`; only the Base64-encoded
   **ciphertext and IV** are written to a private `SharedPreferences` file
   (`gemini_secure_prefs`).
3. `GeminiRepositoryImpl.model` is a `by lazy` delegate — the ciphertext is decrypted back
   to plaintext **only at the instant `GenerativeModel` is constructed**, i.e. right before
   the first network call. The decrypted value is never logged, `Toast`ed, or displayed,
   and doesn't outlive that in-memory `GenerativeModel` instance.
4. `isMinifyEnabled = true` on the `release` build type turns on R8, so class/field names
   around this flow are obfuscated in the shipped APK on top of the encryption above.

### Limits

Client-side encryption raises the bar (a plain `strings` dump of the APK, or reading
`SharedPreferences` off a rooted device, no longer hands over the key) but it cannot fully
hide a secret from a sufficiently determined attacker with root or a debugger attached to
the running process — the app must decrypt the key in memory to use it, and that memory is
inspectable on a compromised device. A production app should instead:

- Move Gemini calls **behind a backend proxy** that holds the real API key server-side, so
  the client never has a usable credential to extract at all; or
- If calling Gemini directly from the client is unavoidable, protect the API key with
  **Firebase App Check** (so only your genuine, unmodified app binary can redeem it) and
  restrict the key in Google Cloud Console to your app's package name + SHA-1 signing
  certificate.

## Features implemented

- **Chat UI**: `LazyColumn` of Material 3 bubbles (user right-aligned/primary, Gemini
  left-aligned/surface), stable `key = message.id`, auto-scroll to the latest item/loading
  indicator via `animateScrollToItem`.
- **State hoisting**: all state lives in `ChatUiState`, exposed as a `StateFlow` from
  `ChatViewModel`, collected with `collectAsStateWithLifecycle()`; `ChatScreen` is stateless.
- **Responsive layout**: `calculateWindowSizeClass` caps the chat column at 720dp on
  `Expanded` width (tablets/landscape) instead of stretching bubbles edge to edge.
- **Loading/error states**: an inline loading bubble while waiting for a response, and a
  `Snackbar` for failures.
- **Dark mode / dynamic color**: `isSystemInDarkTheme()` + Material 3 dynamic color, now
  toggleable from the top bar and persisted (see below).
- **Voice input**: a mic button launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via
  `rememberLauncherForActivityResult`, requesting `RECORD_AUDIO` first.
- **Persistence**:
  - **Room** (`data/local/`) stores the full conversation so history survives app restarts.
  - **Preferences DataStore** (`data/datastore/`) remembers the dynamic-color toggle.

## Tests

```bash
# ChatViewModel unit tests (kotlinx-coroutines-test + fake repositories)
./gradlew testDebugUnitTest

# Compose UI tests (requires a connected device/emulator)
./gradlew connectedDebugAndroidTest
```

- `ChatViewModelTest` drives `ChatViewModel` against `FakeGeminiRepository`,
  `FakeChatHistoryRepository`, and `FakeUserPreferencesRepository` — no Android framework
  dependency, runs on the plain JVM.
- `ChatScreenTest` uses `createComposeRule()` to verify the placeholder, message bubbles,
  validation error text, and send interaction.

## CI

`.github/workflows/android-ci.yml` runs unit tests and assembles debug + release builds
on every push/PR, reading the key from a `GEMINI_API_KEY` repository secret via the
environment-variable fallback above — no `local.properties` on the runner.

## Secret hygiene

- `local.properties` is git-ignored; before every commit, run `git status` and confirm it
  is **not** staged.
- `local.properties.example` documents the expected format without a real key.
- Optional [gitleaks](https://github.com/gitleaks/gitleaks) pre-commit scan: enable with
  `git config core.hooksPath .githooks` (the hook no-ops if gitleaks isn't installed).
