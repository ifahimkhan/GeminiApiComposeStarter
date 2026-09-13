# Gemini API Compose Starter - C057

Name: Dhruv Arragunta  
Roll No: C057

## About the Project

This project is based on the Gemini Jetpack Compose starter app.

I added a better chat UI, secure API key handling, voice input, saved chat history, user preferences, responsive layout and testing.

## Features Added

- Chat bubbles for user and Gemini
- LazyColumn for chat messages
- Auto scroll to latest message
- Loading indicator while Gemini is responding
- Snackbar for errors
- Voice input using speech to text
- Room database for saving chat history
- Preferences DataStore for saving the concise reply setting
- Responsive layout using WindowSizeClass
- Unit tests for ChatViewModel
- Compose UI tests
- Dark theme support through Material 3 theme

## Gemini API Key Setup

Create a Gemini API key from Google AI Studio.

Add the key in:

```text
local.properties
```

Example:

```properties
GEMINI_API_KEY=your_api_key_here
```

Do not upload or commit `local.properties`.

A sample file is included:

```text
local.properties.example
```

The project also supports the `GEMINI_API_KEY` environment variable for CI builds.

## API Key Security

The Gemini API key is not hardcoded in Kotlin or XML files.

The key is read from `local.properties` or an environment variable.

On the device, the key is encrypted using AES-256-GCM.

The encryption key is generated and stored using Android Keystore.

Only the encrypted API key and IV are saved in DataStore.

The decrypted key is only used when the Gemini model needs to be created.

Release builds also use:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

This enables R8 obfuscation and resource shrinking.

## Important Note About API Key Security

Storing and encrypting an API key inside an Android app improves security, but it cannot completely hide the key from a determined attacker.

For a production app, Gemini API calls should ideally go through a backend server. Restricted API keys and Firebase App Check can also be used.

## Chat History

Room database is used to save chat messages.

Because of this, previous chat messages are still visible after closing and reopening the app.

## User Preferences

Preferences DataStore is used to save the `Concise replies` setting.

If concise replies are enabled, Gemini is asked to give shorter answers.

The setting is remembered after restarting the app.

## Voice Input

Voice input is implemented using:

```text
RecognizerIntent
rememberLauncherForActivityResult
```

The microphone button opens Android speech recognition and places the detected speech into the prompt field.

## Responsive Layout

WindowSizeClass is used to handle different screen sizes.

The chat layout works on phone, landscape mode and larger displays.

## Testing

### ChatViewModel Unit Tests

The ViewModel tests use:

```text
JUnit
kotlinx-coroutines-test
Fake GeminiRepository
Fake ChatDao
Fake UserPreferences
```

Tests include:

```text
Sending a message
Empty prompt validation
Repository error handling
```

Result:

```text
3 tests passed
```

### Compose UI Tests

Compose UI tests are written using:

```text
createComposeRule()
```

The tests check:

```text
Empty chat screen
User and Gemini messages
Loading state
```

Result:

```text
3 tests passed
```

## How to Run

1. Clone the repository.
2. Open it in Android Studio.
3. Add the Gemini API key to `local.properties`.
4. Sync Gradle.
5. Start an emulator.
6. Run the app.

## Screenshots

Screenshots of the working app are included in the `screenshots` folder.

## Branch

Assignment work was completed on:

```text
C057-gemini-assignment
```
## Screenshots

Screenshots of the working application and test results are available in the `screenshots` folder.