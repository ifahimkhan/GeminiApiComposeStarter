# Mobile Application Development - Assignment 1

A professional, secure, and responsive AI Chat application built with Jetpack Compose, Gemini AI, and robust local persistence.

## Features

- **Gemini AI Integration**: Seamlessly chat with Google's state-of-the-art `gemini-3.6-flash` model.
- **Persistent Chat History**: All conversations are stored locally using **Room Database**.
- **User Preferences**: Dark Mode preference is persisted using **Preferences DataStore**.
- **Secure API Key Management**: API keys are protected using the **Android Keystore System** and **AES-256-GCM** encryption.
- **Voice Input**: Integrated speech-to-text functionality using `RecognizerIntent`.
- **Responsive UI**: Adaptive layout supporting phones (Portrait/Landscape) and tablets using `WindowSizeClass`.
- **Material 3 Design**: Modern UI following Material Design 3 principles with dynamic color support.

## Project Architecture

The app follows modern Android development practices:
- **UI**: Jetpack Compose (Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **Data Persistence**: Room (SQL) & DataStore (Key-Value)
- **Networking**: Kotlin Coroutines & Flow for asynchronous operations

## Security

### API Key Protection
We follow a multi-layered security approach for the Gemini API key:
1. **Local Configuration**: The key is stored in `local.properties` (never committed to Git).
2. **Build-Time Injection**: `BuildConfig` is used to inject the key into the app.
3. **At-Rest Encryption**:
   - On the first run, the app generates a unique **AES-256** key inside the **Android Keystore**.
   - The API key is encrypted using **GCM (Galois/Counter Mode)** with NoPadding.
   - Only the ciphertext and IV (Initialization Vector) are stored in DataStore.
4. **In-Memory Only**: The key is decrypted only when needed for model initialization and never logged or displayed in the UI.

> [!IMPORTANT]
> **Security Limitation**: While client-side encryption provides a significant hurdle, a determined attacker with root access can theoretically extract keys. For production, consider using a backend proxy or Firebase App Check.

## Setup Instructions

1. **Obtain API Key**: Get your Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. **Configure Key**:
   - Create a file named `local.properties` in the root directory (if not present).
   - Add the following line: `GEMINI_API_KEY=your_actual_key_here`
3. **Environment Variable (Optional)**: Alternatively, set a system environment variable `GEMINI_API_KEY`.
4. **Build**: Run the project in Android Studio.

## Testing

- **Unit Tests**: Run `./gradlew test` to verify ViewModel logic and Repository behaviors using `kotlinx-coroutines-test` and Mockito.
- **UI Tests**: Run `./gradlew connectedAndroidTest` to verify the Compose UI components and user flows.

## Requirements Checklist Compliance

- [x] Gemini API key from `local.properties`
- [x] `BuildConfig.GEMINI_API_KEY` with environment fallback
- [x] Android Keystore with AES-256-GCM encryption
- [x] Room Database for chat history
- [x] Preferences DataStore for User Preferences (Dark Mode)
- [x] LazyColumn with stable keys
- [x] State Hoisting (Stateless Composables)
- [x] Responsive UI with `WindowSizeClass`
- [x] Voice Input using `RecognizerIntent`
- [x] Unit & UI Test Coverage
- [x] Release build configuration with R8 enabled
