# N149 · Gemini Chat — LAB Assignment 1

> **Roll Number:** N149  
> **Branch:** `N149-gemini-chat-implementation`  
> **Base repo:** [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## 📱 Features Implemented

| # | Requirement | Status |
|---|---|---|
| 1 | Gemini API key secured via Android Keystore (AES-256-GCM) | ✅ |
| 2 | Key never hardcoded – read from `local.properties` / env var | ✅ |
| 3 | CI/CD fallback via `System.getenv("GEMINI_API_KEY")` | ✅ |
| 4 | `local.properties` git-ignored; `.example` placeholder committed | ✅ |
| 5 | R8 minification enabled for release builds | ✅ |
| 6 | LazyColumn chat bubbles (user / Gemini) with stable keys | ✅ |
| 7 | Auto-scroll to latest message | ✅ |
| 8 | State hoisting – `ChatUiState` StateFlow, `collectAsStateWithLifecycle()` | ✅ |
| 9 | Responsive layout with `WindowSizeClass` (phone vs tablet) | ✅ |
| 10 | Loading indicator (`CircularProgressIndicator`) | ✅ |
| 11 | Error state shown in Snackbar + error card | ✅ |
| 12 | Dark mode via `isSystemInDarkTheme()` + Material 3 dynamic colour | ✅ |
| 13 | Voice input with `RecognizerIntent` + `rememberLauncherForActivityResult` | ✅ |
| 14 | User preferences persisted in Preferences DataStore | ✅ |
| 15 | Chat history persisted in Room (survives restarts) | ✅ |
| 16 | Unit tests – `ChatViewModelTest` with MockK + coroutines-test | ✅ |
| 17 | UI tests – `ChatScreenTest` with `createComposeRule()` + Hilt | ✅ |
| 18 | Hilt dependency injection throughout | ✅ |

---

## 🔑 API Key Setup

### Step 1 – Get a key
Visit [Google AI Studio](https://ai.google.dev/gemini-api/docs/quickstart) and generate a Gemini API key.

### Step 2 – Add to local.properties
```
# In <project-root>/local.properties  (git-ignored)
GEMINI_API_KEY=AIzaSy...your_actual_key...
```

> **Never** paste the key into any Kotlin file, `strings.xml`, or `build.gradle.kts`.  
> `local.properties` is already in `.gitignore`. Before every commit, run  
> `git status` and confirm the file is **not** staged.

### Step 3 – CI/CD
Set a repository secret `GEMINI_API_KEY` in GitHub Actions. The Gradle build reads it automatically:
```kotlin
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY") ?: ""
```

---

## 🔐 Encryption Flow

```
First Launch
    │
    ├─ BuildConfig.GEMINI_API_KEY (from local.properties, via Gradle)
    │        │
    │   KeystoreManager.encryptAndStore(plaintext)
    │        │
    │   ┌────▼──────────────────────────────────────┐
    │   │  Android Keystore                          │
    │   │  AES-256-GCM key (hardware-backed on      │
    │   │  supported devices, never leaves TEE)      │
    │   └────────────────────────────────────────────┘
    │        │  Encrypt
    │        ▼
    │   SharedPreferences: ciphertext + IV (base64)
    │
Subsequent Launches
    │
    ├─ KeystoreManager.decryptApiKey()  [in-memory only]
    │        │
    │   Decrypt with Keystore key
    │        │
    │        ▼
    │   GenerativeModel(apiKey = <in-memory plaintext>)
    │        │
    │   API call → response
    │        │
    │   plaintext GC'd; never logged/stored/toasted
```

### Production Considerations

> **Client-side encryption raises the bar, but cannot fully hide a key from a determined
> attacker on a rooted device.** The key is decryptable by anyone with root access and the
> ability to interact with the Keystore on that device.
>
> In a production app, you should:
> 1. **Backend proxy** – Never put the Gemini key in the APK at all. Route all calls through
>    your own server (Cloud Functions, Cloud Run, etc.) that holds the key server-side.
> 2. **Firebase App Check** – Attest that requests come from your genuine, unmodified APK.
>    Combine with a restricted API key (allowed callers = your app's SHA-256 fingerprint).
> 3. **Restricted API keys** – In Google Cloud Console, restrict the key to specific Android apps
>    by package name + SHA-256 certificate fingerprint so it is useless if leaked.

---

## 🏗️ Architecture

```
MainActivity
    └── ChatScreen (Composable, stateless)
            ├── ChatViewModel (StateFlow, viewModelScope)
            │       ├── GeminiRepository (Gemini API, IO dispatcher)
            │       ├── MessageDao (Room – chat history)
            │       └── UserPreferencesRepository (DataStore)
            └── KeystoreManager (Android Keystore / AES-256-GCM)
```

- **Hilt** wires the dependency graph.
- **StateFlow + collectAsStateWithLifecycle()** for lifecycle-safe state.
- **Room** persists messages across restarts.
- **DataStore** persists user preferences (dark mode, model name, system prompt).
- **WindowSizeClass** adapts layout for phones and tablets.

---

## 🧪 Running Tests

```bash
# Unit tests (JVM)
./gradlew test

# Instrumentation / UI tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

Key test files:
- [`ChatViewModelTest.kt`](app/src/test/java/com/n149/geminichat/ChatViewModelTest.kt) – 8 unit tests with MockK & coroutines-test
- [`ChatScreenTest.kt`](app/src/androidTest/java/com/n149/geminichat/ChatScreenTest.kt) – 6 Compose UI tests with Hilt

---

## 🚀 Building & Running

```bash
# Debug build
./gradlew assembleDebug

# Release build (R8 minification enabled)
./gradlew assembleRelease
```

Open in Android Studio **Ladybug (2024.2)** or newer. The project targets **API 35** with `minSdk = 26`.

---

## 📂 Project Structure

```
N149-GeminiChat/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/n149/geminichat/
│   │   │   │   ├── GeminiChatApplication.kt   # Hilt entry point
│   │   │   │   ├── MainActivity.kt            # Single activity
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppModule.kt           # Hilt DI module
│   │   │   │   │   ├── ChatDatabase.kt        # Room DB
│   │   │   │   │   ├── GeminiRepository.kt    # Gemini API calls
│   │   │   │   │   ├── KeystoreManager.kt     # AES-256-GCM encryption
│   │   │   │   │   ├── MessageDao.kt          # Room DAO
│   │   │   │   │   ├── MessageEntity.kt       # Room entity
│   │   │   │   │   └── UserPreferencesRepository.kt  # DataStore
│   │   │   │   └── ui/
│   │   │   │       ├── chat/
│   │   │   │       │   ├── ChatScreen.kt      # Composable UI
│   │   │   │       │   ├── ChatUiState.kt     # State data class
│   │   │   │       │   └── ChatViewModel.kt   # ViewModel
│   │   │   │       └── theme/
│   │   │   │           └── Theme.kt           # Material3 + dark mode
│   │   ├── test/   ChatViewModelTest.kt
│   │   └── androidTest/  ChatScreenTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/libs.versions.toml
├── local.properties.example   ← commit this; NOT local.properties
├── .gitignore
└── README.md
```

---

## ✅ Submission Checklist

- [x] Branch name starts with roll number: `N149-gemini-chat-implementation`
- [x] No API key or `local.properties` in any commit
- [x] `local.properties.example` committed with placeholder
- [x] README explains key placement, encryption flow, and test instructions
- [x] R8 minification enabled for release
- [x] All features from the assignment spec implemented
