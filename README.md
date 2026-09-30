# Gemini Compose App

A modern Android chat application built with Kotlin, Jetpack Compose, Material 3, and the Google Gemini API.

This project extends the Gemini Compose starter application with secure API-key handling, persistent chat history, voice input, responsive layouts, theme preferences, automated testing, and performance optimizations.

---

## Features

- Gemini-powered text chat
- Material 3 user and Gemini chat bubbles
- LazyColumn conversation history with stable keys
- Automatic scrolling to the latest message
- Loading indicator while waiting for a Gemini response
- Snackbar error handling
- Voice-to-text input using RecognizerIntent
- Responsive layouts using WindowSizeClass
- Light, dark, and system theme preferences
- Preferences DataStore for saved user preferences
- Room database for persistent chat history
- AES-256-GCM API-key encryption using Android Keystore
- Environment-variable fallback for CI builds
- R8 minification and resource shrinking for release builds
- ChatViewModel unit tests
- Jetpack Compose UI tests
- Performance inspection using Android Studio Layout Inspector

---

# API Key Setup

The Gemini API key must not be hardcoded inside Kotlin files, XML resources, or committed Gradle files.

## 1. Obtain a Gemini API Key

Create a Gemini API key using Google AI Studio.

## 2. Add the API Key to local.properties

Open the root-level file:

local.properties

Add the following line:

GEMINI_API_KEY=your_api_key_here

Replace `your_api_key_here` with your actual Gemini API key.

Example:

GEMINI_API_KEY=YOUR_REAL_GEMINI_API_KEY

The project also contains:

local.properties.example

This file contains only the placeholder:

GEMINI_API_KEY=your_api_key_here

It exists so that anyone cloning the project knows what configuration is required.

## Important

The real `local.properties` file must never be committed to Git.

It is excluded using `.gitignore`.

You can verify that Git is ignoring it with:

git check-ignore -v local.properties

You can also verify that Git is not tracking it with:

git ls-files local.properties

The second command should return no output.

---

# CI / Environment Variable Support

The Gradle configuration first tries to read the Gemini API key from:

local.properties

If the key is not available there, it falls back to the environment variable:

GEMINI_API_KEY

The Gradle logic follows this pattern:

val geminiApiKey =
localProperties.getProperty("GEMINI_API_KEY")
?: System.getenv("GEMINI_API_KEY")
?: ""

This allows CI systems to provide the API key through repository secrets instead of requiring a local.properties file.

---

# API Key Encryption

The application encrypts the Gemini API key at rest using AES-256-GCM encryption backed by the Android Keystore.

The security-related classes are located under:

app/src/main/java/com/fahim/geminiApiComposeStarter/security/

The main security files are:

ApiKeyCryptoManager.kt
ApiKeyStorage.kt
SecureApiKeyProvider.kt

---

## Encryption Flow

The encryption process works as follows:

1. The API key is read at build time from `local.properties` or from the `GEMINI_API_KEY` environment variable.

2. Gradle exposes the value to the application through:

   BuildConfig.GEMINI_API_KEY

3. When the application first needs the Gemini API key, `SecureApiKeyProvider` checks whether an encrypted key has already been stored.

4. If no encrypted key exists yet, `ApiKeyCryptoManager` generates an AES-256-GCM encryption key inside the Android Keystore.

5. The Android Keystore key is created using `KeyGenParameterSpec`.

6. The Gemini API key is encrypted using AES in GCM mode.

7. Only the encrypted API-key data is persisted using DataStore.

8. The plaintext API key is not persisted by the application.

9. On later launches, the application retrieves the encrypted API-key data from DataStore.

10. The encrypted value is decrypted in memory only when the Gemini `GenerativeModel` needs to be created.

11. The decrypted API key is passed directly to `GenerativeModel`.

12. The decrypted API key is never intentionally:
    - logged
    - shown in a Toast
    - displayed on screen
    - written to an XML resource
    - persisted as plaintext by the application
    - committed to Git

The overall flow can be represented as:

local.properties / CI environment variable
|
v
BuildConfig.GEMINI_API_KEY
|
v
SecureApiKeyProvider
|
v
ApiKeyCryptoManager
|
v
Android Keystore AES-256 key
|
v
AES-256-GCM encryption
|
v
Encrypted API key
|
v
Preferences DataStore
|
v
Retrieve ciphertext
|
v
Decrypt in memory
|
v
GenerativeModel

---

# Why Android Keystore Is Used

The Android Keystore allows the application to create and use cryptographic keys without storing the raw encryption key directly in application source code.

The AES key used to encrypt the Gemini API key is generated inside the Android Keystore.

