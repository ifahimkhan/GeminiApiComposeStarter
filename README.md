# Gemini Compose Chat — N077 Assignment 1

Merant Patel · N077 · Mobile Application Development

An extension of the original GeminiApiComposeStarter Android app. Ask Gemini questions through a Material 3 conversation interface, dictate a prompt, and retain conversation history on your device. The runtime model is fixed to **gemini-3.6-flash** with no fallback or model substitution.

## Functionality

- LazyColumn conversation with distinct user/Gemini bubbles, stable database IDs, selectable formatted text and automatic scrolling.
- ChatUiState and a ViewModel-owned StateFlow, collected using collectAsStateWithLifecycle().
- WindowSizeClass layouts for phone, tablet and landscape, with bounded reading width and keyboard insets.
- Loading progress, duplicate-send protection, empty-input validation, safe Snackbar errors and restored prompts after failures.
- RecognizerIntent speech input through rememberLauncherForActivityResult; unavailable recognition is handled gracefully. Recognition depends on an installed speech provider and its permissions/network support.
- System/light/dark theme preferences in Preferences DataStore; Material 3 dynamic colors on Android 12 and later.
- Room message history across process restarts and confirmed clear-history. Room suspend APIs and background network/Keystore work keep I/O off the main thread.

## API key and local setup

Create a Gemini API key in [Google AI Studio](https://aistudio.google.com/apikey) with access to gemini-3.6-flash. Copy local.properties.example to **local.properties** in the repository root and replace its placeholder locally:

```properties
GEMINI_API_KEY=your_api_key_here
```

The filename must be local.properties, without a .txt suffix. Do not put the actual key in source, resources, documentation, screenshots or commits. local.properties and accidental suffixed copies are ignored; only the placeholder example is tracked. Android Studio can add sdk.dir to that local file. Alternatively set ANDROID_HOME to your Android SDK location.

Gradle reads the key from local.properties and exposes BuildConfig.GEMINI_API_KEY. If the local value is absent or blank, it falls back to the **GEMINI_API_KEY** environment variable, then an empty string. In CI, configure a masked repository secret with that environment-variable name. A missing key does not prevent compilation; the app reports setup instructions when sending a prompt.

## Android Keystore flow and limits

At app initialization, SecureApiKeyStore creates or reuses a non-exportable AES-256 key in Android Keystore using KeyGenParameterSpec, GCM mode and no padding. It encrypts the configured key with a fresh random 96-bit IV and a 128-bit authentication tag. Preferences DataStore persists only the Base64 envelope containing the IV, ciphertext and tag. Reinitialization refreshes that envelope, allowing rebuilt API keys to rotate without persisting plaintext.

Only the Gemini repository requests decryption, in memory immediately before creating GenerativeModel. Requests run on Dispatchers.IO with a timeout. API exception details and keys are never logged or displayed. Backups and device transfer are excluded so ciphertext is not restored without its device-bound Keystore key. Clearing chat history does not remove the API-key envelope or theme preference.

**Client-side protection cannot fully hide a secret.** BuildConfig necessarily embeds the build-time key in the APK; encryption protects the additional persisted copy, not the packaged key. R8 release minification is enabled but does not make embedded credentials unrecoverable. A determined attacker or compromised device can inspect the APK or process memory. Conversation text is stored in the app-private Room database, not encrypted by this key-protection mechanism.

For production, move Gemini calls behind an authenticated backend proxy that holds the server-side key and applies rate limits. Where the chosen Firebase integration supports it, enforce Firebase App Check, restrict API keys to the supported API/application scope, monitor usage and rotate compromised keys. Never treat obfuscation or App Check as complete authorization or secret storage.

## Build and run

Use an Android SDK with platform 36 and a JDK supported by Gradle 9.3.1 (JDK 17 or later). This project uses AGP 9 built-in Kotlin; the Compose compiler plugin is aligned with its Kotlin 2.2.10 baseline. KSP generates Room implementations and the schema in app/schemas.

Open this existing project in Android Studio, sync Gradle, select an API 26+ device/emulator, and run app. On Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease
.\gradlew.bat :app:installDebug
```

On macOS/Linux use ./gradlew instead. The official Gradle wrapper JAR is included and the distribution has a pinned SHA-256 checksum. Release output is minified and unsigned; configure your own signing credentials outside Git for distribution. Do not share a key-bearing APK publicly.

## Tests

Unit tests use kotlinx-coroutines-test and a fake GeminiRepository to exercise success, loading, failure, missing key, validation, duplicate sends, database failures, history and preferences:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Start an emulator or connect a device before running the Compose createComposeRule() tests and Room persistence tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Compose tests use deterministic callbacks and fake content; they do not prove a real Gemini response or microphone transcription. Room tests close and reopen an on-disk test database and verify that clearing persists. Reports are under app/build/reports/tests and app/build/reports/androidTests. Additional runtime verification and the requirement checklist are recorded in docs/ASSIGNMENT_AUDIT.md.

Run static Android checks with .\gradlew.bat :app:lintDebug. Before each commit run git status, git diff, git check-ignore local.properties and python scripts/secret_audit.py. The audit compares configured keys without printing them and scans working files, staged files, diffs and reachable Git objects for credentials.

## Manual runtime checks

Send a question with the configured key and verify the exact model responds; test airplane-mode failure and retry, empty input, speech entry, system/light/dark themes, rotation, a tablet-size window, restart persistence and clear-history. Use Android Studio Layout Inspector recomposition counts during scrolling and typing to inspect recomposition performance. Lazy item keys and content types limit unnecessary list work; no claim of measured recomposition performance is made without an actual inspector session.

## Submission

The assignment PR must originate from merantpatel:N077_Assignment1 and target ifahimkhan/GeminiApiComposeStarter:master. Its title begins with N077. Include feature/test results and screenshots, then attach the exact PR URL to the assignment submission. Teams final submission requires the student's explicit approval.

## Verified assignment results

Debug and minified release builds and Android lint passed. All 12 ViewModel tests and 14 emulator tests passed, including a real gemini-3.6-flash response. The final system-bar dark-mode change passed an additional targeted live/theme test. See [the requirement audit](docs/ASSIGNMENT_AUDIT.md) for evidence and verification limits.

To opt into the real API test (uses your configured key and quota):

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.liveGemini=true'
```

Without this property, the live network test is skipped; deterministic UI/storage/voice-result tests still run. Emulator screenshots show [light chat](docs/screenshots/phone-light-conversation.png), [dark chat](docs/screenshots/phone-dark-conversation.png), [loading](docs/screenshots/phone-loading.png), [tablet width](docs/screenshots/tablet-wide.png), [validation](docs/screenshots/empty-validation.png), and [clear-history confirmation](docs/screenshots/clear-confirmation.png).
