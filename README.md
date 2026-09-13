# Gemini API Chat - Android Implementation

This project is an enhanced version of the Gemini Jetpack Compose starter app, implemented for Assignment 1 of the Mobile Application Development subject.

## Features
- **Material 3 UI**: Modern chat interface with distinct bubbles for User and Gemini.
- **Security**: 
    - API key encryption at rest using Android Keystore and `EncryptedSharedPreferences`.
    - API key provided via `local.properties` (git-ignored) or environment variables.
    - R8 obfuscation enabled for release builds.
- **Persistence**: Chat history is saved locally using Room Database.
- **Adaptive Layout**: UI adapts to different screen sizes (Compact, Medium, Expanded).
- **Voice Input**: Speech-to-text integration for sending prompts.
- **Loading & Error Handling**: Real-time feedback using progress indicators and Snackbars.

## Getting Started

### 1. API Key Setup
1. Obtain a Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. Create a `local.properties` file in the project root if it doesn't exist.
3. Add your key: `GEMINI_API_KEY=your_actual_key_here`.

### 2. Encryption Flow
- The API key is read from `BuildConfig.GEMINI_API_KEY` at runtime.
- On first launch, the `SecurityManager` encrypts the key using a `MasterKey` from the Android Keystore and stores it in `EncryptedSharedPreferences`.
- For subsequent uses, the key is decrypted only in memory when the `GenerativeModel` is initialized.

### 3. Running Tests
- **Unit Tests**: Run `./gradlew testDebugUnitTest` or use the IDE test runner on `ChatViewModelTest`.
- **Instrumentation Tests**: Run `./gradlew connectedAndroidTest` (requires an emulator/device) or use the IDE test runner on `ChatScreenTest`.

## Submission Details
- **Roll Number**: C022
- **Branch**: `C022-assignment-1-submission`
