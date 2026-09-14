# Gemini API Compose Starter

## Mobile Application Development - Assignment 1

A Jetpack Compose Android application that uses the Gemini API to generate responses in a chat interface.

## Features

- Gemini API integration
- Jetpack Compose Material 3 UI
- Chat-style conversation interface
- Separate user and Gemini message bubbles
- LazyColumn for displaying messages
- Stable keys for chat messages
- Automatic scrolling to the latest message
- Loading indicator while Gemini is responding
- Error handling using Snackbar
- Voice input using Android Speech Recognition
- Conversation history using Room
- Persistent API key storage using Android Keystore and AES-256-GCM
- Preferences DataStore support
- R8 minification for release builds
- Responsive/adaptive UI
- Unit tests for the ViewModel
- Compose UI tests

## API Key Setup

The Gemini API key must not be hardcoded in Kotlin source files, strings.xml, or Gradle build files.

Create a `local.properties` file in the project root and add:

    GEMINI_API_KEY=your_actual_api_key

The `local.properties` file is ignored by Git and must never be committed.

A `local.properties.example` file is provided as a template:

    GEMINI_API_KEY=your_api_key_here

## API Key Security

The API key is initially provided through `BuildConfig.GEMINI_API_KEY`.

The application uses Android Keystore with an AES-256-GCM key to encrypt the API key before storing it in Preferences DataStore.

The encryption key itself is stored in the Android Keystore and is not stored in the application data.

The encrypted API key and initialization vector are persisted in DataStore.

The API key is decrypted in memory only when the Gemini `GenerativeModel` is created.

The decrypted API key is not logged, displayed, or stored as plaintext in DataStore.

## Security Limitations

Client-side encryption does not make an API key completely inaccessible to a determined attacker because the application must eventually use the key.

For a production application, a backend proxy should be considered so that the API key is kept on a server rather than distributed inside the application.

Additional production protections such as Firebase App Check and appropriately restricted API keys should also be considered.

## Conversation History

Chat messages are stored locally using Room.

The stored data includes:

- Message ID
- Message text
- Whether the message was sent by the user or Gemini
- Message timestamp

Conversation history remains available after restarting the application.

## Voice Input

Voice input uses Android's speech recognition functionality.

The application requests microphone permission at runtime and converts recognized speech into the prompt field.

## Build Configuration

The release build uses R8 code shrinking and minification.

The release build can be generated using Android Studio's APK generation options.

## Testing

The project includes dependencies for:

- JUnit
- Kotlin Coroutines Test
- AndroidX JUnit
- Espresso
- Jetpack Compose UI testing

Unit tests are used to verify ViewModel behavior with a fake Gemini repository.

Compose UI tests verify important UI behavior.

## Architecture

The application follows a simple separation of responsibilities:

- `MainActivity` hosts the Compose UI and creates the ViewModel.
- `ChatScreen` displays the chat interface.
- `ChatViewModel` manages UI state and user actions.
- `GeminiRepository` provides an abstraction for Gemini API calls.
- `GeminiRepositoryImpl` communicates with Gemini.
- `AppDatabase` provides the Room database.
- `ChatMessageDao` handles chat message persistence.
- `SecureApiKeyManager` handles API key encryption and decryption.

## Important

Never commit:

- `local.properties`
- Real Gemini API keys
- Other secrets or credentials

Before submitting the project, verify that no API keys or secrets appear in the Git history or pull request.