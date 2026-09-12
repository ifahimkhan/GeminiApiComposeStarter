# Gemini Chat App — C055

A Gemini-powered Android chat application built with Kotlin and Jetpack Compose. It provides a responsive chat interface with persistent history, voice input, and secure API-key handling.

## Implementation

- **Jetpack Compose + Material 3** — chat interface
- **Gemini REST API + OkHttp** — AI responses
- **StateFlow + ViewModel** — UI state management
- **Room Database** — persistent chat history
- **Preferences DataStore** — application preferences
- **Android Keystore + AES-256-GCM** — secure API-key storage
- **RecognizerIntent** — voice-to-text input
- **WindowSizeClass** — adaptive layout

Network requests are executed on `Dispatchers.IO` to keep them off the main thread.

## API Key

Add your Gemini API key to `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

`local.properties` is excluded from Git, while `local.properties.example` is provided as a configuration template.

The API key is encrypted using AES-256-GCM with a key managed by the Android Keystore. Only the encrypted value and IV are persisted.

For production, a backend proxy or Firebase AI Logic with App Check should be preferred over relying on a client-side API key.

## Testing

The project includes:
- ViewModel unit testing using a fake repository and coroutine test utilities
- Jetpack Compose UI testing using `createComposeRule()`

The Compose UI test was verified on an Android API 35 emulator.

## Build

Open the project in Android Studio, sync Gradle, and run it on an Android device or emulator.

Release builds have R8 code and resource shrinking enabled.