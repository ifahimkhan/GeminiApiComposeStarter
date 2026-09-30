# Gemini Chat – Jetpack Compose (MAD Lab Assignment 1)

A Gemini-powered chat app built on the GeminiApiComposeStarter, with a Material 3 chat UI,
voice input, persistent chat history and an encrypted API key.

## Features
- Chat bubbles (user vs Gemini) in a `LazyColumn` with stable keys and auto-scroll
- All UI state in `ChatUiState`, exposed as a `StateFlow` from `ChatViewModel`, collected with `collectAsStateWithLifecycle()`; composables are stateless
- Responsive layout with `WindowSizeClass` (phone / tablet / landscape)
- Loading bubble with `CircularProgressIndicator`; errors shown in a `Snackbar`
- Light / Dark / System theme toggle (saved in Preferences DataStore) + Material 3 dynamic colour
- Voice input via `RecognizerIntent` + `rememberLauncherForActivityResult`
- Chat history stored in Room, survives app restarts; "Clear chat" button
- API key encrypted at rest with an AES-256-GCM Android Keystore key
- R8 minify + obfuscation on release builds

## 1. Where to put the API key
1. Get a key from Google AI Studio: https://aistudio.google.com/app/apikey
2. Copy `local.properties.example` to `local.properties` in the project root (the file Android Studio already created also works – just add the line).
3. Add:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
4. `local.properties` is git-ignored. Never put the key in Kotlin, XML or Gradle files.

**CI:** if `local.properties` has no key, `app/build.gradle.kts` falls back to the
`GEMINI_API_KEY` environment variable, so CI can use a repository secret.

## 2. How the encryption flow works
1. At build time Gradle reads the key and exposes it as `BuildConfig.GEMINI_API_KEY`.
2. On first launch `SecureApiKeyStore.ensureEncrypted()` runs:
   - `KeystoreCrypto` generates an **AES-256-GCM** key inside the **Android Keystore**
     (`KeyGenParameterSpec`). The key material never leaves the Keystore.
   - The API key is encrypted with it; only the **ciphertext + IV** (Base64) are saved in
     Preferences DataStore (`secure_store`).
3. When the first request is sent, `GeminiRepositoryImpl` calls the key provider, which
   decrypts the key **in memory only** and immediately builds the `GenerativeModel`.
4. The decrypted value is never logged, toasted or displayed.
5. Release builds use `isMinifyEnabled = true`, so R8 obfuscates the code.

### Limits (important)
Client-side encryption raises the bar but cannot fully hide a key: the key is still
compiled into `BuildConfig`, and anyone with a rooted device or a debugger can read it
from memory. A production app should:
- **Move Gemini calls behind a backend proxy** (e.g. Cloud Functions / Cloud Run). The app
  calls your server, the server holds the key and calls Gemini. The key never ships in the APK.
- Or use **Firebase AI Logic with Firebase App Check**, so only genuine copies of your app
  (verified by Play Integrity) can call the API.
- **Restrict the API key** in Google Cloud Console (limit it to the Generative Language API
  and to your app's package name + SHA-1) and set quotas.

## 3. How to run the tests
- **Unit tests** (ViewModel, fake repositories, `kotlinx-coroutines-test`):
  ```
  ./gradlew testDebugUnitTest
  ```
  or right-click `app/src/test` → *Run 'Tests in test'*.
- **Compose UI tests** (`createComposeRule()`, needs an emulator or device running):
  ```
  ./gradlew connectedDebugAndroidTest
  ```
  or right-click `app/src/androidTest` → *Run 'Tests in androidTest'*.

## Project structure
```
MainActivity.kt            – sets content, WindowSizeClass, theme
AppContainer.kt            – GeminiApp (Application) + manual DI container
security/KeystoreCrypto.kt – AES-256-GCM with Android Keystore
security/SecureApiKeyStore.kt – stores ciphertext in DataStore, decrypts in memory
data/GeminiRepository*.kt  – Gemini API calls (off main thread)
data/ChatHistoryRepository.kt – Room-backed chat history
data/UserPreferencesRepository.kt – theme preference (DataStore)
data/local/ChatDatabase.kt – Room entity, DAO, database
ui/chat/ChatScreen.kt      – stateless composables (bubbles, prompt bar, voice)
ui/chat/ChatViewModel.kt   – state + events
ui/chat/ChatUiState.kt     – UI state
```
