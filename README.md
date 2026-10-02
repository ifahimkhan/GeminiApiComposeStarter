# Gemini API Compose Starter

## Setup

1. Open the project in Android Studio.
2. Create a `local.properties` file in the project root if it does not already exist.
3. Add your Gemini API key:

GEMINI_API_KEY=your_api_key_here

Do not commit `local.properties` or the actual API key to GitHub.

## API Key Security

The API key is read from `local.properties`, with an environment-variable fallback for CI.

At runtime, the API key is protected using Android Keystore with AES-256-GCM encryption. Only encrypted data is persisted, and the decrypted key is used in memory when creating the Gemini model.

The release build uses R8 optimization/obfuscation.

For a production application, Gemini API requests should preferably be handled through a secure backend rather than exposing a client-side API key.

## Running the App

1. Open the project in Android Studio.
2. Configure the Gemini API key in `local.properties`.
3. Sync the project with Gradle.
4. Run the app on an Android emulator or device.

## Testing

The project includes Android/JUnit testing dependencies.

Run unit tests with:

./gradlew test

Run Android instrumented tests with:

./gradlew connectedAndroidTest
