# Gemini API Compose Starter

A production-grade Android chat application demonstrating modern **Jetpack Compose**, **Google Gemini API** integration, **Android Keystore AES-256-GCM** hardware-backed encryption at rest, **Room Database** persistence, **Preferences DataStore**, and **Speech-to-Text Voice Input**.

---

## Features

- 💬 **Conversational Chat UI**:
  - `LazyColumn` of Material 3 chat bubbles distinguishing User (trailing end, primaryContainer) and Gemini (leading start, secondaryContainer).
  - Smooth auto-scrolling to the latest message on new entries or during generation.
  - Formatted text rendering with markdown bolding (`**text**`).
- 🎙️ **Voice Input (Speech-to-Text)**:
  - Hands-free voice querying powered by `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched via `rememberLauncherForActivityResult`.
- 🗄️ **Persistent Chat History (Room Database)**:
  - Past conversations are saved to local SQLite via Room (`ChatMessageDao`, `AppDatabase`), surviving app restarts and process death.
  - Ability to clear history with one tap from the top bar.
- ⚙️ **Tailored User Experience (Preferences DataStore)**:
  - Custom AI Tone / Persona selection (**Balanced**, **Concise**, **Creative**, **Technical**) persisted in DataStore and injected dynamically into Gemini requests.
- 📐 **Responsive & Adaptive Layouts**:
  - Uses `WindowSizeClass` to adapt between compact screens (phones) and expanded screens (tablets & landscape), centering conversation width for maximum readability.
- 🎨 **Material 3 & Dark Theme**:
  - Dynamic color support on Android 12+ and system dark theme support via `isSystemInDarkTheme()`.
- 🛡️ **End-to-End API Key Security**:
  - Keystore AES-256-GCM encryption at rest on first launch.
  - Zero plaintext key exposure in VCS, logs, toasts, or UI.
  - Obfuscated release builds with R8 minification enabled.

---

## 1. Obtaining and Storing the Gemini API Key

### Step 1: Obtain a Key
1. Visit [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account and click **Get API key**.
3. Create an API key in a new or existing Google Cloud project.

### Step 2: Store the Key Locally
1. In the project root, open (or create) the `local.properties` file. This file is ignored by Git (`.gitignore`) and will **never** be committed to version control.
2. Add your key:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
3. A template file [`local.properties.example`](local.properties.example) is provided for reference:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

### CI/CD Environment Fallback
In `app/build.gradle.kts`, the build system checks `local.properties` first and falls back to the environment variable `GEMINI_API_KEY` for CI/CD runners:
```kotlin
val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY") ?: "").trim()
```

---

## 2. API Key Security & Encryption Architecture

### Encryption at Rest (Android Keystore + DataStore)
Storing API keys in cleartext inside SharedPreferences or files leaves them vulnerable on rooted devices or via device backups. This app enforces **hardware-backed encryption at rest**:
1. **Key Generation**: On first launch, [`CryptoManager`](app/src/main/java/com/fahim/geminiApiComposeStarter/security/CryptoManager.kt) generates an AES-256 key inside the `AndroidKeyStore` provider using `KeyGenParameterSpec`:
   - Algorithm: `AES`
   - Block Mode: `GCM` (Galois/Counter Mode)
   - Padding: `NoPadding`
   - Key Size: 256 bits
2. **Ciphertext Storage**: The key from `BuildConfig.GEMINI_API_KEY` is encrypted with AES-256-GCM. Only the **Base64 ciphertext** and the **Initialization Vector (IV)** are persisted in Preferences DataStore (`secure_gemini_prefs`).
3. **In-Memory Decryption**: Decryption occurs strictly in-memory at the moment the `GenerativeModel` is invoked inside [`GeminiRepositoryImpl`](app/src/main/java/com/fahim/geminiApiComposeStarter/data/GeminiRepositoryImpl.kt).
4. **No Plaintext Logging**: Decrypted strings are never logged to Logcat, toasted, or written to disk.

### Code Obfuscation (R8)
In `app/build.gradle.kts`, release builds have R8 minification enabled:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```
This strips unused code, renames classes and identifiers, and prevents trivial reverse-engineering of decompiled APKs.

### Limits of Client-Side Storage & Production Best Practices
> [!WARNING]
> While client-side Keystore encryption significantly raises the bar against local extractors, **no client-side application can completely hide a secret from an attacker with full root, Frida, or memory inspection access**.

For a commercial or enterprise production app, recommended architectures include:
1. **Backend Proxy Service**:
   - Move Gemini API calls behind an authenticated backend server (e.g., Google Cloud Functions, Cloud Run, or custom backend).
   - The mobile client authenticates with the backend using Firebase Auth or OAuth tokens.
   - The backend holds the Gemini API key securely in Secret Manager and forwards requests.
2. **Firebase App Check**:
   - Attest that incoming requests originate from your genuine, unmodified app on a certified device using Play Integrity.
3. **API Key Restrictions**:
   - Apply API restrictions in the Google Cloud Console to restrict key usage strictly to designated APIs and quotas.

---

## 3. Architecture & Components

```
com.fahim.geminiApiComposeStarter
├── MainActivity.kt                  # Entry point, WindowSizeClass, dependencies wiring
├── security
│   ├── CryptoManager.kt             # Android Keystore AES-256-GCM engine
│   └── SecureKeyStorage.kt          # Encrypted ciphertext storage in DataStore
├── data
│   ├── ChatMessage.kt               # Domain model (User vs Gemini)
│   ├── ChatHistoryRepository.kt     # Repository for conversation history
│   ├── GeminiRepository.kt          # Interface for Gemini API
│   ├── GeminiRepositoryImpl.kt      # GenerativeModel execution with in-memory key
│   ├── local
│   │   ├── AppDatabase.kt           # Room Database
│   │   ├── ChatMessageDao.kt        # Room DAO with reactive Flow queries
│   │   └── ChatMessageEntity.kt     # SQLite entity mapping
│   └── preferences
│       └── UserPreferencesRepository.kt # Preferences DataStore for AI tone/persona
└── ui
    ├── chat
    │   ├── ChatScreen.kt            # Compose UI: LazyColumn bubbles, voice bar, top bar
    │   ├── ChatUiState.kt           # Immutable UI state
    │   └── ChatViewModel.kt         # StateFlow view model with error & loading handling
    ├── text
    │   └── BoldMarkdown.kt          # Markdown bold parser
    └── theme
        ├── Color.kt
        ├── Theme.kt                 # Material 3 dynamic & dark theme
        └── Type.kt
```

---

## 4. Running the App and Tests

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 or JDK 21
- Android SDK Platform 36 (or configured SDK)

### Run the App on Emulator
1. Open the project in Android Studio.
2. Ensure an Android Virtual Device (AVD) is running (e.g. Pixel 7 with API 34+).
3. Click **Run > Run 'app'** or execute:
   ```bash
   ./gradlew installDebug
   ```

### Run Unit Tests
Unit tests verify `ChatViewModel`, state flows, error handling, voice inputs, and Room/DataStore interactions using `kotlinx-coroutines-test`:
```bash
./gradlew testDebugUnitTest
```

### Run Instrumented Compose UI Tests
Compose UI tests verify `ChatScreen` rendering, chat bubbles, empty states, voice triggers, and loading states using `createComposeRule()`:
```bash
./gradlew connectedDebugAndroidTest
```
*(Ensure an emulator or physical device is connected).*

### Build Release APK
Verify R8 minification and release packaging:
```bash
./gradlew assembleRelease
```
