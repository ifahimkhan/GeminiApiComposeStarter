# Judy AI

A native Android Gemini chatbot built on the Assignment 1 Compose starter. The supplied f6c video guides the dark home, voice and chat screens. Kotlin, Jetpack Compose, Material 3, ChatViewModel, ChatUiState and the GeminiRepository abstraction remain the application foundation.

Local branch: `C016-assignment-1-chatbot`. Upstream baseline: `8d01c6862630a509e379d395220598e6f640e9ef`. Work is local; nothing has been pushed or submitted.

## Run locally

Open this `android-app` folder in Android Studio, let Gradle sync, select an emulator or Android device and run `app`. Minimum Android version is API 26. The project compiles and targets API 36.

Copy `local.properties.example` to `local.properties` only if you do not already have a local configuration. Set your Android SDK path and your own Gemini API key there. Do not put the key into source code or paste it into a message. The local configuration already created on this machine has an empty API key.

```properties
sdk.dir=/path/to/Android/sdk
GEMINI_API_KEY=your_api_key_here
GEMINI_MODEL=gemini-3.6-flash
GEMINI_KEY_VERSION=1
```

The model defaults to the starter's model. Change `GEMINI_MODEL` if your key/project needs a different supported Gemini model. Increment `GEMINI_KEY_VERSION` when replacing the credential so the encrypted runtime copy is refreshed.

