# Gemini Chat (Jetpack Compose) — Manvi Segan, N146

An Android chat app built on the Gemini Jetpack Compose starter. You can type or speak a question and get replies from Google Gemini. Chats are saved on the device, and the API key is encrypted at rest.

## Features

| Area | What was implemented |
|---|---|
| Chat UI | Conversation shown as a `LazyColumn` of Material 3 chat bubbles (user on the right, Gemini on the left) with stable keys and auto-scroll to the latest message |
| State | All UI state in `ChatUiState`, exposed as a `StateFlow` from `ChatViewModel` and collected with `collectAsStateWithLifecycle()`; composables are stateless |
| Responsive layout | `WindowSizeClass`: on tablets / landscape the chat is centred with a max width and wider bubbles |
| Loading & errors | Loading bubble with `CircularProgressIndicator` while waiting; `Snackbar` when the API call fails; "Field cannot be empty" validation |
| Dark mode | Dark mode switch; Material 3 dynamic colour in `Theme.kt`; the choice is saved |
| Voice input | Mic button launches `RecognizerIntent` via `rememberLauncherForActivityResult`; spoken text goes into the input box |
| Chat history | Messages saved with **Room**, so they survive app restarts; 🗑️ button clears the chat |
| Preferences | Dark mode preference saved with **Preferences DataStore** |
| Security | API key read from `local.properties` / env variable, encrypted with an **Android Keystore AES-256-GCM** key, stored only as ciphertext, R8 enabled for release |
| Tests | ViewModel unit tests with `kotlinx-coroutines-test` and fakes; Compose UI tests with `createComposeRule()` |

## 1. Where to put the API key

1. Get a free key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Copy `local.properties.example` and add the line to your own `local.properties` (in the project root):
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
3. Sync Gradle and run the app.

- `local.properties` is listed in `.gitignore` and is **never committed**.
- The key is never written in any Kotlin file, `strings.xml` or `build.gradle.kts`.
- **CI:** if `local.properties` has no key, `app/build.gradle.kts` falls back to the `GEMINI_API_KEY` environment variable, so a CI build can use a repository secret:
  ```kotlin
  val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")?.trim()
      ?: System.getenv("GEMINI_API_KEY") ?: ""
  ```

## 2. How the encryption works

Files: `security/KeystoreCipher.kt`, `security/SecureApiKeyStore.kt`, `data/GeminiRepositoryImpl.kt`, `MainActivity.kt`

1. **First launch:** `KeystoreCipher` creates an **AES-256 key inside the Android Keystore** using `KeyGenParameterSpec` (GCM mode, no padding). The AES key never leaves the Keystore.
2. The Gemini API key is encrypted with **AES/GCM/NoPadding**. The 12-byte IV and the ciphertext are Base64-encoded together.
3. **Only the ciphertext** is saved in Preferences DataStore (`encrypted_gemini_api_key`). The plain key is never written to disk.
4. **Decrypt only when needed:** `GeminiRepositoryImpl` gets the key through `apiKeyProvider`, which decrypts it **in memory only at the moment the `GenerativeModel` is created**.
5. The decrypted key is **never logged, toasted or displayed**.
6. **Release build:** `isMinifyEnabled = true` and `isShrinkResources = true`, so R8 shrinks and obfuscates the APK and the code is harder to read after decompiling.

```
First launch:   BuildConfig key → AES-256-GCM (Keystore key) → ciphertext → DataStore
When sending:   DataStore ciphertext → decrypt in memory → GenerativeModel(apiKey) → Gemini
```

### Limits of client-side protection

Encrypting the key on the device **raises the bar but cannot fully hide it**. The key is still built into the APK, and a determined attacker with a rooted device or a debugger can read it from memory while the app runs. A production app should instead:

- **Use a backend proxy:** the app calls your own server (for example Cloud Functions or Cloud Run), and only the server holds the Gemini key and calls the Gemini API. The key never ships in the app, and the server can add authentication, rate limits and logging.
- **Use Firebase App Check with Firebase AI Logic (Vertex AI in Firebase):** App Check (Play Integrity on Android) proves that requests come from your real, unmodified app, and Firebase calls Gemini for you, so no API key is in the app.
- **Restrict the API key** in Google Cloud Console: allow only the Generative Language API, restrict it to your app's package name and SHA-1 signing certificate, set quotas and budget alerts, and rotate the key if it leaks.

## 3. How to run the tests

**Unit tests** (no emulator needed): `app/src/test/.../ChatViewModelTest.kt`
- Tests `ChatViewModel` with a **fake `GeminiRepository`**, a fake chat history and fake preferences, using `kotlinx-coroutines-test` (`Dispatchers.setMain`, `runTest`).
- Covers: empty prompt error, missing API key, successful reply, API failure, clearing errors, clearing chat, and saving dark mode.
```bash
./gradlew testDebugUnitTest
```

**Compose UI tests** (emulator or device must be running): `app/src/androidTest/.../ChatScreenTest.kt`
- Uses `createComposeRule()` to test the stateless `ChatScreen`.
- Covers: title and placeholder, message bubbles, typing, the Send button, and the empty-field error.
```bash
./gradlew connectedDebugAndroidTest
```

You can also click the green ▶ next to a test class in Android Studio and choose **Run**.

## 4. Performance

- The Layout Inspector's recomposition counts were used to check that typing in the input box does not recompose the chat bubbles.
- `LazyColumn` uses stable `key`s so existing bubbles are not recomposed when a new message arrives.
- Network calls (Gemini SDK) and database/Keystore work (Room, DataStore, `withContext(Dispatchers.IO)`) run off the main thread in coroutines.

## Project structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt              # WindowSizeClass, theme, dependencies, first-launch key encryption
├── data/
│   ├── GeminiRepository.kt      # interface (faked in tests)
│   ├── GeminiRepositoryImpl.kt  # creates GenerativeModel with the decrypted key
│   ├── ChatHistoryRepository.kt # Room-backed chat history
│   ├── UserPreferencesRepository.kt # DataStore preferences (dark mode)
│   └── local/                   # Room: entity, DAO, database
├── security/
│   ├── KeystoreCipher.kt        # Android Keystore AES-256-GCM
│   └── SecureApiKeyStore.kt     # stores only the ciphertext in DataStore
└── ui/chat/                     # ChatScreen, ChatViewModel, ChatUiState
```