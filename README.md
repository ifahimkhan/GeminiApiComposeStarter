# MAD Assignment 1 — Gemini AI Chat App
### N035 · Anish Hegde

A fully functional Android chat application powered by the **Google Gemini API**, built with Kotlin, Jetpack Compose, and Material 3.

---

## ✨ Features

| Feature | Details |
|---|---|
| **Gemini Chat** | Multi-turn conversation via Gemini API |
| **Chat Bubbles** | Material 3 user/AI bubbles with LazyColumn |
| **Chat History** | Room database — messages persist across restarts |
| **Voice Input** | Microphone button via `RecognizerIntent` (speech-to-text) |
| **Dark Mode** | System dark mode + manual toggle (persisted in DataStore) |
| **Error Handling** | Snackbar error feedback, loading indicator |
| **Unit Tests** | `ChatViewModelTest` with fake repository and DAO |

---

## 🔑 How to Configure the API Key

**Step 1 — Create `local.properties`** in the project root (same folder as `local.properties.example`):

```
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

> ⚠️ **`local.properties` is git-ignored and must NEVER be committed.** It is listed in `.gitignore`.

**Step 2 — Get your API key** from [Google AI Studio](https://aistudio.google.com/app/apikey).

**Step 3 — Build the app.** The key is read at build time into `BuildConfig.GEMINI_API_KEY` and is never stored in source files.

---

## 🏃 How to Run

```bash
# Build a debug APK
./gradlew assembleDebug

# Install and run on connected device/emulator
./gradlew installDebug
```

---

## 🧪 How to Run Tests

```bash
# Unit tests (ViewModel, Repository logic)
./gradlew test

# Instrumented UI tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

---

## 🏗️ Architecture

```
├── data/
│   ├── GeminiRepository.kt         # Interface for AI calls
│   ├── GeminiRepositoryImpl.kt     # Gemini SDK implementation
│   └── db/
│       ├── ChatMessageEntity.kt    # Room entity
│       ├── ChatDao.kt              # Room DAO
│       └── AppDatabase.kt          # Room database
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt           # Compose UI (stateless)
│   │   ├── ChatViewModel.kt        # StateFlow-based ViewModel
│   │   └── ChatUiState.kt          # Immutable UI state model
│   └── theme/                      # Material 3 theming
└── MainActivity.kt                 # Entry point, DI wiring
```

---

## 🔒 Security Notes

### How the API Key is Protected
- The key is read from `local.properties` (gitignored) at **build time** into `BuildConfig.GEMINI_API_KEY`.
- It is never hardcoded in Kotlin, XML, or committed Gradle files.
- For CI/CD, the build script also falls back to a `GEMINI_API_KEY` **environment variable**.
- **R8/minification is enabled for release builds** to obfuscate the compiled code.

### ⚠️ Important Limitation
> **Client-side API key protection is fundamentally limited.** Any key embedded in an app binary — even obfuscated — can be extracted by a determined attacker.
>
> **For production apps, you should:**
> - Proxy all Gemini API calls through your own **backend server** (never expose the key to the client)
> - Use **Firebase App Check** to verify your app's integrity before allowing API access
> - Use **restricted API keys** scoped to specific APIs/referrers in Google Cloud Console
> - Never commit secrets to version control

---

## 📁 Important Files

| File | Purpose |
|---|---|
| `local.properties` | **Gitignored** — put your `sdk.dir` and `GEMINI_API_KEY` here |
| `local.properties.example` | Template — shows the format expected |
| `.gitignore` | Ensures `local.properties` is never committed |

---

## 📦 Dependencies

- Jetpack Compose + Material 3
- Google Generative AI SDK (`gemini-3.6-flash`)
- Room (persistence)
- DataStore Preferences (settings)
- AndroidX Lifecycle / ViewModel
- KSP (annotation processing)
