# Gemini Compose Chat App

A student lab Android application built with Jetpack Compose, Material 3, and the Google Generative AI SDK (Gemini API).

## Setup & Gemini API Key Configuration

1. Copy `local.properties.example` to `local.properties` at the project root:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
2. Build and run the project. `app/build.gradle.kts` automatically loads `GEMINI_API_KEY` from `local.properties` (or falls back to `System.getenv("GEMINI_API_KEY")`) and exposes it via `BuildConfig.GEMINI_API_KEY`.
3. `local.properties` is included in `.gitignore` and is never committed to version control.

## Security Architecture: EncryptedSharedPreferences

To protect the API key at rest on device:
- On application launch, `GeminiRepositoryImpl` initializes `EncryptedSharedPreferences` backed by Android Keystore using `MasterKey` (AES-256-GCM).
- The API key is stored encrypted in `EncryptedSharedPreferences`.
- The key is decrypted **only in memory** when instantiating the Gemini `GenerativeModel`. The decrypted key is never logged, toasted, or displayed anywhere in the UI.
- In `app/build.gradle.kts`, `isMinifyEnabled = true` is configured for `release` builds so R8 obfuscates release code.

### Production Security Considerations

In a real production app, storing API keys on-device (even encrypted) is vulnerable to reverse engineering. Production applications should route Gemini API calls through a secure backend proxy server or use Firebase App Check with restricted API keys to avoid embedding API secrets directly within the Android client app.

## Features & Architecture

- **UI**: Jetpack Compose LazyColumn with Material 3 chat bubbles (user & Gemini styles), auto-scrolling with `LazyListState`, and responsive width layout using `WindowSizeClass`.
- **Voice Input**: Microphone button launching Android Speech Recognizer (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) via `rememberLauncherForActivityResult`.
- **Persistence**:
  - **DataStore**: Single preference (`UserPreferences`) using `datastore-preferences`.
  - **Room Database**: Full chat message persistence across app restarts via `ChatMessageEntity`, `ChatMessageDao`, and `ChatDatabase`.

## Running Tests

### Unit Tests
Run the ViewModel unit test using Gradle:
```bash
./gradlew test
```
Or run `ChatViewModelTest` directly in Android Studio.

### Compose UI Tests
Run instrumented UI tests on a connected device or emulator:
```bash
./gradlew connectedCheck
```
Or run `ChatScreenTest` directly in Android Studio.
