# Gemini API Compose Starter - MAD Assignment 1

## Features

- Gemini API chat integration
- Secure API key configuration using `local.properties`
- AES-256-GCM encryption using Android Keystore
- Room database for persistent chat history
- Preferences DataStore for user preferences
- Jetpack Compose Material 3 chat UI
- LazyColumn with stable message keys
- Automatic scrolling to latest message
- Loading and error states
- Voice input using Android Speech Recognition
- Dark mode and dynamic colors
- Responsive layout using WindowSizeClass
- R8 enabled for release builds
- ViewModel unit tests using kotlinx-coroutines-test
- Compose UI test using createComposeRule()

## API Key Setup

Create `local.properties` in the project root:

GEMINI_API_KEY=YOUR_GEMINI_API_KEY

Never commit the real API key or `local.properties`.

A template is provided in `local.properties.example`.

## Security

The API key is protected using Android Keystore with AES-256-GCM encryption.

The encrypted value is stored locally, while the decrypted API key exists only in memory when creating the Gemini model.

The API key is never logged or displayed.

For production, API requests should preferably go through a secure backend proxy. Firebase App Check and restricted API keys can also provide additional protection.

## Running Tests

Run the ViewModel tests with:

./gradlew test

The project also contains a Compose UI test under:

app/src/androidTest/

The Compose UI test requires an Android emulator or physical Android device.

## Build

The project can be built using Android Studio or Gradle.

Release builds use R8 minification.