# C002-Dhruvil-Assignment1: Gemini Jetpack Compose Lab Assignment 1

Enhanced Android XR / Gemini Jetpack Compose application built for Lab Assignment 1.

**Student Details**: Dhruvil Kothari (Roll No: C002)

## Complete Feature Matrix

1. **Repository Pattern Architecture (`GeminiRepository`)**:
   - Decoupled ViewModel from network and SDK logic via `GeminiRepository` interface and `GeminiRepositoryImpl`.
   - Enabled unit testing with `FakeGeminiRepository`.

2. **Secure API Key Management (Android Keystore + AES-256-GCM)**:
   - Encrypts the Gemini API key at rest using `AES-256-GCM` inside the Android Keystore (`KeyGenParameterSpec`).
   - Ciphertext and IV stored in local SharedPreferences/DataStore. Decrypted in memory only when generating content.
   - `local.properties` environment variable fallback for CI builds.

3. **Build Configuration & R8 Obfuscation**:
   - `buildConfigField("String", "GEMINI_API_KEY", ...)` configured in `app/build.gradle.kts`.
   - Release build optimization and obfuscation enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
   - `local.properties.example` included in root repository.

4. **Room Database Persistence & Preferences DataStore**:
   - Room Database (`ChatDatabase`, `MessageEntity`, `ChatDao`) persists conversation history across app restarts.
   - Preferences DataStore (`PreferencesManager`) remembers user preferences (such as selected image index).

5. **Adaptive UI (WindowSizeClass & Material 3 Compose)**:
   - Responsive design using `WindowSizeClass` (`calculateWindowSizeClass`):
     - **Tablet / Landscape Mode**: Two-pane side-by-side layout (Controls & Image selector on left, Conversation list on right).
     - **Phone Portrait Mode**: Single-column stacked layout.
   - `LazyColumn` for chat bubbles (User vs. Gemini) with stable keys and auto-scrolling (`LazyListState.animateScrollToItem`).
   - State hoisting with `collectAsStateWithLifecycle()`.
   - Loading indicator (`CircularProgressIndicator`) and error handling (`Snackbar`).
   - Dark mode support and student name/roll number header.

6. **Voice Input (Speech-to-Text)**:
   - Speech recognition using `RecognizerIntent` launched via `rememberLauncherForActivityResult`.

7. **Unit & UI Testing**:
   - Unit tests (`BakingViewModelTest`) using `kotlinx-coroutines-test`, `RobolectricTestRunner`, and `FakeGeminiRepository`.
   - Compose UI tests (`BakingScreenTest`) using `createComposeRule()`.

---

## Setup Instructions

### 1. Configure API Key
1. Copy `local.properties.example` to `local.properties` in the project root.
2. Add your Gemini API Key:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```

### 2. Running the App
- Open project in Android Studio.
- Sync Gradle and run the app on an Android device or emulator.

### 3. Running Tests
- **Unit Tests**: Run `./gradlew testDebugUnitTest`
- **Instrumented UI Tests**: Run `./gradlew connectedAndroidTest`

---

## Production Security Considerations
In production, client-side key storage should be complemented by:
1. **Backend Proxy**: Routing requests through a secure server proxy so API keys are never distributed to client devices.
2. **Firebase App Check & Key Restrictions**: Restricting API keys in Google Cloud Console by package name and SHA-1 fingerprint, combined with Firebase App Check.
