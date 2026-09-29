# GeminiApiComposeStarter

## Overview
GeminiApiComposeStarter is an enhanced Gemini-powered Android application built using Jetpack Compose and Material 3 for the Mobile Application Development Lab Assignment 1. The application features a reactive conversational chat experience with Google's Gemini generative AI model, encrypted API key management via Android Keystore, speech-to-text input, adaptive layouts across form factors, offline persistence with Room and Preferences DataStore, comprehensive automated unit test coverage, and optimized Compose recomposition performance.

---

## Features
1. **Secure Gemini API Key Handling**: API key is injected via `local.properties` (git-ignored) or environment variables at build time and encrypted at runtime using Android Keystore and AES-256-GCM.
2. **Android Keystore AES-256-GCM Encryption**: Plaintext key is encrypted on first launch; ciphertext and IV are securely stored in private preferences. Plaintext is decrypted on demand in memory and never exposed to UI state or logs.
3. **R8 Release Minification**: Release builds are minified, obfuscated, and optimized with R8 and tailored ProGuard rules.
4. **Material 3 Chat Interface**: Distinct chat bubbles for user queries and Gemini responses following Material 3 design tokens.
5. **Multi-Message Conversation**: Fluid chat history supporting back-and-forth interactions.
6. **Stable LazyColumn Keys & Auto-Scroll**: `LazyColumn` items are indexed with stable, unique UUID-based keys. `LaunchedEffect` smoothly auto-scrolls to the newest message or loading indicator.
7. **Loading & Error States**: Clear visual feedback including an animated loading indicator, actionable Snackbars, and dialog confirmations.
8. **Responsive WindowSizeClass Layout**: Dynamic content constraints adapting gracefully to Compact (phones), Medium (foldables/small tablets), and Expanded (tablets and landscape) window sizes.
9. **Voice Input via RecognizerIntent**: Speech-to-text integration using `RecognizerIntent` launched with `rememberLauncherForActivityResult`, populating the prompt input safely without auto-sending.
10. **Room Chat-History Persistence**: Local SQLite database via Room persisting chat history across app restarts with support for clearing conversations.
11. **Preferences DataStore Theme Persistence**: User dark/light theme preference persisted asynchronously via Jetpack Preferences DataStore.
12. **Automated Unit Testing**: 32 unit tests covering database operations, repository logic, ViewModel state transformations, and DataStore preferences using Robolectric and Coroutines Test.
13. **Compose Performance Optimization**: Recomposition-scoped theme observation, cached markdown parsing, remembered layout calculations, and static shape allocation to ensure smooth 60fps UI interactions.

---

## Tech Stack
- **Kotlin**: 2.0.21
- **Jetpack Compose**: Compose BOM `2024.09.00`
- **Material 3**: `androidx.compose.material3:material3` & `material3-window-size-class`
- **Android Gradle Plugin (AGP)**: 9.0.0
- **Google Generative AI SDK**: `com.google.ai.client.generativeai:generativeai:0.9.0`
- **Security Crypto**: `androidx.security:security-crypto:1.1.0-alpha06` (Android Keystore + AES-256-GCM)
- **Room Database**: `androidx.room:room-runtime:2.7.0` & `room-ktx:2.7.0`
- **KSP (Kotlin Symbol Processing)**: 2.0.21-1.0.28
- **Preferences DataStore**: `androidx.datastore:datastore-preferences:1.1.2`
- **Coroutines & Lifecycle**: `kotlinx-coroutines-android`, `lifecycle-runtime-ktx:2.10.0`, `lifecycle-viewmodel-compose:2.10.0`
- **Testing Libraries**: JUnit 4.13.2, Robolectric 4.14.1, `kotlinx-coroutines-test:1.9.0`, `androidx.test:core:1.6.1`
- **Build System**: Gradle 9.3.1 (with Java 11 compilation target)
- **Code Shrinker / Obfuscation**: R8

---

## Requirements
- **Android Studio**: Ladybug (2024.2.1) / Meerkat or newer
- **JDK**: Java 11 or higher (Android Studio bundled JBR 17/21 recommended)
- **Android SDK**:
  - `compileSdk`: 36
  - `targetSdk`: 36
  - `minSdk`: 26 (Android 8.0 Oreo)
- **Device / Emulator**: Physical device or Android Virtual Device running API 26 or higher
- **Gemini API Key**: Valid API key generated from Google AI Studio

---

## Gemini API Key Setup

