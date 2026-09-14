# Gemini API Jetpack Compose Starter (MAD Lab Assignment 1)

An Android chat application integrating **Google Gemini AI** (`gemini-3.6-flash`), built with **100% Jetpack Compose (Material 3)**, **Room Database**, **Preferences DataStore**, and **Android Keystore AES-256-GCM Encryption**.

---

## 🚀 Key Features

### 💬 Multi-Session Chat
- Start a **New Chat** anytime with the ✏️ button — each conversation is its own session.
- Swipe right (or tap the 🕐 History icon) to open the **Chat History Drawer** and switch between any past conversation.
- Delete the current chat with the 🗑️ button (with confirmation dialog).
- All sessions are persisted across app restarts via **Room Database**.

### 🎨 Modern Material 3 UI
- Dynamic **Light / Dark mode** toggle per user preference.
- `LazyColumn` conversation view with distinct user and Gemini chat bubbles.
- Auto-scrolls to the latest message on every response.
- Copy any response to clipboard with a single tap.
- Retry any prompt instantly with the Retry button.

### 🎙️ Voice Input
- Integrated **Speech-to-Text** using `RecognizerIntent` and `rememberLauncherForActivityResult`.
- Tap the 🎙️ mic icon in the input bar to speak your prompt.

### 💾 Persistence
- **Room Database (v2)**: Stores full chat history partitioned by session ID.
- **Preferences DataStore**: Persists user theme preference across sessions.

### 🔐 Security & Cryptography
- **API Key Security**: Stored in git-ignored `local.properties`; exposed at build time only via `BuildConfig.GEMINI_API_KEY`.
- **Android Keystore AES-256-GCM**: Every user prompt is encrypted before being written to Room DB; decrypted only on read.
- **R8 Code Obfuscation**: `isMinifyEnabled = true` and `isShrinkResources = true` for release builds.

---

## 📸 App Structure

```
GeminiApiComposeStarter/
├── data/
│   ├── GeminiRepository.kt          # Gemini API abstraction
│   ├── local/
│   │   ├── AppDatabase.kt           # Room database (v2)
│   │   ├── ChatMessageDao.kt        # Session-aware DAO queries
│   │   └── ChatMessageEntity.kt     # Chat message with sessionId
│   └── preferences/
│       └── UserPreferencesRepository.kt  # DataStore theme prefs
├── security/
│   └── KeystoreManager.kt           # AES-256-GCM encrypt/decrypt
└── ui/
    └── chat/
        ├── ChatScreen.kt            # Compose UI + History Drawer
        ├── ChatViewModel.kt         # State + session management
        └── ChatUiState.kt           # Immutable UI state model
```

---

## 🔑 Setup & Configuration

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Zeshawn-Martis/GeminiApiComposeStarter.git
   cd GeminiApiComposeStarter
   ```

2. **Configure Gemini API Key**:
   - Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
   - Open (or create) `local.properties` in the project root.
   - Add your key:
     ```properties
     GEMINI_API_KEY=your_actual_gemini_api_key_here
     ```

3. **Build & Run**:
   - Open in **Android Studio**.
   - Run on a physical device or emulator.

---

## 🔐 Security Architecture

### Local Key Protection
- The API key is injected at Gradle build time from `local.properties` into `BuildConfig` and never committed to source control.
- Prompts in the local Room database are encrypted at rest via **AES-256-GCM** keys generated inside the Android Keystore — keys never leave secure hardware.

### Production Best Practices
In a production deployment, client-side API keys carry inherent risks. Recommended safeguards:
1. **Backend Proxy Server** — Route Gemini API calls through a secure backend (e.g., Firebase Cloud Functions) so the key never lives in the APK.
2. **Firebase App Check** — Attest device and app integrity to block unauthorized clients.
3. **API Key Restrictions** — Restrict keys in Google AI Studio by Android package name and SHA-1 fingerprint.

---

## 🧪 Running Tests

### Unit Tests (ChatViewModel)
```bash
./gradlew test
```

### UI Tests (Compose — requires emulator/device)
```bash
./gradlew connectedAndroidTest
```

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| AI | Google Gemini API (`gemini-3.6-flash`) |
| Local DB | Room (v2) |
| Preferences | Jetpack DataStore |
| Encryption | Android Keystore AES-256-GCM |
| Voice | Android SpeechRecognizer |
| Build | Gradle KTS + KSP |
| Min SDK | API 26 (Android 8.0) |
