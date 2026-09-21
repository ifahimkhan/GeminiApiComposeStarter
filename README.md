# Gemini AI Chat - Mobile Application Development Assignment 1

An advanced Android application built using **Kotlin**, **Jetpack Compose (Material 3)**, and Google's **Gemini Generative AI API**. This project fulfills all requirements for Mobile Application Development Lab Assignment 1, demonstrating secure API key handling, hardware-backed cryptography, persistent chat history, voice-to-text input, adaptive layouts, and thorough automated testing.

---

## 🚀 Features Implemented

1. **API Key Security & Environment Fallback**
   - API keys are never hardcoded in Kotlin, XML, strings, or committed Gradle files.
   - Sourced at build time from `local.properties` (gitignored) with seamless fallback to `System.getenv("GEMINI_API_KEY")` for CI/CD pipelines.
   - Clean template provided in `local.properties.example`.

2. **Hardware-Backed API Key Encryption**
   - AES-256-GCM master key generated inside the **Android Keystore** (`AndroidKeyStore`).
   - The Gemini API key is encrypted on first launch with a fresh 12-byte Initialization Vector (IV).
   - Only encrypted ciphertext and IV are persisted in Jetpack **Preferences DataStore**.
   - Decryption occurs strictly in-memory at the instant the `GenerativeModel` executes an API call; the key is never logged, toasted, or written to disk in plain text.
   - In-app configuration dialog allows secure runtime entry/updating of the API key.

3. **Modern Jetpack Compose Chat Interface (Material 3)**
   - Single-activity Compose architecture adhering to unidirectional data flow (UDF).
   - Conversation rendered inside an optimized `LazyColumn` with stable keys (`key = { it.id }`).
   - Automatically animates scrolling to the newest message upon receipt.
   - Distinct, styled chat bubbles:
     - **User Messages**: Aligned to the right with primary container styling and asymmetric rounded corners.
     - **Gemini Responses**: Aligned to the left with surface variant styling, assistant avatar icon, formatted bold markdown parsing (`toBoldAnnotatedString`), and readable timestamps.
   - Animated typing / loading indicator bubble (`CircularProgressIndicator`) with status messaging while generation is active.
   - Robust error handling via Material 3 error banners and Snackbars with user-friendly sanitized messages (preventing sensitive error leakage).

4. **Speech-to-Text Voice Input**
   - Microphone button integrated into the prompt bar.
   - Uses `rememberLauncherForActivityResult` with `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
   - Transcribed voice input smoothly populates the prompt field.
   - Handles device limitations gracefully if speech recognition is unavailable without crashing.

5. **Chat History Persistence (Room Database)**
   - Conversation history persists across app restarts using **Jetpack Room**.
   - Structured Room database (`ChatDatabase`), DAO (`ChatDao`), and entity (`ChatMessageEntity`).
   - Clear conversation action with confirmation dialog.

6. **User Preferences (Preferences DataStore)**
   - Remembers user settings across app restarts via **Preferences DataStore**.
   - Dynamic Theme support: **System Default**, **Light Mode**, and **Dark Mode** with dynamic colors on Android 12+.

7. **Responsive & Adaptive Layout**
   - Ergonomic layout adaptation for smartphones, tablets, and landscape orientations.
   - Centered container with responsive maximum width (`840.dp`) preventing unnaturally stretched bubbles on large screens.

8. **Release Build Obfuscation**
   - Configured `isMinifyEnabled = true` for release builds.
   - Custom ProGuard/R8 rules in `proguard-rules.pro` protecting Generative AI models, Room entities, and Coroutines.

9. **Automated Testing Suite**
   - **Unit Tests**: Full suite for `ChatViewModel` using `kotlinx-coroutines-test` and a fake repository.
   - **Compose UI Tests**: Instrumented UI tests using `createComposeRule()` verifying component rendering, user interactions, message bubbles, and loading indicators.

---

## 🔑 How to Obtain a Gemini API Key

1. Navigate to [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account.
3. Click on **Get API key** and create a key in a new or existing Google Cloud project.
4. Copy your API key.

---

## 🛠️ Configuration & Setup

### 1. Configure `local.properties`
Create or open the `local.properties` file in the root project directory and add your API key:

```properties
GEMINI_API_KEY=your_actual_api_key_here
```

> [!CAUTION]
> **NEVER commit `local.properties` to version control.**
> `local.properties` is explicitly listed in `.gitignore` to prevent secret leaks. Teammates can refer to the provided `local.properties.example` template:
> ```properties
> GEMINI_API_KEY=your_api_key_here
> ```

### 2. CI/CD Environment Variable Fallback
If building in a continuous integration environment (e.g., GitHub Actions), set the environment variable:

```bash
export GEMINI_API_KEY="your_api_key_here"
```

In `app/build.gradle.kts`, Gradle automatically prioritizes `local.properties` and falls back to `System.getenv("GEMINI_API_KEY")`.

---

## 🔐 Architecture: Keystore Encryption Flow

```
┌─────────────────────────────────┐
│ local.properties / BuildConfig  │
└────────────────┬────────────────┘
                 │ (First launch / runtime entry)
                 ▼
