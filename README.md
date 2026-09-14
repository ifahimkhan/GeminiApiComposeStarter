Mobile Application Development Lab

This project is an enhanced version of the Gemini Jetpack Compose starter application developed as part of Mobile Application Development Assignment 1.

The application provides a Gemini-powered chat interface with an improved user interface, secure API key handling, persistent chat storage, voice input, responsive layouts, and testing support.

Features
Enhanced Chat UI
Built using Jetpack Compose and Material 3.
Displays conversations using LazyColumn.
Uses Material 3 styled chat bubbles.
Automatically scrolls to the latest message.
Provides a clean and responsive chat experience.
State Management
Uses ChatUiState for managing UI state.
Uses StateFlow for state management.
Uses lifecycle-aware state collection with collectAsStateWithLifecycle.
Separates UI state from application logic.
Responsive UI
Supports different screen sizes and orientations.
Uses adaptive layouts to provide a responsive user experience.
Loading and Error Handling
Displays a loading state while waiting for Gemini responses.
Handles API and application errors.
Provides appropriate feedback when a request cannot be completed.
Material 3 and Dark Mode
Uses Material 3 components and theming.
Supports dark mode for a better user experience.
Voice Input
Supports voice input using Android speech recognition.
Uses RecognizerIntent to convert spoken input into text.
The recognized text can be used directly in the chat interface.
Chat History

The application provides persistent storage for conversations using:

Room Database for storing chat history.
Preferences DataStore for storing application preferences.

This allows relevant application data to persist between sessions.

Gemini API Integration

The application uses the Gemini API to generate responses to user messages.

The API interaction is handled through the application's repository layer rather than directly from the user interface.

API Key Security

The Gemini API key is not hardcoded in the Kotlin source code, XML files, or Gradle files.

The API key is stored locally using the local.properties file.

Example:

GEMINI_API_KEY=your_api_key_here

The actual API key must never be committed to Git or uploaded to GitHub.

A local.properties.example file is included as a template for local configuration.

API Key Protection

The application implements additional protection for the API key:

The API key is kept outside the source code.
local.properties is not committed to the repository.
Environment-variable fallback is supported.
AES-256-GCM encryption is used for securely storing the API key.
Android Keystore is used for protecting the encryption key.
The decrypted API key is only kept in memory when required by the Gemini API client.
The decrypted API key is never logged or displayed to the user.

Never share the Gemini API key publicly or commit it to GitHub.

Project Structure

The project follows a standard Android Jetpack Compose structure.

GeminiApiComposeStarter
├── app
├── gradle
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties.example
└── README.md

Testing

The project includes testing support for the application's main functionality.

Unit Testing

The ChatViewModel can be tested using:

Kotlin coroutine testing utilities.
A fake Gemini repository.
ViewModel state verification.
Compose UI Testing

Compose UI tests are included to verify important user interface behaviour.

Running the Project
Prerequisites
Android Studio
Android SDK
JDK compatible with the project
Android emulator or physical Android device
A valid Gemini API key
Steps
Open the project in Android Studio.
Create a local.properties file in the project root if it does not already exist.
Add your Gemini API key to the file.
Sync the Gradle project.
Connect an Android device or start an emulator.
Build and run the application from Android Studio.
Running Tests

To run unit tests from the project root on Windows:

.\gradlew.bat test

For connected Android and Compose tests:

.\gradlew.bat connectedAndroidTest

The second command requires a connected Android device or running emulator.

Release Build and Optimization

R8/minification is enabled for release builds to help reduce application size and improve protection of application code.

For a production application, additional security measures such as a backend proxy, Firebase App Check, and properly restricted API credentials should be considered.

Git and Branching

The assignment work is completed on the dedicated branch:

C054_Assignment-1

The completed work is pushed to the repository and submitted through a Pull Request.

Security Checklist
No Gemini API key is hardcoded in the source code.
local.properties is not committed.
local.properties.example contains only a placeholder.
API keys are not logged or displayed.
API key encryption uses AES-256-GCM.
Android Keystore is used for key protection.
Sensitive plaintext values are only kept in memory when required.
Release builds use R8/minification.
Assignment Requirements Covered

The project covers the major requirements of Assignment 1, including:

Enhanced Jetpack Compose chat UI
Material 3 components
LazyColumn chat interface
Automatic scrolling
State hoisting and StateFlow
Lifecycle-aware state collection
Responsive/adaptive layouts
Loading and error states
Dark mode/theme support
Voice input
Room Database
Preferences DataStore
Secure Gemini API key handling
AES-256-GCM encryption
Android Keystore
Unit testing
Compose UI testing
Network operations away from the main thread
R8/minification
Security documentation
Author

Shreya Dikhonda

Mobile Application Development – Assignment 1

NMIMS / STME