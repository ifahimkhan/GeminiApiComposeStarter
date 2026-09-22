# Gemini API Compose Starter / Chat Assistant

An advanced, production-ready Android application built with **Jetpack Compose**, **Material 3**, **Gemini API**, **Room Database**, and **Preferences DataStore**.

---

## Features & Architecture

- **Jetpack Compose & Material 3**:
  - Modern declarative UI with `LazyColumn` conversation rendering, stable keys, and auto-scrolling to the latest message.
  - Material 3 chat bubbles (distinct styling for user vs. Gemini responses).
  - Responsive layouts supporting phones and tablets.
  - Low-light and **Dark Mode** support with dynamic color (`isSystemInDarkTheme()`).
- **State Hoisting & ViewModel**:
  - All UI state is managed in `ChatUiState` exposed as a `StateFlow` from `ChatViewModel` and collected via `collectAsStateWithLifecycle()`.
- **Expand Input Modalities**:
  - Voice-to-text speech input using `RecognizerIntent` launched via `rememberLauncherForActivityResult`.
- **Local Persistence & Personalization**:
  - **Room Database**: Persists chat conversation history locally so past chats survive app restarts.
  - **Preferences DataStore**: Remembers user draft prompt preferences across sessions.
- **Testing**:
  - **Unit Tests**: `ChatViewModelTest` using `kotlinx-coroutines-test` and a fake repository.
  - **UI Tests**: Compose UI tests using `createComposeRule()` for `ChatScreen`.

---

## API Key Security & Best Practices

To protect sensitive API credentials, this app implements rigorous security measures:
1. **Never Hardcode Keys**: The API key is never hardcoded in source code or committed resources.
2. **`local.properties` & CI Fallback**:
   - Stored in git-ignored `local.properties`. A template is provided in [`local.properties.example`](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/local.properties.example).
   - Automatically falls back to environment variables (`System.getenv("GEMINI_API_KEY")`) for CI/CD pipeline builds.
3. **Android Keystore Encryption at Rest**:
   - Uses `ApiKeyManager` with AES-256-GCM backed by the Android Keystore (`KeyGenParameterSpec`) to encrypt and decrypt sensitive data securely at runtime.
4. **Release Obfuscation**:
   - Release builds have `isMinifyEnabled = true` and `isShrinkResources = true` enabled via R8 to prevent reverse engineering.
5. **Production Considerations**:
   - In a production environment, direct client-side API calls should be routed through a **secure backend proxy** (e.g., Cloud Functions / Ktor server) or protected using **Firebase App Check** and API key restrictions (package name & SHA-256 fingerprint).

---

## Getting Started

### Prerequisites

1. Android Studio Ladybug or newer.
2. Android SDK 36 (compileSdk / targetSdk 36, minSdk 26).
3. A valid Gemini API Key from [Google AI Studio](https://aistudio.google.com/).

### Setup API Key

1. Copy `local.properties.example` to `local.properties`.
2. Add your Gemini API key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
3. Rebuild the project.

---

## Building and Running

- **Build**: `./gradlew assembleDebug`
- **Run Unit Tests**: `./gradlew testDebugUnitTest`
- **Deploy**: Run the app from Android Studio on an emulator or physical device.
