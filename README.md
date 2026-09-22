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

## Getting Started

### Prerequisites

1. Android Studio Ladybug or newer.
2. Android SDK 36 (compileSdk / targetSdk 36, minSdk 26).
3. A valid Gemini API Key from [Google AI Studio](https://aistudio.google.com/).

### Setup API Key

To keep your API key secure and out of version control:
1. Open or create `local.properties` in the root project directory.
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

---

## Tech Stack

- **UI**: Jetpack Compose, Material 3, Navigation / Lifecycle Compose
- **AI**: Google Generative AI SDK (`google-generativeai`)
- **Persistence**: Room Database, Preferences DataStore
- **Concurrency**: Kotlin Coroutines & Flow
- **Testing**: JUnit, Kotlinx Coroutines Test, Compose UI Test
