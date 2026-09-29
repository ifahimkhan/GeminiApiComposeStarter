# Gemini Jetpack Compose Starter

An advanced, feature-rich Android client for Google Gemini built with modern **Jetpack Compose**, **Material 3**, and the **Google Generative AI SDK**.

---

## Getting Started & API Key Configuration

### 1. Obtain a Gemini API Key
- Access Google AI Studio: [https://aistudio.google.com/](https://aistudio.google.com/)
- Generate a new Gemini API Key.

### 2. Store the Key Locally
1. Copy `local.properties.example` to `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
2. The `local.properties` file is git-ignored and will never be committed to source control.
3. Gradle reads this file during build time and exposes it via `BuildConfig.GEMINI_API_KEY`.

### 3. CI/CD Environment Variable Fallback
For headless CI pipelines (e.g. GitHub Actions), Gradle falls back to the `GEMINI_API_KEY` environment variable if `local.properties` is absent:
```kotlin
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: ""
```

---

## Security Architecture & Encryption at Rest

### Hardware-Backed Keystore Encryption (AES-256-GCM)
The application implements client-side encryption-at-rest via [`ApiKeySecurityManager`](file:///Users/shaurya3006/AndroidStudioProjects/GeminiApiComposeStarter/app/src/main/java/com/fahim/geminiApiComposeStarter/security/ApiKeySecurityManager.kt):
1. **Key Generation**: On first launch, an AES-256-GCM key is generated inside the hardware-backed **Android Keystore** (`KeyGenParameterSpec`) with `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`.
2. **Encryption at Rest**: The raw API key from `BuildConfig` is encrypted with AES-256-GCM and only the ciphertext alongside its random Initialization Vector (IV) is persisted in private storage (`gemini_secure_store`).
3. **In-Memory Decryption**: Decryption occurs in-memory strictly when the `GenerativeModel` instance is created inside `GeminiRepositoryImpl` via an `apiKeyProvider` lambda.
4. **Zero-Leak Policy**: The decrypted key is never logged, toasted, printed, or exposed in UI state.
5. **R8 Code Obfuscation**: Release builds enable R8 code shrinking and obfuscation (`isMinifyEnabled = true`) to prevent reverse engineering of strings and bytecode.

### Security Considerations & Production Best Practices
> [!IMPORTANT]
> **Client-Side Limits**: While Android Keystore and R8 raise the bar significantly against casual inspection, no client-side secret is 100% immune against root/debugger compromise by a determined attacker on an untrusted device.

For enterprise production deployments:
- **Backend Proxy**: Move Gemini API calls behind a trusted backend server (Cloud Run, Firebase Cloud Functions, or custom API Gateway). The mobile client authenticates with the server (e.g., via OAuth / Firebase Auth), and the server securely communicates with Gemini.
- **Firebase App Check**: Protect backend endpoints and API quotas using Firebase App Check with Play Integrity provider to guarantee that only legitimate instances of the unmodified app can issue requests.
- **Key Restrictions**: Restrict Google Cloud API keys by package name, SHA-1 certificate fingerprint, and specific Gemini API endpoints.

---

## Features Implemented

- **Material 3 Adaptive UI**:
  - Chat bubbles styled with Material 3 elevation, shape, and contrast for User vs. Gemini.
  - Smooth auto-scrolling with `LazyListState.animateScrollToItem()`.
  - Comprehensive Markdown rendering: formatted headers, bold/italic, monospace code blocks, and full Markdown tables with horizontal scroll.
- **Dynamic Model Switcher**:
  - Live model selector dialog supporting `gemini-3.8-flash` (default), `gemini-3.5-flash`, `gemini-3.1-flash-lite`, `gemini-3.1-pro`, `gemini-2.5-pro`, and custom model IDs.
- **Multimodal Inputs**:
  - **Camera Capture**: Integrated `ActivityResultContracts.TakePicture` with `FileProvider` (`content://`) for high-resolution photo inputs.
  - **Gallery Picker**: Multiple image selection with preview thumbnails and chip removal.
  - **Document Attachments**: Text/code/document file attachments with dynamic size and type detection.
  - **Voice Input (STT)**: Speech-to-text integration using `RecognizerIntent` transcribing directly into the prompt field for editing prior to send.
- **Multi-Session Chat History**:
  - Slide-out Navigation Drawer organizing conversation sessions with timestamps and message previews.
  - Persistent on-device storage surviving app restarts.
  - Quick action to create new chats, switch between sessions, or delete individual chats.
- **"Open Gemini" Quick Launch**:
  - Sidebar button with official Google Gemini 4-color gradient emblem.
  - Subtle rotating gradient border animation with continuous seamless loop.
  - Direct deep-link launching the official Gemini Android app (`com.google.android.apps.bard`) with seamless fallback to Google Play Store.

---

## Testing

### Running Unit Tests
Unit tests verify the `ChatViewModel` state transitions, coroutine dispatchers, session switching, and prompt validations against a `FakeGeminiRepository`:
```bash
./gradlew testDebugUnitTest
```
*Test File*: [`ChatViewModelTest.kt`](file:///Users/shaurya3006/AndroidStudioProjects/GeminiApiComposeStarter/app/src/test/java/com/fahim/geminiApiComposeStarter/ChatViewModelTest.kt)

### Running UI / Instrumented Tests
Compose UI tests verify component rendering, top bar title, model selection pill, empty state welcome screen, and user interactions using `createComposeRule()`:
```bash
./gradlew connectedAndroidTest
```
*Test File*: [`ChatScreenTest.kt`](file:///Users/shaurya3006/AndroidStudioProjects/GeminiApiComposeStarter/app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ChatScreenTest.kt)