1. **Obtain an API Key**:
   - Go to [Google AI Studio](https://aistudio.google.com/).
   - Sign in with your Google account and select **Get API key**.
   - Create and copy your Gemini API key.

2. **Configure `local.properties`**:
   - In the root directory of the project, create or open `local.properties` (this file is excluded by `.gitignore` and must never be committed).
   - Add your Gemini API key:
     ```properties
     GEMINI_API_KEY=your_actual_api_key_here
     ```

3. **Template Reference**:
   - `local.properties.example` is committed to the repository as a reference template:
     ```properties
     GEMINI_API_KEY=your_api_key_here
     ```

4. **Environment Variable Fallback**:
   - As an alternative to `local.properties`, the build script in `app/build.gradle.kts` also checks for an environment variable named `GEMINI_API_KEY`. This is useful for CI/CD environments:
     ```bash
     export GEMINI_API_KEY="your_actual_api_key_here"
     ```

> **Warning**: Never hardcode or commit your Gemini API key into any tracked file or version control.

---

## Security Architecture

```
Build-Time (local.properties / Env Var)
                │
                ▼
BuildConfig.GEMINI_API_KEY
                │
                ▼
SecureApiKeyManager (Application Init)
                │
         Is key encrypted?
           ├── No  ──► Encrypt with Android Keystore (AES-256-GCM) ──► Store Ciphertext + IV in EncryptedPrefs
           └── Yes ──► Stored securely on device
                │
                ▼
GeminiRepositoryImpl (Lazy Key Provider)
                │
                ▼
Decrypted in memory ONLY when GenerativeModel is instantiated
                │
                ▼
Gemini API Request (Google AI SDK)
```

### Security Flow
1. **Build Configuration**: The key is supplied via `local.properties` or environment variables and injected into `BuildConfig` at compile time.
2. **Android Keystore AES-256-GCM**: During app initialization, `SecureApiKeyManager` checks for stored ciphertext. If absent, it encrypts the key using an AES-256 key generated in the hardware-backed Android Keystore and persists the ciphertext along with the initialization vector (IV) in private SharedPreferences.
3. **Lazy Memory Decryption**: `GeminiRepositoryImpl` uses a lazy key provider (`lazyApiKeyProvider`), decrypting the key into memory only when `GenerativeModel` needs to execute a network query.
4. **State Isolation**: The decrypted API key is never placed into `ChatUiState`, never passed down Compose hierarchies, never saved to Room or DataStore, and never logged.
5. **R8 Minification & Obfuscation**: Release builds enable R8 to strip unused metadata and obfuscate class and field names.

### Production Considerations & Limitations
- **Client-Side Limitation**: Client-side encryption provides defense-in-depth against casual inspection, but on a rooted device with physical access or runtime instrumentation (e.g., Frida), an attacker can inspect memory or hook cryptographic APIs. No client-side key storage mechanism is 100% immune to a determined attacker.
- **Production Architecture**: For production applications, API keys should never reside directly in the mobile client. Instead:
  - Route Gemini requests through a secure backend proxy server.
  - Authenticate client requests using short-lived OAuth2/OpenID tokens.
  - Utilize client attestation services such as **Firebase App Check** or Play Integrity API to ensure only genuine, untampered app instances can communicate with the backend.

---

## Project Structure
```
GeminiApiComposeStarter/
├── app/
│   ├── build.gradle.kts                     # App-level build config, SDK versions, dependencies
│   ├── proguard-rules.pro                   # R8 and ProGuard rules for release builds
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # App manifest, permissions, speech recognition queries
│       │   └── java/com/fahim/geminiApiComposeStarter/
│       │       ├── MainActivity.kt          # Single-activity host with WindowSizeClass calculation
│       │       ├── data/
│       │       │   ├── ChatHistoryRepository.kt      # Repository for Room message persistence
│       │       │   ├── GeminiRepository.kt           # Interface for Gemini API calls
│       │       │   ├── GeminiRepositoryImpl.kt       # Generative AI SDK client with lazy key provider
│       │       │   ├── local/
│       │       │   │   ├── AppDatabase.kt            # Room database definition
│       │       │   │   ├── ChatMessageDao.kt         # Room DAO for message operations
│       │       │   │   └── ChatMessageEntity.kt      # Room entity mapping
│       │       │   └── preferences/
│       │       │       └── UserPreferencesRepository.kt # Preferences DataStore for theme
│       │       ├── security/
│       │       │   └── SecureApiKeyManager.kt        # Android Keystore AES-256-GCM manager
│       │       └── ui/
│       │           ├── chat/
│       │           │   ├── ChatMessage.kt            # UI message domain model
│       │           │   ├── ChatScreen.kt             # Material 3 Compose chat UI & bubbles
│       │           │   ├── ChatUiState.kt            # Immutable UI state definition
│       │           │   └── ChatViewModel.kt          # ViewModel managing chat, persistence, and theme
│       │           └── theme/                        # Material 3 Color, Type, and Theme definitions
│       └── test/java/com/fahim/geminiApiComposeStarter/
│           ├── AppDatabaseTest.kt                    # Room database unit tests (Robolectric)
│           ├── ChatViewModelTest.kt                  # ViewModel unit tests (coroutines test dispatcher)
│           ├── GeminiRepositoryImplTest.kt           # Gemini repository unit tests
│           └── UserPreferencesRepositoryTest.kt      # DataStore preferences unit tests
├── gradle/
│   ├── libs.versions.toml                   # Version catalog (dependencies and plugins)
│   └── wrapper/                             # Gradle wrapper files
├── build.gradle.kts                         # Top-level build configuration
├── local.properties.example                 # Example template for local configuration
└── README.md                                # Project documentation
```

---

## Persistence
- **Room Database (`AppDatabase`)**:
  - Persists chat messages (`ChatMessageEntity`) containing unique UUID string IDs, sender role (`USER` or `ASSISTANT`), text content, and Unix epoch timestamps.
  - Automatically loads full conversation history upon application startup via a reactive Kotlin `Flow`.
  - Supports full conversation clearance (`clearAll()`).
  - No secrets or API keys are ever stored in Room.
- **Preferences DataStore (`UserPreferencesRepository`)**:
  - Asynchronously persists the user's dark/light theme preference using Jetpack Preferences DataStore.
  - Emits preference updates through a reactive `Flow<Boolean>`, ensuring seamless theme transitions across app launches.

---

## Voice Input
- Uses Android's speech recognition framework via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` configured with `LANGUAGE_MODEL_FREE_FORM`.
- Launched from Compose using `rememberLauncherForActivityResult` with `ActivityResultContracts.StartActivityForResult()`.
- Successfully recognized text is extracted from `RecognizerIntent.EXTRA_RESULTS` and appended/populated into the prompt input field.
- **Safety**: Voice input does not automatically transmit the query. The user retains full control to review, edit, or append to the recognized text before pressing send.
- **Fallback**: Handles missing speech recognition services gracefully by surfacing an error message without crashing.

---

## Performance & Optimization
Optimizations implemented during Phase 7 to eliminate unnecessary recompositions and minimize latency:
1. **Scoped Theme State**: In `MainActivity`, the theme wrapper observes a targeted `viewModel.isDarkTheme` StateFlow rather than the entire `uiState`. Keystrokes in the prompt input field no longer trigger root-level theme recomposition.
2. **Cached Markdown Parsing**: Parsing bold markdown syntax (`**text**`) into `AnnotatedString` is memoized using `remember(text)` inside `AssistantMessageBubble`.
3. **Static Shapes & Remembered Modifiers**: Bubble shapes (`UserBubbleShape`, `AssistantBubbleShape`, `PromptTextFieldShape`) are statically allocated at the file level. Responsive layout constraints (`maxContentWidth`, `horizontalPadding`, and `bubbleMaxWidthModifier`) are cached using `remember(windowWidthSizeClass)`.
4. **Stable Callback Lambdas**: Event handlers in `ChatRoute` (`onVoiceClick`, `onVoiceErrorDismissed`) are wrapped with `remember` to preserve reference equality across recompositions.
5. **Thread Confinement**: Asynchronous network calls and database operations are confined to coroutines in `viewModelScope` and background dispatchers, keeping the main thread free for fluid 60fps UI animations and scrolling.

---

## Testing

### Automated Unit Tests
The project contains 32 comprehensive unit tests across 4 test suites:
- **`AppDatabaseTest` (8 tests)**: Verifies Room DAO insertion, ordering by timestamp, duplicate conflict replacement, and clearing history using in-memory databases with Robolectric.
- **`ChatViewModelTest` (13 tests)**: Verifies prompt validation, loading state management, Gemini response handling, error presentation, dark theme toggles, and voice error dismissal.
- **`GeminiRepositoryImplTest` (7 tests)**: Verifies prompt dispatching, error mapping, and lazy API key decryption lifecycle.
- **`UserPreferencesRepositoryTest` (4 tests)**: Verifies DataStore theme preference persistence and default values using temporary test DataStore files.

### Running Tests and Builds

**Run Unit Tests**:
```powershell
.\gradlew.bat clean test
```

**Build Debug APK**:
```powershell
.\gradlew.bat clean assembleDebug
```

**Build Release APK (with R8 minification)**:
```powershell
.\gradlew.bat clean assembleRelease
```

---

## Build & Run Instructions
1. **Clone the Repository**:
   ```bash
   git clone -b N043-assignment-1 <repository-url>
   cd GeminiApiComposeStarter
   ```
2. **Configure API Key**:
   Create `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   ```
3. **Open in Android Studio**:
   - Open Android Studio and select **Open** -> choose the project root.
   - Allow Gradle to sync dependencies.
4. **Run Unit Tests**:
   - In Android Studio, right click on `app/src/test/java` and select **Run 'Tests in 'geminiApiComposeStarter''**, or run `.\gradlew.bat test`.
5. **Run the App**:
   - Select your target emulator or physical device.
   - Click the green **Run** button (or press `Shift + F10`).

---

## Submission & Git Checklist
- [x] Branch name begins with student roll number (`N043-assignment-1`).
- [x] `local.properties` is strictly ignored by Git and not tracked.
- [x] No API keys, credentials, or secrets appear in tracked files or Git history.
- [x] `local.properties.example` is committed with a placeholder.
- [x] All 32 automated unit tests pass successfully.
- [x] Debug build (`assembleDebug`) succeeds cleanly.
- [x] Release build (`assembleRelease`) with R8 minification succeeds cleanly.
- [x] Complete README documentation included.
- [x] PR description lists all implemented features with attached screenshots.
