# GeminiApiComposeStarter

This project is a mobile chat application built using modern Android architecture, Jetpack Compose, and the Google Gemini API. It serves as a comprehensive example of integrating Generative AI into an Android application with a focus on UI responsiveness, security, and offline persistence.

## Features Implemented

*   **Gemini API Integration:** Generates intelligent text responses to user prompts using the Gemini 3.5 Flash model.
*   **Material 3 Chat UI:** Features a complete conversational interface built with Jetpack Compose.
*   **LazyColumn Conversation:** Displays a scrolling list of messages with distinct visual chat bubbles for the user and the Gemini AI.
*   **Stable Chat Item Keys:** Uses stable IDs to ensure optimal UI recomposition.
*   **Automatic Scroll-to-Latest-Message:** The chat view automatically scrolls down when a new message is added.
*   **StateFlow-based UI State:** The Chat UI state is reactively hoisted in the ViewModel using StateFlow.
*   **Lifecycle-aware Compose Collection:** Uses `collectAsStateWithLifecycle()` for proper state collection that respects the Android lifecycle.
*   **Adaptive Phone/Tablet Layout:** Integrates `WindowSizeClass` to intelligently constrain chat bubble width on wider screens.
*   **Loading State:** Displays a `CircularProgressIndicator` during active generation.
*   **Error Handling:** gracefully handles empty prompts, missing API keys, or network failures, showing sensible UI messages in a Snackbar instead of crashing.
*   **Speech-to-Text Input:** Uses `RecognizerIntent` and Compose launcher APIs to enable voice input natively without external libraries.
*   **Room Chat Persistence:** Local SQLite database securely caches the user's conversational history.
*   **Preferences DataStore:** Saves the user's theme selection.
*   **System/Light/Dark Themes:** Reactively switches visual themes on the fly.
*   **Dynamic Material 3 Colors:** Supported seamlessly on newer Android builds.
*   **AES-256-GCM Encrypted API-key Storage:** Safely encrypts the API key before persisting it to disk on first launch.
*   **Android Keystore:** Leverages the Android Keystore to generate and house the encryption key securely on device hardware.
*   **CI Environment-Variable Fallback:** Allows continuous integration build servers to provide the Gemini API key securely through environmental variables.
*   **R8 Release Obfuscation:** Configured to shrink and obfuscate production builds to enhance security and reduce binary size.

## Architecture Overview

The app follows a modern Android layered architecture:
*   **UI Layer:** Composable functions (`ChatScreen`) observing `StateFlow` from `ChatViewModel`.
*   **Data Layer:** Repositories (`GeminiRepository`, `ChatHistoryRepository`, `PreferencesRepository`) handle abstracting data sources.
*   **Local Storage:** Uses Room for structured relational data (chat history) and Preferences DataStore for simple key-value pairs (theme mode).
*   **Security:** `SecureApiKeyStore` encapsulates the logic to manage the API key securely.

## How to Clone

```bash
git clone https://github.com/ifahimkhan/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
git checkout N003-lab1
```

## How to Configure GEMINI_API_KEY

This project requires a valid Google Gemini API key.

1.  Visit [Google AI Studio](https://aistudio.google.com/) and create a new project and API Key.
2.  In the root of the project directory, you will find `local.properties.example`.
3.  Create a file named `local.properties` in the same directory (it is ignored by Git to prevent secrets from being tracked).
4.  Add the following line to `local.properties`, replacing the placeholder with your actual key:

```properties
GEMINI_API_KEY=your_real_key_here
```

### Environment Variable Fallback

For CI/CD environments (like GitHub Actions), the build script will automatically fall back to reading an environment variable named `GEMINI_API_KEY`. Set this variable in your CI platform's secret manager.

## Security Architecture

To mitigate the risk of hardcoding API keys or storing them as plain text on the device:

1.  **Build-Time Configuration:** The key is read from `local.properties` or the environment and injected into `BuildConfig`.
2.  **First Launch Encryption:** On the very first launch, the app takes the `BuildConfig` key and generates a strong AES-256-GCM key inside the Android Keystore.
3.  **Encrypted Client-Side Storage:** The API key is encrypted using AES-GCM and only the raw ciphertext (and Initialization Vector) is persisted in a local DataStore.
4.  **In-Memory Decryption:** When the user attempts a chat, the API key is decrypted in memory and directly passed to the `GenerativeModel`. The decrypted key is never logged or shown.

### Production Recommendation

While client-side encryption using the Android Keystore provides a strong defense against casual inspection and stops the key from resting unencrypted on disk, **it does not make a client-side API key impossible to extract**. A sufficiently motivated attacker on a rooted device can hook into the decryption routine or memory dump the process to extract the plaintext key when it's passed to the SDK.

For a true production application, you should **never** ship the API key to the client. Instead, use a backend proxy server (like a Cloud Function) to hold the secret, validate user requests (e.g., using Firebase App Check), and securely communicate with the Gemini API on the client's behalf.

## How to Run

### Run the App
Open the project in Android Studio, select your emulator or device, and click the "Run" button. Alternatively, use Gradle:
```bash
./gradlew installDebug
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Run Instrumentation/Compose Tests
Ensure an emulator is running, then execute:
```bash
./gradlew connectedDebugAndroidTest
```

### Generate a Release Build
The release build has R8 minification enabled.
```bash
./gradlew assembleRelease
```

## Screenshots

*(Placeholders for UI Screenshots)*

*   **Empty State:** *Screenshot missing*
*   **User/Gemini Conversation:** *Screenshot missing*
*   **Loading State:** *Screenshot missing*
*   **Error State:** *Screenshot missing*
*   **Voice Input:** *Screenshot missing*
*   **Dark Theme:** *Screenshot missing*
*   **Wide/Tablet Layout:** *Screenshot missing*
