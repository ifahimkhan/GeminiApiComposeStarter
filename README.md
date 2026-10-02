# GeminiApiComposeStarter

A modern, production-grade Android starter application demonstrating how to integrate the **Google Gemini API** with **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room Persistence**, **Jetpack DataStore**, and **Hardware-Backed Android KeyStore Encryption**.

---

## Architecture & Key Features

- **Modern Jetpack Compose & Material 3**:
  - Stateless composables following state-hoisting best practices.
  - Chat interface using `LazyColumn` with stable keys (`key = { it.id }`) and distinct visual styling for User vs. Gemini assistant bubble cards.
  - Automatic scrolling to the latest message using `rememberLazyListState()` and `LaunchedEffect(messages.size)`.
  - Comprehensive loading state feedback with `CircularProgressIndicator` and error reporting via `SnackbarHost`.
  - Responsive adaptive layout scaling cleanly across phones, tablets, foldables, and landscape orientations.
  - Full Material 3 dynamic theming with dark and light mode support.
- **Voice Input (Speech-to-Text)**:
  - Microphone action button integrated via `rememberLauncherForActivityResult` using `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` to automatically populate prompts.
- **Local Persistence (Room Database)**:
  - Offline chat history persisted across app restarts via `AppDatabase`, `ChatMessageEntity`, and `ChatDao`.
  - Real-time reactive updates exposed as a Kotlin `Flow<List<ChatMessageEntity>>` to `ChatViewModel`.
- **User Preferences (Jetpack DataStore)**:
  - Persistent user preferences (user name, custom system instructions/persona) managed reactively through `PreferencesRepository`.
- **Hardware-Backed Encryption at Rest (Android KeyStore)**:
  - AES-256-GCM encryption at rest for the API key.
  - Plaintext key is never logged, leaked, or serialized. Decryption occurs strictly in-memory at the moment `GenerativeModel` is instantiated.

---

## 1. Supplying the API Key