This raises the difficulty of extracting the stored Gemini API key from the application.

---

# Security Limitations

Client-side encryption improves security, but it cannot make an API key completely inaccessible to a determined attacker.

An attacker who controls the device, application process, or runtime environment may still be able to extract sensitive information.

For a production application, Gemini API requests should preferably be routed through a trusted backend server.

This would prevent the Gemini API key from being distributed with the Android application.

Additional production protections could include:

- Firebase App Check
- restricted API keys
- backend authentication
- authorization
- request validation
- rate limiting
- monitoring and abuse detection

---

# Release Build Protection

The release build enables R8 minification and resource shrinking.

The release configuration includes:

isMinifyEnabled = true
isShrinkResources = true

R8 obfuscates and optimizes the release APK, making the compiled application more difficult to inspect through straightforward decompilation.

This is an additional protection layer, but it does not replace secure backend handling for production secrets.

---

# Chat UI

The conversation interface is implemented using Jetpack Compose and Material 3.

The chat screen includes:

- separate user and Gemini message bubbles
- LazyColumn conversation rendering
- stable message IDs as LazyColumn keys
- automatic scrolling when new messages arrive
- loading indicator during Gemini requests
- Snackbar error handling
- responsive spacing
- adaptive chat-bubble widths
- rounded prompt input
- separate microphone and send buttons
- custom light and dark themes

---

# State Management

UI state is stored inside `ChatUiState`.

`ChatViewModel` exposes the state using:

StateFlow<ChatUiState>

The Compose UI observes the state using:

collectAsStateWithLifecycle()

This keeps the UI synchronized with the ViewModel while following state-hoisting principles.

ChatUiState contains information such as:

- the current prompt
- conversation messages
- loading state
- prompt validation errors
- API error messages

---

# Responsive Layout

The application uses Material 3 `WindowSizeClass` to adapt the interface to different screen sizes.

Different layout values are used for:

- Compact screens
- Medium screens
- Expanded screens

The responsive layout changes:

- maximum content width
- horizontal padding
- maximum chat-bubble width

This allows the interface to adapt to phones, tablets, and orientation changes.

---

# Voice Input

Voice input is implemented using Android's:

RecognizerIntent

Speech recognition is launched from Compose using:

rememberLauncherForActivityResult()

When the user taps the microphone button:

1. Android speech recognition is opened.
2. The user speaks their prompt.
3. Recognized speech is returned to the application.
4. The recognized text is inserted into the prompt field.
5. The user can edit the text before sending it to Gemini.

---

# Persistent Chat History

Chat history is stored locally using Room.

The Room-related files are located under:

app/src/main/java/com/fahim/geminiApiComposeStarter/data/local/

The main Room classes are:

ChatDatabase.kt
ChatHistoryRepository.kt
ChatMessageDao.kt
ChatMessageEntity.kt

Messages are persisted locally so that previous conversations survive application restarts.

---

# User Preferences

Preferences DataStore is used to store user preferences.

The preferences implementation is located under:

app/src/main/java/com/fahim/geminiApiComposeStarter/data/preferences/

The application currently remembers the selected theme preference.

Available theme options are:

- System
- Light
- Dark

The selected preference survives application restarts.

---

# Theme Support

The application supports both light and dark modes.

The dark theme uses:

- near-black background
- charcoal Gemini bubbles
- muted blue user bubbles
- blue primary-action controls
- light foreground text

The light theme uses:

- soft off-white background
- cool-gray Gemini bubbles
- pale-blue user bubbles
- blue primary-action controls
- dark foreground text

The application also supports following the system theme using:

isSystemInDarkTheme()

---

# Loading and Error Handling

While waiting for Gemini to return a response, the application displays:

CircularProgressIndicator

If an API request fails, the error is displayed using a Material 3 Snackbar.

This provides visible feedback instead of allowing requests to fail silently.

---

# Performance Optimization

Android Studio Layout Inspector was used to inspect Compose recomposition counts.

The application was tested while:

- typing a prompt
- sending a message
- waiting for Gemini
- receiving a Gemini response
- opening the theme menu
- leaving the app idle

The recomposition counts were monitored to check for unnecessary repeated recompositions.

The application uses several optimizations:

- immutable UI state models
- remembered responsive-layout configuration
- stable LazyColumn keys
- controlled recomposition behavior
- Gemini network work moved off the main thread

Gemini API work is executed using:

withContext(Dispatchers.IO)

This helps keep potentially blocking work away from the main UI thread and improves responsiveness.

---

# Testing

The project contains both local unit tests and Android Compose UI tests.

---

## ChatViewModel Unit Tests

The unit tests are located at:

app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt

