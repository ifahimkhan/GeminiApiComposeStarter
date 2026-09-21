# Gemini Jetpack Compose AI Chat Assistant

**Student Name:** Yug Viyala  
**Roll Number:** N118  
**Course:** Mobile Application Development (Subject Code: 702AI0E002)  
**Institution:** School of Technology Management & Engineering, SVKM's NMIMS, Mumbai  
**Git Branch:** `N118-assignment-1`  
**Base Repository:** [ifahimkhan/GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## Table of Contents
1. [Overview & Application Features](#overview--application-features)
2. [Architecture & Enhancements](#architecture--enhancements)
3. [Setup & Configuration](#setup--configuration)
4. [API Key Security Architecture](#api-key-security-architecture)
   - [Android Keystore & AES-256-GCM](#android-keystore--aes-256-gcm)
   - [Encryption at Rest Flow](#encryption-at-rest-flow)
   - [In-Memory Decryption Only](#in-memory-decryption-only)
5. [Data Layer Implementation](#data-layer-implementation)
   - [Room Database Persistent Chat History](#room-database-persistent-chat-history)
   - [Preferences DataStore](#preferences-datastore)
6. [UI & UX Enhancements](#ui--ux-enhancements)
   - [Material 3 Chat Bubbles & User/AI Differentiation](#material-3-chat-bubbles--userai-differentiation)
   - [LazyColumn with Stable Keys & Auto-Scrolling](#lazycolumn-with-stable-keys--auto-scrolling)
   - [Responsive & Adaptive Layouts](#responsive--adaptive-layouts)
   - [Speech-to-Text Voice Input](#speech-to-text-voice-input)
   - [Loading, Error & Dark Theme Handling](#loading-error--dark-theme-handling)
7. [Testing Suite](#testing-suite)
   - [Unit Testing (ChatViewModel)](#unit-testing-chatviewmodel)
   - [Instrumented Compose UI Testing](#instrumented-compose-ui-testing)
8. [Production Security Analysis & Limitations](#production-security-analysis--limitations)
9. [Verification & Commands](#verification--commands)

---

## 1. Overview & Application Features

This project extends the baseline Gemini Jetpack Compose application into an intelligent, secure, and responsive AI chat client powered by the Google Generative AI SDK (`gemini-3.6-flash`). The application allows users to interact with Google Gemini using text and voice inputs, preserves conversational history across device reboots and app restarts, secures the API key using hardware-backed cryptographic hardware, and delivers an adaptive Material 3 user experience.

### Key Capabilities:
- **Interactive Multi-Turn Chat:** Natural conversation with Gemini using modern Material 3 chat bubbles.
- **Voice Transcription:** One-tap speech-to-text input powered by Android's `RecognizerIntent`.
- **Persistent Chat History:** Seamless persistence across app restarts via an optimized Room Database.
- **Hardware-Backed Cryptography:** First-launch AES-256-GCM key generation inside Android Keystore (`KeyGenParameterSpec`) with ciphertext persistence.
- **Responsive Layout:** Adaptive styling optimized for phones and tablets in both portrait and landscape modes.
- **Robust Error Handling:** Sanitized error notifications that never leak API keys, system logs, or stack traces.

---

## 2. Architecture & Enhancements

The application follows the official Android Architecture Guidelines (Clean Architecture + MVI/MVVM):

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│   ChatRoute ──> ChatScreen (Stateless Composables)      │
│   Material 3 LazyColumn | Auto-Scroll | Voice Launcher │
└───────────────────────────▲────────────────────────────┘
                            │ collectAsStateWithLifecycle()
┌───────────────────────────┴────────────────────────────┐
│                    ViewModel Layer                     │
│   ChatViewModel (StateFlow<ChatUiState>)               │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutines / Flow
┌───────────────────────────┴────────────────────────────┐
│                    Repository Layer                    │
│   ChatRepository (Room DB Flow + Gemini API Result)    │
│   GeminiRepositoryImpl (Off-main-thread GenerativeModel)│
└─────────────▲────────────────────────────▲─────────────┘
              │                            │
┌─────────────┴──────────┐   ┌─────────────┴─────────────┐
│       Data Layer       │   │      Security Layer       │
│ ChatDatabase (Room)    │   │ ApiKeyManager (DataStore) │
│ UserPreferencesRepo    │   │ KeystoreManager (AES-GCM) │
└────────────────────────┘   └───────────────────────────┘
```

---

## 3. Setup & Configuration

### Prerequisites
- **Android Studio:** Ladybug Feature Drop (2024.2.2+) or Meerkat
- **JDK:** OpenJDK 21 (bundled in Android Studio JBR)
- **Android SDK:** API 36 (Compile/Target), API 26+ (minSdk)

### Cloning and Opening
```bash
git clone https://github.com/ifahimkhan/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
git checkout N118-assignment-1
```

### Configuring the Gemini API Key
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. In the project root, open or create `local.properties`:
   ```properties
   sdk.dir=PATH_TO_YOUR_ANDROID_SDK
   GEMINI_API_KEY=your_actual_api_key_here
   ```
3. A template `local.properties.example` is provided in the repository for reference.

### Why `local.properties` Must NEVER Be Committed
`local.properties` is strictly ignored in `.gitignore`:
- It stores local machine paths (`sdk.dir`) and sensitive credentials (`GEMINI_API_KEY`).
- Committing API keys to version control exposes credentials to automated scrapers, resulting in immediate key revocation, billing abuse, or quota exhaustion.
- Gradle reads `local.properties` at build time and injects the value into `BuildConfig.GEMINI_API_KEY`. In continuous integration (CI) environments, Gradle automatically falls back to `System.getenv("GEMINI_API_KEY")`.

---

## 4. API Key Security Architecture

### Android Keystore & AES-256-GCM
Android Keystore protects cryptographic keys by keeping them inside a Secure Element (SE) or Hardware Security Module (HSM) / Trusted Execution Environment (TEE).
- Symmetric Key Specification: **AES-256-GCM** (`KeyProperties.KEY_ALGORITHM_AES`, `KEY_SIZE_BITS = 256`).
- Block Mode & Padding: `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE` with a 128-bit authentication tag.
- Randomized Encryption: Explicitly enforced so every encryption operation produces a distinct Initialization Vector (IV).

### Encryption at Rest Flow
1. On first app launch, `ApiKeyManager.initializeKey()` checks whether an encrypted key is already saved in DataStore.
2. If not saved, `KeystoreManager` generates an AES-256 key inside the Android Keystore using `KeyGenParameterSpec`.
3. The build-time key from `BuildConfig.GEMINI_API_KEY` is encrypted with the Keystore key.
4. **Only the resulting ciphertext and IV** are Base64-encoded and persisted in Preferences DataStore.
5. The plaintext key is NEVER written to SharedPreferences, Room, or disk.

### In-Memory Decryption Only
- When a user sends a prompt, `GeminiRepositoryImpl` requests the decrypted key on demand via `apiKeyProvider()`.
- `KeystoreManager.decrypt(iv, ciphertext)` decrypts the ciphertext in memory using the Keystore key.
- The plaintext key is immediately passed to `GenerativeModel` inside volatile RAM.
- Plaintext keys are **never** logged (`Log.d`/`Log.e`), never toasted, never exposed in `ChatUiState`, and never persisted.

---

## 5. Data Layer Implementation

### Room Database Persistent Chat History
Past conversations survive app reboots and kills:
- **`ChatMessageEntity`**:
  - `id: Long` (Auto-generated primary key)
  - `role: String` (`"USER"` vs `"GEMINI"`)
  - `text: String` (Prompt or response markdown)
  - `timestamp: Long` (Epoch millisecond)
- **`ChatMessageDao`**: Exposes `getAllMessages(): Flow<List<ChatMessageEntity>>` allowing automatic, real-time UI updates when new messages are inserted.
- **`ChatDatabase`**: Room database singleton configured with safe fallback migrations.
- **`ChatRepository`**: Automatically persists user prompts before dispatching API requests, and commits Gemini responses upon receipt. A "Clear conversation" action is provided in the TopAppBar to wipe history when desired.

### Preferences DataStore
- Stores user preferences (`user_preferences.preferences_pb`) such as dynamic color toggling and UI theme options.
- Preferred over `SharedPreferences` because it runs asynchronously on `Dispatchers.IO`, prevents ANRs, and eliminates UI blocking.

---

## 6. UI & UX Enhancements

### Material 3 Chat Bubbles & User/AI Differentiation
- **User Messages:** Right-aligned, styled with `primaryContainer` and rounded bubble corners (`18.dp, 18.dp, 18.dp, 4.dp`).
- **Gemini Messages:** Left-aligned with an AI Assistant avatar (`ic_assistant`), styled with `surfaceVariant` (`18.dp, 18.dp, 4.dp, 18.dp`), supporting bold text rendering via `toBoldAnnotatedString()`.
- **Timestamps:** Each message bubble renders a localized formatted time (`hh:mm a`).

### LazyColumn with Stable Keys & Auto-Scrolling
- Implemented with `items(items = state.messages, key = { it.id })`.
- Stable keys eliminate unnecessary recompositions and ensure smooth animations.
- `LaunchedEffect(state.messages.size, state.isLoading)` animates the scroll position directly to the bottom of the list whenever a new turn is sent or received.

### Responsive & Adaptive Layouts
- The chat layout uses `Modifier.widthIn(max = 840.dp)` with centered alignment.
- On large tablets, foldables, and landscape orientations, the chat interface does not stretch awkwardly across the screen, maintaining optimal reading width.

### Speech-to-Text Voice Input
- Located directly in the message input bar via a dedicated microphone button (`ic_mic`).
- Launched using `rememberLauncherForActivityResult` with `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- Speech results are transcribed and appended to the prompt field for user confirmation before sending.
- Gracefully handles missing speech recognition engines (`ActivityNotFoundException`) via Snackbar notifications without crashing.

### Loading, Error & Dark Theme Handling
- **Loading Indicator:** When `isLoading = true`, a loading bubble with a `CircularProgressIndicator` and "Gemini is thinking…" renders at the bottom of the message list. Input controls and send buttons are disabled to prevent duplicate submissions.
- **Error State:** Network failures, quota exhaustion, or invalid keys are caught and presented via a clean `Snackbar` with sanitized messages. No stack traces or sensitive strings are shown.
- **Theme:** Full dynamic color (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) on Android 12+ and system dark theme support via `isSystemInDarkTheme()`.

---

## 7. Testing Suite

### Unit Testing (`ChatViewModelTest`)
Run with:
```bash
.\gradlew testDebugUnitTest
```
7 comprehensive unit tests verify:
1. **Initial State:** Empty prompt, empty messages, `isLoading = false`, zero errors.
2. **Input Validation:** Empty prompt submission triggers `PromptError.EMPTY` without invoking API.
3. **Missing Key Handling:** Shows `MISSING_API_KEY_MESSAGE` when API key is unconfigured.
4. **Loading State:** `isLoading = true` while awaiting asynchronous Gemini response; user bubble is rendered immediately.
5. **Successful Turn:** Gemini response appends Gemini bubble to conversation and resets `isLoading = false`.
6. **Error Handling:** Network/API failure updates `errorMessage` cleanly and allows error dismissal.
7. **Conversation Actions:** Voice transcription updates prompt; clearing conversation flushes history.

### Instrumented Compose UI Testing (`ChatScreenTest`)
Run with:
```bash
.\gradlew connectedAndroidTest
```
UI tests using `createComposeRule()` verify:
- Input bar, microphone icon, and send button exist on screen.
- Typing into the `OutlinedTextField` updates state properly.
- User and Gemini messages render in their respective bubbles.
- Thinking / loading indicator renders when `isLoading = true`.

---

## 8. Production Security Analysis & Limitations

### The Limits of Client-Side Encryption
While our implementation generates an AES-256-GCM key inside the hardware-backed Android Keystore and stores only ciphertext at rest, **client-side encryption alone cannot completely prevent a determined reverse engineer from extracting an API key**.
- **Memory Dumping & Instrumentation:** An attacker with a rooted device can attach dynamic analysis tools (such as Frida or Xposed), inspect JVM runtime memory, hook `GenerativeModel` constructors, or dump memory buffers during key decryption.
- **R8 / ProGuard Obfuscation:** While `isMinifyEnabled = true` makes static disassembly harder, decompiled control flow can still be analyzed.
- **Device Cloning / Key Extraction:** Although Android Keystore hardware keys cannot be exported directly, the application itself can be coerced into decrypting the key on behalf of the attacker.

### Recommended Production Architecture
For production mobile applications, client-side API keys should be replaced with:
1. **Backend Proxy Service (BFF Pattern):** The Android app should authenticate users with Firebase Auth or OAuth2 and communicate with a backend server (e.g., Cloud Run, Firebase Cloud Functions). The backend holds the Gemini API key securely in Secret Manager and forwards prompts to Gemini.
2. **Firebase App Check:** Validates that incoming requests originate strictly from an authentic, unmodified copy of your app running on an authentic device (via Play Integrity API or SafetyNet).
3. **API Key Restrictions:** Restrict API keys in the Google Cloud Console to specific IP addresses, HTTP referrers, or Android app package signatures (`SHA-256` certificate fingerprint).

---

## 9. Verification & Commands

### Clean & Assemble Debug APK
```bash
$env:JAVA_HOME = "D:\VALORANT\android studio\jbr"
.\gradlew assembleDebug
```

### Run Unit Tests
```bash
$env:JAVA_HOME = "D:\VALORANT\android studio\jbr"
.\gradlew testDebugUnitTest
```

### Git Inspection
```bash
git status --short
git branch --show-current
git diff --stat
```
Ensure `local.properties` is NEVER tracked or committed.