To use the Google Gemini API, you need an API key from [Google AI Studio](https://aistudio.google.com/).

### Option A: `local.properties` (Recommended for Local Development)
1. Copy the provided template file:
   ```bash
   cp local.properties.example local.properties
   ```
2. Open `local.properties` and add your API key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
   *(Note: `local.properties` is excluded from version control via `.gitignore`.)*

### Option B: Environment Variable (CI/CD Fallback)
The build configuration in `app/build.gradle.kts` automatically falls back to the system environment variable if not present in `local.properties`:
```bash
export GEMINI_API_KEY="your_actual_gemini_api_key_here"
```

In `app/build.gradle.kts`, the key is resolved safely as follows:
```kotlin
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY") ?: ""
```

---

## 2. Hardware-Backed Encryption at Rest (AES-256-GCM)

To adhere to the principle of defense-in-depth on the client device, the application implements hardware-backed encryption at rest using the **Android KeyStore provider** and **AES-256 in Galois/Counter Mode (GCM)**:

### How the Flow Operates:

```
[BuildConfig / Initial Key]
          │
          ▼
┌───────────────────────────────┐
│     SecurityManager.kt        │
│  - AndroidKeyStore Provider   │
│  - AES-256-GCM SecretKey      │
│  - Generates 12-byte IV       │
└──────────────┬────────────────┘
               │  (Encrypts plain key)
               ▼
┌───────────────────────────────┐
│       Jetpack DataStore       │
│ Stores ONLY:                  │
│  • Base64 Ciphertext          │
│  • Base64 Initialization (IV) │
│ (Plaintext key is discarded)  │
└──────────────┬────────────────┘
               │
               │ (On demand in GeminiRepositoryImpl)
               ▼
┌───────────────────────────────┐
│ In-Memory Decryption Only     │
│  • Load IV and Ciphertext     │
│  • KeyStore decrypts to RAM   │
│  • GenerativeModel created    │
└───────────────────────────────┘
```

1. **Key Generation**:
   `SecurityManager` generates an AES-256 master key inside `AndroidKeyStore` with `KeyGenParameterSpec`:
   - Purpose: `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`
   - Block Mode: `BLOCK_MODE_GCM`
   - Padding: `ENCRYPTION_PADDING_NONE`
   - Key Size: `256 bits`
   - Key material remains securely protected inside hardware (TEE / StrongBox) and cannot be exported.
2. **Encryption on First Launch**:
   On first launch, `SecurityManager.ensureApiKeyStored()` takes the API key, initializes a cipher with `AES/GCM/NoPadding`, generates an authenticated 12-byte initialization vector (IV), encrypts the key, and writes **only** the IV and ciphertext to Jetpack DataStore. The plaintext is cleared from memory.
3. **Lazy In-Memory Decryption**:
   The key is decrypted only when `GeminiRepositoryImpl.generateText(...)` is called to instantiate the `GenerativeModel`. The plaintext key is never written to disk, logged, or exposed across UI state layers.

---

## 3. Production Security Limitations: Why Client-Side Security Is Not Enough

While KeyStore AES-256-GCM encryption provides strong protection against casual offline file extraction from rooted devices or backups, **no client-side application can completely hide a secret from an attacker who controls the hardware**.

### Limitations of Client-Side API Keys:
1. **Reverse Engineering & Memory Inspection**:
   Attackers can attach debuggers (e.g. Frida, lldb) or perform runtime hooking on rooted devices or emulators to dump decrypted keys directly from process RAM when `GenerativeModel` is instantiated.
2. **APK Decompilation**:
   Keys hardcoded into `BuildConfig` or build artifacts can be inspected with tools like `jadx` or `apktool` unless strictly supplied via server-side mediation.
3. **Lack of Rate Limiting & Revocation Control**:
   A leaked client-side key allows unauthorized third parties to drain API quotas or incur charges without your knowledge.

### Recommended Production Architecture:
For production-grade mobile applications, you should **never call LLM APIs directly from the client with a shared master API key**. Instead, employ:

1. **Backend API Proxy / Gateway**:
   - The Android client authenticates against your own backend server (e.g., via OAuth2, Firebase Auth).
   - Your backend holds the Gemini API key securely in secrets management (e.g. Google Cloud Secret Manager).
   - The backend validates requests, enforces rate limits, logs usage, and invokes the Gemini API on behalf of the user.
2. **Firebase App Check / Play Integrity API**:
   - Use Google Play Integrity API or Firebase App Check to attest that incoming requests originate from your genuine, unmodified app installed on a certified Android device.
   - Unauthorized requests or compromised devices are blocked before reaching your backend or API quotas.

---

## 4. Running the Test Suites

The project includes both JVM Unit Tests and Android Instrumentation UI tests.

### Running Unit Tests (JVM)
The unit test suite tests `ChatViewModel`, state flows, Room persistence logic, preferences, and error handling using `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`) and test fakes:

```bash
# On Windows
.\gradlew.bat testDebugUnitTest

# On macOS / Linux
./gradlew testDebugUnitTest
```

HTML test reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

### Running Compose UI Tests (Android Instrumentation)
The instrumented UI test suite verifies message rendering, Material 3 bubble card styling, loading indicators, empty greetings, and input interactions using `createComposeRule()`:

1. Connect a physical Android device with USB debugging enabled, or start an Android Emulator.
2. Execute the test task:
```bash
# On Windows
.\gradlew.bat connectedDebugAndroidTest

# On macOS / Linux
./gradlew connectedDebugAndroidTest
```

HTML instrumented test reports are generated at:
```
app/build/reports/androidTests/connected/index.html
```

### Building the Debug APK
```bash
# On Windows
.\gradlew.bat assembleDebug

# On macOS / Linux
./gradlew assembleDebug
```
The resulting APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

### Building the Release APK (Minification & ProGuard Enabled)
```bash
# On Windows
.\gradlew.bat assembleRelease

# On macOS / Linux
./gradlew assembleRelease
```
The release build type has `isMinifyEnabled = true` with standard Android R8/ProGuard optimization rules configured in `app/proguard-rules.pro`.