The tests use:

- JUnit
- kotlinx-coroutines-test
- a fake GeminiRepository
- fake local chat-history components

The unit tests verify ViewModel behavior such as:

- empty prompt validation
- successful Gemini responses
- message creation
- loading state behavior
- error handling
- missing API key handling
- chat-history behavior

During development, the ChatViewModel unit test suite passed successfully.

---

## Run Unit Tests

### Windows PowerShell

Run:

.\gradlew.bat testDebugUnitTest

### macOS / Linux

Run:

./gradlew testDebugUnitTest

Local unit tests do not require an Android emulator or physical device.

---

# Compose UI Tests

Compose UI tests are located at:

app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt

The Compose tests use:

createComposeRule()

The UI tests verify that:

- user messages are displayed
- Gemini messages are displayed
- the prompt field accepts text
- the send button is displayed
- the voice-input button is displayed
- the loading indicator appears while waiting for a response

During development, the Compose UI test suite passed successfully.

---

## Run Compose UI Tests

A connected Android device or running emulator is required.

### Windows PowerShell

Run:

.\gradlew.bat connectedDebugAndroidTest

### macOS / Linux

Run:

./gradlew connectedDebugAndroidTest

Android Studio can also run the test by right-clicking:

ChatScreenTest.kt

and selecting:

Run 'ChatScreenTest'

---

# Build and Run the App

Requirements:

- Android Studio
- Android SDK
- Gemini API key
- Android emulator or physical Android device

Steps:

1. Clone the repository.

2. Open the project in Android Studio.

3. Create or edit the root-level:

   local.properties

4. Add:

   GEMINI_API_KEY=your_api_key_here

5. Sync the Gradle project.

6. Select an Android emulator or connected physical device.

7. Run the `app` configuration.

---

# Project Structure

app/
└── src/
├── main/
│   └── java/
│       └── com/fahim/geminiApiComposeStarter/
│
│           ├── data/
│           │   ├── local/
│           │   │   ├── ChatDatabase.kt
│           │   │   ├── ChatHistoryRepository.kt
│           │   │   ├── ChatMessageDao.kt
│           │   │   └── ChatMessageEntity.kt
│           │   │
│           │   ├── preferences/
│           │   │   └── UserPreferencesRepository.kt
│           │   │
│           │   ├── GeminiRepository.kt
│           │   └── GeminiRepositoryImpl.kt
│           │
│           ├── security/
│           │   ├── ApiKeyCryptoManager.kt
│           │   ├── ApiKeyStorage.kt
│           │   └── SecureApiKeyProvider.kt
│           │
│           ├── ui/
│           │   ├── chat/
│           │   │   ├── ChatScreen.kt
│           │   │   ├── ChatUiState.kt
│           │   │   └── ChatViewModel.kt
│           │   │
│           │   └── theme/
│           │       └── Theme.kt
│           │
│           └── MainActivity.kt
│
├── test/
│   └── java/
│       └── com/fahim/geminiApiComposeStarter/ui/chat/
│           └── ChatViewModelTest.kt
│
└── androidTest/
└── java/
└── com/fahim/geminiApiComposeStarter/ui/chat/
└── ChatScreenTest.kt

---

# Main Technologies Used

- Kotlin
- Jetpack Compose
- Material 3
- Gemini API
- Kotlin Coroutines
- StateFlow
- Room
- Preferences DataStore
- Android Keystore
- AES-256-GCM
- RecognizerIntent
- WindowSizeClass
- JUnit
- kotlinx-coroutines-test
- Compose UI Testing
- R8

---

# Git and Secret Safety

The real Gemini API key must never be committed.

Before committing changes, run:

git status

Make sure `local.properties` does not appear under staged files.

You can verify that it is ignored using:

git check-ignore -v local.properties

You can verify that it is not tracked using:

git ls-files local.properties

The last command should return no output.

A safe placeholder file is included:

local.properties.example

with:

GEMINI_API_KEY=your_api_key_here

---

# Assignment Branch

The assignment implementation branch is:

N053_MAD_Assignment

The branch name begins with the required roll number.

---

# Summary

This project enhances the original Gemini Jetpack Compose starter application with:

- secure Gemini API-key handling
- Android Keystore encryption
- persistent Room chat history
- saved DataStore preferences
- voice input
- responsive Material 3 UI
- light and dark themes
- loading and error states
- unit testing
- Compose UI testing
- performance inspection and optimization

The API key is configured through `local.properties`, encrypted at rest using AES-256-GCM with Android Keystore protection, and decrypted only when required for Gemini model creation.

The project includes both local unit tests and Android Compose UI tests, with commands documented above for running each test suite.