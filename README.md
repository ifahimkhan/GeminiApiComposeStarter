# Gemini Chat (Jetpack Compose)

**Name:** Shirin Sharma  **Roll No:** N094

A Gemini-powered chat app built with Kotlin and Jetpack Compose, extended from the course starter project.

## Features
- Chat UI with Material 3 message bubbles in a `LazyColumn` that auto-scrolls to the newest message
- Loading indicator and Snackbar error handling
- Voice input using `RecognizerIntent`
- Adaptive layout for phones and tablets using `WindowSizeClass`
- Dark mode switch, saved with DataStore
- Chat history saved with Room, so conversations survive closing the app
- API key kept out of source control and encrypted at rest with Android Keystore

## Setup
1. Get an API key from [Google AI Studio](https://aistudio.google.com).
2. Copy `local.properties.example` to `local.properties` in the project root and put your key in it:
```
   GEMINI_API_KEY=your_api_key_here
```
`local.properties` is git-ignored, so the key is never committed. If the file has no key, the build falls back to a `GEMINI_API_KEY` environment variable.
3. Open the project in Android Studio, sync Gradle, and run the `app` configuration.

## How the API key is protected
- The key is read at build time from `local.properties` (or the environment) and is never written in source files.
- On first launch the key is encrypted with **AES-256-GCM**. The encryption key lives in the **Android Keystore** and never leaves it. Only the ciphertext is stored, in DataStore.
- Release builds use R8 minification and resource shrinking.

### Limitations
Client-side protection has limits. The key is still compiled into `BuildConfig`, so someone who unpacks the APK can extract it, and obfuscation only slows that down. A production app should keep the key on a backend proxy or use Firebase App Check, so the client never holds the real key.

## Tests
- **Unit tests** (ViewModel, using fake repositories): run the `testDebugUnitTest` Gradle task, or right-click `ChatViewModelTest`.
- **Compose UI tests** (need an emulator or device): run `connectedDebugAndroidTest`, or run `ChatScreenTest`.

## Tech
Kotlin, Jetpack Compose, Material 3, ViewModel and StateFlow, Room (KSP), DataStore, Android Keystore, Gemini generative AI SDK.