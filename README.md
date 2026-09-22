# Gemini Jetpack Compose Chat App
### LAB Assignment-1 — Mobile Application Development (702AI0E002)
**Student:** Abhay Telrandhe | **Roll No:** N104 | **Branch:** `N104-enhanced-gemini-chat`

---

## Features Implemented

| # | Feature | Assignment Section |
|---|---|---|
| 1 | Gemini API key via `local.properties` → `BuildConfig` | §3 |
| 2 | AES-256-GCM Android Keystore encryption | §3 |
| 3 | Environment variable fallback for CI | §3 |
| 4 | `isMinifyEnabled = true` + R8 obfuscation | §3 |
| 5 | Room database — full chat history persists across restarts | §5 |
| 6 | Preferences DataStore — user name + session timestamp | §5 |
| 7 | Multi-turn `ChatSession` (conversation context) | §5 |
| 8 | `LazyColumn` with stable-key chat bubbles | §4 |
| 9 | Voice input via `RecognizerIntent` | §5 |
| 10 | `WindowSizeClass` adaptive layout (phone / tablet) | §4 |
| 11 | Animated typing indicator | §4 |
| 12 | `CircularProgressIndicator` + Snackbar error handling | §4 |
| 13 | Dark mode with `isSystemInDarkTheme()` + dynamic colour | §4 |
| 14 | Unit tests — `ChatViewModelTest` with fake repository | §6 |
| 15 | Compose UI tests — `ChatScreenTest` with `createComposeRule()` | §6 |

---

## Setup — API Key

1. Copy `local.properties.example` to `local.properties` (same project root directory):
   ```
   cp local.properties.example local.properties
   ```
2. Open `local.properties` and replace the placeholder:
   ```
   GEMINI_API_KEY=your_actual_key_here
   ```
3. Get a free key at: https://aistudio.google.com/app/apikey
4. **Never commit `local.properties`** — it is listed in `.gitignore`.

> **CI / GitHub Actions**: Add `GEMINI_API_KEY` as a repository secret. The `build.gradle.kts` falls back to `System.getenv("GEMINI_API_KEY")` automatically when `local.properties` has no entry.

---

## API Key Encryption Flow

```
local.properties
      │
      ▼  (Gradle build)
BuildConfig.GEMINI_API_KEY   ← plaintext only in the compiled APK
      │
      ▼  (first app launch – KeystoreEncryption.kt)
Android Keystore ──► generates AES-256-GCM MasterKey (never leaves hardware)
      │
      ▼
EncryptedSharedPreferences ──► stores AES-256-GCM ciphertext only
      │
      ▼  (every subsequent launch)
Decrypt in memory  ──►  passed directly to GenerativeModel constructor
      │                  (never stored in a field, never logged)
      ▼
GeminiRepositoryImpl
```

**Key points:**
- The hardware-backed `MasterKey` is generated once and lives in the Android Keystore — it never leaves the secure element.
- Only the **ciphertext** is written to disk (via `EncryptedSharedPreferences`).
- The decrypted string exists only as a local variable inside `KeystoreEncryption.getDecryptedApiKey()` and is passed immediately to the `GenerativeModel`. It is never assigned to a class field or logged.
- `isMinifyEnabled = true` + R8 obfuscates the release APK, making the key harder to extract via decompilation.

### Production Recommendation

> Client-side encryption raises the bar but **cannot fully hide a key from a determined attacker** who has root access to the device.
>
> In a production app you would:
> 1. **Server-side proxy** — Move Gemini API calls to a backend (e.g. Cloud Functions). The client sends requests to _your_ server; the server holds the key and calls Gemini. The key never reaches the client.
> 2. **Firebase App Check** — Verify that requests come from your genuine app binary. Use this together with restricted API keys (IP/referrer restrictions in Google Cloud Console).

---

## Running Tests

### Unit Tests (JVM — no emulator needed)
```bash
./gradlew :app:test
```

### Compose UI Tests (requires a connected emulator or device)
```bash
./gradlew :app:connectedAndroidTest
```

### Verify no secrets in git history
```bash
git log -p | grep -i "GEMINI_API_KEY"
# Should return nothing except the BuildConfig field definition — no literal key values.
```

---

## Project Structure

```
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt                  # Entry point; wires Keystore + Room + DataStore
├── security/
│   └── KeystoreEncryption.kt        # AES-256-GCM key encryption/decryption
├── data/
│   ├── GeminiRepository.kt          # Interface
│   ├── GeminiRepositoryImpl.kt      # Gemini ChatSession + Room persistence
│   ├── db/
│   │   ├── ChatMessage.kt           # @Entity (id, role, text, timestamp)
│   │   ├── ChatDao.kt               # @Dao (insert, observe, delete)
│   │   └── ChatDatabase.kt          # @Database singleton
│   └── prefs/
│       └── UserPreferences.kt       # Preferences DataStore (userName, lastSession)
└── ui/
    ├── chat/
    │   ├── ChatScreen.kt            # Full Compose UI (LazyColumn, bubbles, voice, adaptive)
    │   ├── ChatViewModel.kt         # StateFlow + coroutine logic
    │   └── ChatUiState.kt           # Immutable UI state
    ├── text/
    │   └── BoldMarkdown.kt          # **bold** markdown renderer
    └── theme/
        ├── Color.kt                 # Gemini-inspired palette
        ├── Theme.kt                 # Material 3 + dynamic colour + dark mode
        └── Type.kt                  # Typography
```

---

## Branch & PR

- **Branch:** `N104-enhanced-gemini-chat`
- **Base:** `main`
- All commits verified to contain no API key literals.