Use the Android Studio Gradle JDK (JDK 17 or newer; local validation uses Studio's JDK 21). For a terminal on this Mac:

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.fahim.geminiApiComposeStarter/.MainActivity
```

Without a key, navigation, settings, history and speech input remain available. Sending displays a configuration error and keeps the failed turn retryable. No fake AI responses are shipped in the application. Add a valid key and rebuild to verify real replies; do not treat the test fixture answers as a live Gemini test.

## What is implemented

- Home greeting, research/health cards, recent history and bottom navigation.
- Chat bubbles in a stable-key LazyColumn, bottom composer, scrolling, progress, error Snackbar and retry.
- Native drawn animated particle orb, hexagonal microphone and reference-inspired glass surfaces.
- Speech input through `RecognizerIntent` and `rememberLauncherForActivityResult`; returned text is reviewed before sending.
- Separate conversations in Room, ordered messages, local history search and local answer feedback.
- Preferences DataStore for name, preferred assistant and reduced motion.
- Copy/share, latest-turn regeneration and new conversations.
- StateFlow collected with `collectAsStateWithLifecycle`, stateless screens and SavedStateHandle for draft/route recovery.
- WindowSizeClass-based expanded layout with a history panel, keyboard insets and scrollable compact-height voice content.
- AES-256-GCM credential storage using Android Keystore; release shrinking/obfuscation with R8.

The Home sample-history rows in the visual screenshots are test fixtures. Fresh installs begin with empty history. Category cards suggest prompts; they do not upload papers or perform specialized medical diagnosis.

## Architecture

`MainActivity` obtains an application-owned Room database, repositories and a ViewModel factory. `ChatRoute` collects state and owns Android effects such as speech recognition, sharing and snackbars. `ChatScreen` and its component screens receive immutable state and callbacks.

`ChatViewModel` validates sends, coordinates repository work, observes persistence, prevents concurrent sends and routes returned answers to their original conversation. `GeminiRepositoryImpl` retains the starter's `generateText(prompt): Result<String>` contract. A bounded request builder includes successful prior turn pairs, without including failed turns or unrelated conversations. These text role labels are formatting, not a security boundary.

`ConversationRepository` persists user turns before requesting Gemini, then stores the assistant answer transactionally. Retry reuses a turn ID; regeneration replaces the associated answer. An interrupted request is marked retryable after restart and is never automatically resubmitted. The full local history remains available even when old turn pairs are omitted from model context.

The current integration returns completed answers; server streaming and continuous voice conversation are not implemented. The voice orb is decorative, not a live microphone amplitude meter.

## Credential encryption and limits

1. Gradle uses a nonblank `GEMINI_API_KEY` from ignored `local.properties`, then the environment, then an empty value. It emits the value into generated BuildConfig with escaped string handling.
2. On first launch with a configured key, `ApiKeyVault` creates an AES-256 key through Android Keystore and `KeyGenParameterSpec`.
3. AES/GCM encrypts the API credential with a fresh random IV. A separate DataStore contains only its authenticated ciphertext, IV and credential version. The AES key stays in Keystore.
4. The repository requests decryption only while creating `GenerativeModel`. Temporary byte buffers are cleared; Java strings and SDK internals cannot be guaranteed to be zeroized.
5. Invalid ciphertext/device keys are regenerated from the configured bootstrap value. A missing key produces a controlled error. Incrementing the credential version rotates the encrypted copy.
6. Backup/device-transfer exclusions prevent copying device-bound credentials to another device. This build also disables backup for conversation data and preferences.

**Client-side limitation:** the bootstrap value still exists in the APK's generated BuildConfig. Encrypting its runtime copy does not remove it from the binary. R8 makes reverse engineering harder but does not make an API key secret. Never claim that a distributed APK has an unrecoverable credential.

A production deployment should move the Gemini call behind an authenticated backend proxy with quotas/rate limits and server-managed secrets. An alternative requires a properly supported Firebase integration with App Check and restricted API keys; App Check is not automatically provided by this direct client SDK. Both would be separate deployment work from the assignment's prescribed integration.

Never log raw SDK errors or request URLs containing credentials. `GeminiRepositoryImpl` maps failures to safe messages. Do not commit `local.properties`, keystores, generated BuildConfig, APKs with real credentials, or screenshots of key configuration.

For CI, inject `GEMINI_API_KEY` as a repository secret only for a deliberately authorized live smoke test. Deterministic tests require no real secret, including for fork PRs.

## Tests

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug assembleRelease
```

Unit tests use `kotlinx-coroutines-test` and a fake GeminiRepository. Compose tests use `createComposeRule()`. On-device tests cover Room reopen/order/retry behavior, DataStore persistence, Keystore round trips, IV rotation, tamper recovery and missing credentials. Screenshot tests render deterministic data; they are not stored in the production database.

Reports are under `app/build/reports/`. The release APK is unsigned; use your own signing configuration for distribution. Local smoke testing may use a debug-signed copy of the minified APK, clearly identified as a test build.

Run `python3 scripts/check_secrets.py` before committing. It reports counts and filenames, never matched secret text. Also inspect `git status` and confirm that `local.properties` is ignored and unstaged. This focused scanner is supplementary and does not guarantee detection of every secret format.

## Visual reference and remaining verification

See [local verification](evidence/VERIFICATION.md) and [screenshots](evidence/screenshots/). The screenshots identify fixture data separately from real local runs.

The app reproduces the reference's layout vocabulary, colours, cards, docks and animation in Compose. It is not certified pixel-identical. The original avatar and font assets were not supplied; the avatar is a local profile icon, some icons are native approximations, and the orb is reconstructed procedural artwork. Android owns its system bars and the speech-provider activity. Voice capture therefore temporarily uses the provider's interface rather than remaining entirely inside the custom orb screen.

The fixed dark theme is intentional for reference fidelity. The assignment's optional dynamic colour and system-following light mode are not enabled. No subscriptions, payments, uploads, cloud sync or notification backend are implied by the reference controls.

Before academic submission, complete the remaining live-device/API checks in the verification record, inspect recomposition counts in Android Studio Layout Inspector, add the relevant screenshots and create the assignment PR from the C016 branch. The user's current instruction is local testing only.

## Starter repairs

- Restored the missing Gradle wrapper JAR and executable wrapper permission.
- Aligned the Compose compiler plugin to Kotlin 2.2.10 for AGP 9's built-in Kotlin.
- Used KSP 2.3.12 for AGP 9 compatibility and Room schema generation.
- Updated Compose BOM to 2025.09.01 to keep Compose UI/foundation components aligned with dependencies, after a layout crash surfaced in emulator testing.
- Replaced a starter reference to the missing `ic_assistant` drawable with native Compose components.

These changes repair dependencies and extend the existing Android stack; they do not substitute a web or cross-platform implementation.


## Second UI pass

Tap the AI Host pill at the top of a chat (or **Settings → Your model**) to choose a Gemini model. The selection is saved locally and applies to the next message, retry or regeneration. An in-flight reply keeps its original model; changing selection preserves your conversation and draft. The curated list includes preview labeling and retains `GEMINI_MODEL` as the default. Model access and quota depend on the configured account; no automatic fallback is performed. The catalog was checked against Google's models endpoint on 25 September 2026 ([model documentation](https://ai.google.dev/gemini-api/docs/models)).

The second visual pass adds short screen/message/transcript transitions, press feedback and a cached particle-orb cycle. **Reduce motion** pauses the orb and suppresses these app-owned effects. The waveform is decorative; speech is still handled by RecognizerIntent. See `evidence/PASS_2.md` for this pass's checks and screenshots.