┌─────────────────────────────────┐
│         CryptoManager           │
│   (Android Keystore Master Key) │
│           AES-256-GCM           │
└────────────────┬────────────────┘
                 │ (Ciphertext + 12-byte IV)
                 ▼
┌─────────────────────────────────┐
│   Preferences DataStore (Disk)  │
│   [Never stores plain text key] │
└────────────────┬────────────────┘
                 │ (On-demand in-memory decryption)
                 ▼
┌─────────────────────────────────┐
│       GenerativeModel (RAM)     │
│   [Never logged, never toasted] │
└─────────────────────────────────┘
```

1. **Key Generation**: An AES-256 key is created inside the hardware-backed Android Keystore with `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, and `ENCRYPTION_PADDING_NONE`.
2. **Encryption at Rest**: The key is encrypted into ciphertext with an authenticated tag and a cryptographically random 12-byte IV.
3. **Persistence**: Only Base64-encoded ciphertext and IV are saved in Jetpack Preferences DataStore.
4. **Decryption**: When a user dispatches a message, the key is decrypted in memory for the lifecycle of the network request.

---

## 🧪 Running Tests

### Running Unit Tests
Executes ViewModel state transitions, error handling, prompt validation, and repository interactions:

```bash
./gradlew testDebugUnitTest
```

### Running Compose UI Tests
Runs instrumented Compose tests on an active Android device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

---

## 📦 Building the Application

### Debug Build
```bash
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (Minified & Obfuscated with R8)
```bash
./gradlew assembleRelease
```
Output APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 🛡️ Production Security Considerations & Limitations

While client-side encryption with Android Keystore significantly raises the barrier against static reverse-engineering, **no client-side secret is 100% impenetrable against a determined attacker** with physical access, root privileges, or memory instrumentation tools (e.g., Frida, Xposed):

1. **Rooted Devices & Memory Inspection**: An attacker on a rooted device can attach a debugger or use dynamic binary instrumentation to hook memory during `GenerativeModel` construction.
2. **Backend Proxy Pattern (Recommended for Production)**:
   In a production enterprise architecture, API keys should **never** reside on mobile clients. Instead:
   - The mobile client authenticates the user (e.g., via OAuth2/Firebase Auth).
   - The app makes requests to a **Backend-For-Frontend (BFF)** or serverless proxy (e.g., Cloud Functions).
   - The backend server validates the user token, enforces rate limits and quota abuse protections, and communicates with the Gemini API using server-side service credentials.
3. **Firebase App Check / Play Integrity**:
   - Integrate **Play Integrity API** or **Firebase App Check** to ensure requests originate exclusively from genuine, unmodified instances of your application.
4. **Google Cloud Key Restrictions**:
   - Restrict the Gemini API key in the Google Cloud Console by Android package name (`com.fahim.geminiApiComposeStarter`) and release SHA-1 signing fingerprint.
