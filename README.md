# Gemini Android Starter (Jetpack Compose)

A production-ready Android conversational application demonstrating secure integration with the **Google Gemini API** using Jetpack Compose, Material Design 3, Room Database, Preferences DataStore, and Android Keystore AES-256-GCM encryption.

---

## 1. Obtaining a Gemini API Key

1. Navigate to [Google AI Studio: Get API Key](https://aistudio.google.com/app/apikey).
2. Sign in with your Google account.
3. Click **"Create API key"** and choose to generate a key in a new or existing Google Cloud project.
4. Copy the generated key string.
5. In the root directory of this repository, open `local.properties` (which is already git-ignored) and add:
   ```properties
   GEMINI_API_KEY=AIzaSyYourGeneratedApiKeyHere
   ```
6. Sync and rebuild the project in Android Studio or via Gradle.

> [!WARNING]
> Never hardcode or commit your Gemini API key to version control. The repository contains a `local.properties.example` template for reference.

---

## 2. API Key Security & Encryption at Rest

Client-side keys require defense-in-depth security measures to protect against tampering and inspection:

### Encryption Flow (Android Keystore + AES-256-GCM)
1. **Key Generation:** On app launch, `CryptoManager` checks for an existing cryptographic key in the hardware-backed `AndroidKeyStore`. If not present, it generates an **AES-256-GCM** key with `KeyGenParameterSpec` (`PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`).
2. **Encryption at Rest:** `SecureApiKeyStorage` encrypts the API key at rest, persisting only the Base64-encoded initialization vector (IV) and ciphertext in private storage.
3. **In-Memory Decryption:** The key is decrypted into memory **only at the exact moment** the `GenerativeModel` is instantiated to perform a prompt request. The decrypted key is never logged, toasted, or exposed to the UI layer.
4. **CI/CD Environment Fallback:** In `app/build.gradle.kts`, the build system checks `local.properties` first, falling back to the `GEMINI_API_KEY` environment variable so CI runners can use repository secrets without local files:
   ```kotlin
   val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
       ?: System.getenv("GEMINI_API_KEY") ?: ""
   ```
5. **R8 Code Obfuscation:** The release buildType enables `isMinifyEnabled = true` with optimized ProGuard rules, obfuscating symbols and deterring reverse-engineering.

### Production Limits & Recommended Architecture
> [!IMPORTANT]
> **Client-Side Encryption Limits:**
> Client-side encryption raises the security bar against casual inspection, but determined reverse-engineers with rooted devices or memory inspectors can eventually intercept client-side keys.
> 
> **For production applications, always consider:**
> - **Backend Gateway / Proxy:** Route AI requests through your backend service (e.g. Cloud Functions, Cloud Run, or your API server) where API keys remain entirely server-side.
> - **Firebase App Check:** Protect your endpoints by verifying that incoming requests originate from your authentic, untampered mobile application.
> - **API Key Restrictions:** In Google Cloud Console, restrict API keys by package name, SHA-1 fingerprint, and specific Gemini API scopes.

---

## 3. Architecture & Features

This application follows Google's recommended Android Architecture guidelines:

- **Presentation Layer (Jetpack Compose + Material 3):**
  - **Conversational Bubble UI:** `LazyColumn` displays distinct user and Gemini bubbles with stable keys (`key = { it.id }`).
  - **Smooth Auto-Scroll:** Reactively scrolls to the bottom on new incoming messages and typing states using `animateScrollToItem()`.
  - **Markdown Rendering:** Formatted text rendering supporting `**bold**` markdown notation via `BoldMarkdown.kt`.
  - **State Hoisting:** Stateless composables driven by `StateFlow<ChatUiState>` collected with `collectAsStateWithLifecycle()`.
  - **Responsive Layout:** Responsive container (`Modifier.widthIn(max = 840.dp)`) adapts gracefully between phones, tablets, and landscape orientations.
  - **Dark & Light Theme:** Dynamic Material 3 theming with persistent System/Light/Dark mode runtime toggle.
  - **Voice Input (STT):** Speech-to-text input via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched with `rememberLauncherForActivityResult`.
  - **Model Selector:** Dynamic selection between `gemini-3.6-flash`, `gemini-3.6-pro`, `gemini-1.5-flash`, and `gemini-1.5-pro`.
- **Data & Persistence Layer:**
  - **Room Database:** Stores complete conversation history (`ChatMessageEntity`, `ChatMessageDao`, `AppDatabase`), ensuring past chats survive app process kills and device reboots.
  - **Preferences DataStore:** Persists user preferences such as the selected Gemini model.
  - **Coroutines & Flow:** Offloads all database and network interactions to background threads (`Dispatchers.IO`).

---

## 4. Running Tests

### Unit Tests
The project contains comprehensive unit tests for `ChatViewModel` using `kotlinx-coroutines-test` and test doubles (`FakeGeminiRepository`):
```bash
./gradlew testDebugUnitTest
```

### Compose UI Tests
Instrumented UI tests verify Compose elements, user interaction, bubble rendering, and loading indicators:
```bash
./gradlew connectedAndroidTest
```

---

## 5. Submission Checklist

- [x] API key is stored in `local.properties` (never committed).
- [x] `.gitignore` includes `local.properties`.
- [x] `local.properties.example` is provided for collaborators.
- [x] Android Keystore AES-256-GCM encrypts key at rest.
- [x] In-memory decryption only when calling GenerativeModel.
- [x] Environment variable fallback configured in Gradle for CI builds.
- [x] R8 minification enabled for release builds.
- [x] Jetpack Compose `LazyColumn` with stable keys and auto-scrolling.
- [x] Speech-to-text voice input via `RecognizerIntent`.
- [x] Room database conversation persistence across restarts.
- [x] Preferences DataStore for model customization.
- [x] Unit tests and Compose UI tests implemented.
