# Gemini Compose Chat (Lab Assignment 1)

A Jetpack Compose chat app for the Gemini API, built on
[GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter).

## Features
- Material 3 chat bubbles in a `LazyColumn` (stable keys, auto-scroll)
- Single `ChatUiState` exposed as `StateFlow`, stateless composables
- Responsive layout with `WindowSizeClass` (phone / foldable / tablet, both orientations)
- Loading bubble with `CircularProgressIndicator`, error card with Retry
- System / Light / Dark theme + dynamic colour (Android 12+)
- Voice input (speech-to-text) with `RecognizerIntent`
- Preferences in Preferences DataStore, chat history in Room
- API key encrypted at rest with an Android Keystore AES-256-GCM key

## 1. Where to put the API key
1. Get a key from Google AI Studio: https://ai.google.dev/gemini-api/docs/quickstart
2. Copy `local.properties.example` to `local.properties` (it is git-ignored) and set:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
3. Sync Gradle and run. `app/build.gradle.kts` reads the value into `BuildConfig.GEMINI_API_KEY`.

**CI:** if `local.properties` has no key, Gradle falls back to the `GEMINI_API_KEY`
environment variable, so a CI pipeline can supply the key from a repository secret
instead of a file.

**Never** put the key in Kotlin files, `strings.xml` or any committed Gradle file.
Before each commit run `git status` and make sure `local.properties` is not staged.
Install the hook to enforce this automatically:
```
cp scripts/pre-commit .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit
```

## 2. How the encryption flow works
```
first launch (GeminiApp.onCreate)
  BuildConfig.GEMINI_API_KEY
        │
        ▼
  KeystoreCipher ── AES-256-GCM key generated inside AndroidKeyStore (KeyGenParameterSpec)
        │            random 12-byte IV per encryption, 128-bit auth tag
        ▼
  Base64(IV ‖ ciphertext) ──► Preferences DataStore "secure_store"   (only ciphertext stored)

sending the first message
  GeminiRepositoryImpl.getModel()
        └─► ApiKeyManager.decryptApiKey()  → plaintext exists only in memory
                └─► GenerativeModel(modelName, apiKey)  (built once and cached)
```
- The AES key never leaves the Keystore (hardware-backed on most devices), so copying the
  DataStore file off the device does not reveal the API key.
- A short SHA-256 fingerprint (not reversible) is stored next to the ciphertext so the app
  re-encrypts automatically if you change the key in `local.properties`.
- If the Keystore key is lost (e.g. restore from backup onto a new phone), the unreadable
  ciphertext is discarded and re-created.
- The decrypted key is never logged, toasted or displayed.
- Release builds use R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) so code is
  obfuscated.

### Limits – why this is not enough for production
The key is still compiled into the APK as `BuildConfig.GEMINI_API_KEY`, and at runtime it is
in memory. Anyone who decompiles the APK or hooks the process (Frida, rooted device) can
recover it. Encryption and R8 only raise the bar. A production app should:

1. **Move Gemini calls behind a backend proxy** (e.g. Cloud Run / Cloud Functions). The app
   authenticates users to *your* server; only the server holds the Gemini key, and it can
   add rate limiting, quotas and abuse detection.
2. **Or use Firebase AI Logic with Firebase App Check** (Play Integrity provider), so only
   genuine, unmodified copies of your app can call Gemini, and no key ships in the APK.
3. **Restrict the API key** in Google Cloud Console: limit it to the Generative Language API
   and to your Android package name + signing-certificate SHA-1, and set quotas/budgets.
4. Rotate keys immediately if one leaks.

## 3. Architecture
```
MainActivity ─ collectAsStateWithLifecycle() ─ ChatScreen (stateless)
      │
ChatViewModel (StateFlow<ChatUiState>)
      ├── GeminiRepository      → Gemini SDK (Dispatchers.IO)
      ├── ChatHistoryRepository → Room (chat_history.db)
      └── SettingsRepository    → Preferences DataStore (user_settings)
security/ ApiKeyManager + KeystoreCipher
```
Dependencies are wired by hand in `AppContainer` (created in `GeminiApp`).

## 4. Running the tests
Unit tests (ViewModel with fake repositories, `kotlinx-coroutines-test`):
```
./gradlew testDebugUnitTest
```
Compose UI tests (`createComposeRule`, needs a running emulator/device):
```
./gradlew connectedDebugAndroidTest
```
Or right-click `ChatViewModelTest` / `ChatScreenTest` in Android Studio → *Run*.

## 5. Performance notes
- Network calls run in `withContext(Dispatchers.IO)`; Room uses its own executor.
- `LazyColumn` items have stable `key`s and `contentType`s, so adding a message does not
  recompose existing bubbles.
- `ChatUiState` / `ChatMessage` are `@Immutable`; callbacks are method references on the
  ViewModel, so they are stable across recompositions.
- Checked with **Layout Inspector → Show Recomposition Counts** while chatting: existing
  bubbles stay at 0 recompositions when a new message arrives.
- Only the last 20 messages are sent as context to keep latency and tokens low.

## 6. Voice input on the emulator
Use an emulator image **with Google Play**, enable the host microphone
(*Extended controls → Microphone → "Virtual microphone uses host audio input"*).
If no speech recogniser is installed, the app shows an error card instead of crashing.
