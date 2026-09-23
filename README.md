# Gemini API Compose Chat Application

**Student Information:**
- **Roll Number:** N078
- **GitHub Username:** [Rohit-Patil768](https://github.com/Rohit-Patil768)
- **Repository Fork:** [Rohit-Patil768/GeminiApiComposeStarter](https://github.com/Rohit-Patil768/GeminiApiComposeStarter)
- **Instructor Starter:** [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)
- **Branch:** `N078-assignment-1`

---

## 1. Project Overview

This project is a modern, responsive Android chat application built with **Jetpack Compose**, **Material 3**, and the **Google Generative AI SDK (Gemini)**. The application demonstrates an enterprise-grade mobile development architecture featuring hardware-backed API key encryption, persistent conversation storage with Room, user preferences with DataStore, speech recognition integration, and responsive layout constraints across phones and tablets.

### Key Implemented Features
- **Conversational AI Chat:** Interactive chat UI with Material 3 message bubbles, Markdown bold text rendering, thinking indicators, and automatic scroll-to-bottom.
- **Hardware-Backed Security:** Android Keystore AES-256-GCM encryption with fresh randomized IVs, storing only ciphertext in private preferences.
- **Room Database Persistence:** Full offline message history persistence across app process lifecycles.
- **DataStore Theme Preference:** Reactive light/dark theme toggle persisted via Jetpack Preferences DataStore.
- **System Speech Recognition:** Voice-to-text integration using `RecognizerIntent` and `rememberLauncherForActivityResult`, with no unnecessary microphone permissions requested.
- **Responsive Layout Design:** Adapts smoothly across phone portrait, phone landscape (compact height), and expanded tablet viewports (centered 840dp max content width).
- **R8 / ProGuard Optimization:** Full release minification and resource shrinking reducing APK size from 13.1 MB to 1.66 MB (87.3% reduction).

---

## 2. Environment & Prerequisites

The application was built and verified using the following toolchain:

| Component | Specification |
| :--- | :--- |
| **Android Studio** | Ladybug / Meerkat (Windows x64) |
| **Java Development Kit** | Android Studio Bundled JetBrains Runtime (**JBR OpenJDK 25.0.2**) |
| **Android SDK** | Compile SDK: `36`, Target SDK: `36`, Minimum SDK: `26` (Android 8.0 Oreo) |
| **Build Tools** | Android Gradle Plugin `9.0.0-alpha13`, Gradle Wrapper `9.3.1` |
| **Kotlin & KSP** | Kotlin `2.0.21`, KSP `2.0.21-1.0.28` |

> [!NOTE]
> When executing Gradle commands from PowerShell or terminal on machines where system Java is JDK 26, set `JAVA_HOME` to Android Studio's bundled JBR:
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
> .\gradlew.bat assembleDebug
> ```

---

## 3. Gemini API Key Configuration

The Gemini API key is managed securely and is **never committed to version control**.

### Step 1: Create `local.properties`
In the root directory of the project, create or edit `local.properties` (this file is excluded by `.gitignore`):

```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

A template file [local.properties.example](file:///C:/Users/sam12/Documents/antigravity/GeminiApiComposeStarter/local.properties.example) is provided in the repository with placeholder values.

### Step 2: Build Injection
In `app/build.gradle.kts`, the key is read from `local.properties` (or CI environment variables) and exposed securely via `BuildConfig.GEMINI_API_KEY`:

```kotlin
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY"))?.trim().orEmpty()

defaultConfig {
    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
}
```

If no key is supplied, the app builds normally and presents a graceful in-app notification asking the user to provide an API key.

---

## 4. Security Architecture

The app implements defense-in-depth protection for sensitive API credentials on Android:

```
[BuildConfig.GEMINI_API_KEY]
              │
              ▼
   [AesGcmCipher.encrypt]  ◄───  [KeystoreManager] (AES-256 Key in AndroidKeyStore)
              │
              ▼
   (Ciphertext + 12-byte IV)
              │
              ▼
[SecureApiKeyStorage] (Private SharedPreferences)
              │
   (On demand in-memory decryption only)
              ▼
    [GeminiRepositoryImpl]  ────►  [Gemini GenerativeModel]
```

1. **Hardware-Backed Keystore:** `KeystoreManager` generates an AES-256 secret key stored directly inside the `AndroidKeyStore` hardware security module (StrongBox or TEE where available).
2. **AES-256-GCM Encryption:** Authenticated encryption with 128-bit authentication tags providing confidentiality and cryptographic integrity.
3. **Randomized IV:** Every call to `AesGcmCipher.encrypt()` generates a fresh, cryptographically secure 12-byte IV via `SecureRandom`.
4. **Ciphertext-Only Persistence:** `SecureApiKeyStorage` writes only Base64-encoded ciphertext and IV into private `SharedPreferences` (`gemini_secure_storage`). Plaintext is never stored on disk or in the Room database.
5. **In-Memory Decryption:** `getDecryptedApiKey()` decrypts the key in-memory only when the repository initializes.
6. **No Leaks in Logs/UI:** No API key, decrypted secret, prompt, or sensitive payload is ever printed to Logcat or exposed in the UI.

### Realistic Security Limitation
> [!IMPORTANT]
> **Client-Side Key Limitation:** In any standalone client-side mobile application, an API key embedded or decrypted on device carries inherent exposure risk against sophisticated reverse-engineering or memory analysis on rooted devices. In enterprise production systems, API calls should be routed through a trusted backend proxy server with user authentication, rate limiting, and server-side secret management.

---

## 5. Architectural Components & Features

### A. Gemini Chat Architecture
- **`ChatMessage`**: Domain model representing chat messages with unique UUIDs, text content, participant roles (`USER`, `MODEL`), and UTC millisecond timestamps.
- **`ChatUiState`**: Immutable UI state exposing the message list, current prompt, loading flag, prompt validation error, transient error messages, and dark theme state.
- **`ChatViewModel`**: State holder managing user inputs, input validation (rejecting whitespace-only submissions), Room database observation, theme toggle, and background IO execution via `Dispatchers.IO`.

### B. Room Database Persistence
- **Entity (`ChatMessageEntity`)**: Persists `id` (primary key), `text`, `participant`, and `timestamp`.
- **DAO (`ChatMessageDao`)**: Exposes `getAllMessages(): Flow<List<ChatMessageEntity>>` ordered chronologically, along with `insertMessage()` and `clearAll()`.
- **Database (`ChatDatabase`)**: Room database singleton providing migration safety and off-thread transactional execution. Conversation history is automatically restored on app startup.

### C. Preferences DataStore & Theme Toggle
- **`ThemePreferencesRepository`**: Interface backed by `ThemePreferencesRepositoryImpl` using AndroidX DataStore Preferences (`theme_preferences`).
- **Persistence**: Persists the user's explicit theme choice across app cold starts.
- **UI Control**: TopAppBar action button (`testTag("theme_toggle_button")`) dynamically renders a sun (`ic_light_mode`) or moon (`ic_dark_mode`) icon and toggles theme state immediately.

### D. Voice Input Integration
- **Approach**: Uses Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched via `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- **Workflow**: Tapping the microphone button (`testTag("mic_button")`) opens the system speech recognition overlay. When speech is recognized, the string is populated directly into the prompt text field.
- **User Control**: The recognized text is **not automatically sent**. The user can inspect, edit, or append to the text before tapping Send.
- **Permission Safety**: **No `RECORD_AUDIO` permission is requested.** The system speech recognizer activity handles its own audio permissions safely.
- **Graceful Error Handling**: If speech recognition is unavailable or fails, `ActivityNotFoundException` is caught and a non-intrusive Snackbar message is displayed without crashing.

### E. Responsive UI
Built using Compose `BoxWithConstraints`:
- **Portrait Phone:** Full-width conversation layout with standard padding (16dp) and bubble width up to 320dp.
- **Landscape Phone:** Detects compact vertical height (`maxHeight < 480.dp`). The prompt bar reduces vertical padding to 4dp with a maximum of 2 lines, and the empty state uses compact icons and spacing to preserve vertical reading space.
- **Tablet / Large Screens:** Content is bounded by a maximum content width of 840dp (`widthIn(max = 840.dp)`) and centered horizontally (`Alignment.CenterHorizontally`). Message bubbles comfortably expand up to 520dp without stretching across the entire screen.

---

## 6. R8 & Release Configuration

The release variant is configured in `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("debug")
    }
}
```

- **R8 Minification:** Enabled (`isMinifyEnabled = true`). Strips unused classes and obfuscates code.
- **Resource Shrinking:** Enabled (`isShrinkResources = true`). Strips unused resources.
- **ProGuard Rules:** Standard rules from `proguard-android-optimize.txt` and `app/proguard-rules.pro`. No broad wildcard keep rules (`-keep class **`) were added.
- **APK Size Optimization Results:**
  - `app-debug.apk`: **13.1 MB** (13,738,893 bytes)
  - `app-release.apk`: **1.66 MB** (1,739,951 bytes) — **87.3% reduction**
- **Signing Note:** Debug signing is used to permit local generation and testing of the release APK without publishing real release credentials.

---

## 7. Verification & Testing

### Automated Unit Tests
Command:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest
```
- **Total: 19/19 unit tests passed** (0 failures, 0 errors):
  - `AesGcmCipherTest` (3 tests): Encryption/decryption roundtrip, random IV generation uniqueness, corruption detection.
  - `ChatViewModelTest` (16 tests): Prompt handling, blank submission rejection, API key validation, message ordering, error handling, Room preloading, Room insertion, failed response isolation, theme preferences default/loading/toggling, voice result insertion, blank voice rejection, and voice error handling.

### Compose UI Tests
Automated Compose UI tests are implemented in [ChatScreenTest.kt](file:///C:/Users/sam12/Documents/antigravity/GeminiApiComposeStarter/app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ChatScreenTest.kt) using `createComposeRule()`:
- `chatScreen_rendersEmptyStateInitially`
- `chatScreen_displaysMessagesCorrectly`
- `chatScreen_sendButton_triggersOnSend`
- `chatScreen_promptInput_triggersOnPromptChange`
- `chatScreen_displaysErrorStateWhenPromptEmpty`
- `chatScreen_displaysLoadingIndicatorWhenLoading`
- `chatScreen_themeToggleButton_triggersOnToggleTheme`
- `chatScreen_micButton_isDisplayedAndHasCorrectContentDescription`
- `chatScreen_micButton_triggersOnVoiceInputClick`
- `chatScreen_voiceResult_placesRecognizedTextIntoPromptField`
- `chatScreen_portraitPhone_rendersAllComponents` (360dp constraint)
- `chatScreen_landscapePhone_rendersAllComponentsWithoutClipping` (720x360dp constraint)
- `chatScreen_tabletWideScreen_rendersAllComponentsWithReadableConstraints` (1024x768dp constraint)

Both `assembleDebug` and `assembleDebugAndroidTest` compile cleanly.

---

## 8. Known Limitations & Disclosures

1. **Hardware Testing Disclosure:** No physical Android device or active Android emulator was attached during development. Consequently, while all Android instrumentation test APKs compiled cleanly (`assembleDebugAndroidTest`), connected device execution (`connectedAndroidTest`) and physical speech recognition testing through a microphone were not manually exercised on hardware.
2. **Client-Side Secret Scope:** As documented in the Security Architecture section, client-side API key encryption protects credentials against casual extraction from static storage, but production architectures should always terminate LLM API requests on an authenticated proxy server.
