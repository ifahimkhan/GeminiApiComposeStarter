# Mobile Application Development — Lab Assignment 1
## Conversational AI Assistant using Jetpack Compose, Material 3 & Google Gemini API

---

### Student Information
- **Student Name:** Tanmay Sharma
- **Roll Number:** N159
- **Course:** Mobile Application Development (CS / IT)
- **Institution:** SVKM's NMIMS University
- **Git Branch:** `N159-assignment-1`
- **Base Repository:** [GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter)

---

## 1. Executive Summary & Application Overview

This project is a production-grade, secure, modern Android conversational AI application developed as part of **Mobile Application Development Lab Assignment 1**. 

Building upon the starter project, this implementation delivers:
- A fully responsive **Jetpack Compose Material 3** chat interface.
- Native integration with **Google's Gemini Generative AI SDK** (`gemini-3.8-flash`).
- **Android Keystore hardware-backed AES-256-GCM encryption-at-rest** for API key security.
- **Room SQLite database** for persistent, local-first chat history with reactive Kotlin Coroutines `Flow`.
- **Jetpack Preferences DataStore** for user preferences (theme mode, user profile, auto-scroll).
- **Voice speech-to-text input** via Android's `RecognizerIntent`.
- **Adaptive layout design** utilizing `WindowWidthSizeClass` (Compact, Medium, Expanded).
- **ProGuard / R8 minified release build** with zero runtime regressions.
- **Comprehensive test suite** comprising 24 Unit Tests and Compose UI Instrumentation Tests.

---

## 2. Architecture & Design Patterns

The application follows the official **Android Recommended Architecture** guidelines with **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer                              │
│  ChatScreen (Jetpack Compose M3) ◄──► ChatRoute / Activity   │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow<ChatUiState>
                               │ Events (onSend, onRetry...)
┌──────────────────────────────┴──────────────────────────────┐
│                    ViewModel Layer                          │
│                      ChatViewModel                          │
└──────────────────────────────▲──────────────────────────────┘
                               │ Coroutines / Flow
┌──────────────────────────────┴──────────────────────────────┐
│                    Repository Layer                         │
│  GeminiRepositoryImpl  │  RoomChatHistoryRepository         │
│  SecureApiKeyManager   │  UserPreferencesRepository         │
└───────────────▲──────────────────────────────▲──────────────┘
                │                              │
┌───────────────┴──────────────┐ ┌─────────────┴──────────────┐
│        Remote Source         │ │         Local Storage      │
│   Google Generative AI SDK   │ │  Room DB (SQLite Cipher)   │
│       (gemini-3.8-flash)     │ │  Preferences DataStore     │
│                              │ │  Android Keystore (AES-GCM)│
└──────────────────────────────┘ └────────────────────────────┘
```

### Key Architectural Layers:
1. **Presentation Layer (`ui/chat`, `ui/theme`)**:
   - Pure Jetpack Compose with Material 3 styling, dynamic color theming, and elevation tokens.
   - `ChatViewModel` exposes an immutable `StateFlow<ChatUiState>`, observed in Compose via `collectAsStateWithLifecycle()` to prevent background resource leaks.
   - Screen states include loading indicators, error banners, retry prompts, confirmation dialogs, and student details.

2. **Domain Layer (`data/model`)**:
   - `ChatMessage`: Core immutable domain model representing text content, sender (`USER` or `GEMINI`), status (`SUCCESS`, `LOADING`, `ERROR`), and Unix epoch timestamps.

3. **Data Layer (`data/`, `data/local/`, `data/preferences/`)**:
   - `GeminiRepository`: Abstraction for AI inference, allowing clean mocking in tests via `FakeGeminiRepository`.
   - `ChatHistoryRepository`: Room abstraction providing `Flow<List<ChatMessage>>` for reactive SQLite updates.
   - `UserPreferencesRepository`: Kotlin DataStore Preferences managing persistent settings asynchronously.

4. **Security Layer (`data/security/`)**:
   - `CryptoManager`: Interfaces with the hardware-backed `AndroidKeyStore` to generate, store, and utilize AES-256 keys in GCM mode.
   - `SecureApiKeyManager`: Coordinates in-flight encryption of the build-time key before writing ciphertext to DataStore.

---

## 3. Comprehensive Feature Implementation

### 3.1 Material 3 Conversational Interface
- **Dynamic Message Bubbles:** Styled according to Material 3 guidelines (`primaryContainer` for user messages, `surfaceVariant` for Gemini responses).
- **Auto-Scrolling:** Automatically scrolls to the bottom on message submission or streaming arrival using `LazyListState.animateScrollToItem()`.
- **Markdown Formatting:** Custom regex-driven `toBoldAnnotatedString()` parses Markdown bold text (`**text**`) into bold Compose `AnnotatedString` spans.
- **Thinking Indicator:** Displays a smooth `CircularProgressIndicator` with companion text (*"Gemini is thinking…"*) during inference.

### 3.2 Secure API Key & Keystore Management
- **Zero Hardcoded Secrets:** Keys are injected via `local.properties` (git-ignored) or environment variables into `BuildConfig.GEMINI_API_KEY`.
- **Hardware-Backed Keystore:** The master key is stored securely in the hardware Secure Element (SE) / StrongBox Keymaster.
- **Authenticated Encryption:** Uses **AES-256-GCM** (Galois/Counter Mode) with an initialization vector (IV) and 128-bit authentication tag to ensure ciphertext confidentiality and integrity.

### 3.3 Offline Room Persistence
- **Room Database (`ChatDatabase`):** Persists all chat messages across app restarts and device reboots.
- **Reactive UI Updates:** Room queries return Kotlin Coroutines `Flow<List<ChatMessageEntity>>`, automatically notifying the UI of additions, updates, or deletions.
- **Clear History:** Users can purge chat history with a confirmation dialog, safeguarding against accidental deletions.

### 3.4 Speech-to-Text Voice Input
- Built-in microphone button triggers Android's `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- Transcribed speech is appended directly into the prompt text field, allowing users to review or edit before sending.

### 3.5 Adaptive Layout (WindowSizeClass)
- Supports phone, foldable, and tablet form factors.
- Automatically adjusts message bubble width constraints (up to 85% on compact phone screens, max 600dp on large tablets and landscape mode) for optimal readability.

### 3.6 Student Information Dialog
- Accessible via the info button in the top app bar.
- Explicitly presents student details:
  - **Name:** Tanmay Sharma
  - **Roll Number:** N159
  - **Course:** Mobile Application Development
  - **Institution:** SVKM's NMIMS University

---

## 4. Security Audit & Best Practices

1. **`.gitignore` Hardening:** Both root and nested `local.properties` files are strictly ignored to prevent leaking credentials.
2. **Template Provided:** `local.properties.example` provides an illustrative template without exposing sensitive values.
3. **No Leakage in Git History:** Full audit performed (`git log`, `git diff`) verifying zero occurrences of `AIza...` API keys.
4. **Keystore Integrity:** Plaintext API keys exist only in volatile RAM during active request dispatch; encrypted ciphertext is saved to local storage.

---

## 5. Build & Setup Instructions

### 5.1 Prerequisites
- **Android Studio:** Ladybug (2024.2+) or Meerkat
- **JDK:** OpenJDK 17 or higher (tested on JDK 25)
- **Android SDK:** API 35 / 36 platform installed
- **Gemini API Key:** Free key obtainable from [Google AI Studio](https://aistudio.google.com/)

### 5.2 Setting Up the API Key
1. Create a `local.properties` file in the root directory (if not already present):
   ```properties
   sdk.dir=/Users/tanmay/Library/Android/sdk
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
2. Alternatively, set the environment variable:
   ```bash
   export GEMINI_API_KEY="your_actual_gemini_api_key_here"
   ```

### 5.3 Building & Running

**Debug Build:**
```bash
./gradlew assembleDebug
```
*Output APK located at:* `app/build/outputs/apk/debug/app-debug.apk`

**Minified Release Build (R8 Enabled):**
```bash
./gradlew assembleRelease
```
*Output APK located at:* `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 6. Testing Strategy & Verification

### 6.1 Unit Tests (24 Passed)
Located in `app/src/test/java/com/fahim/geminiApiComposeStarter/`:
- **`ChatViewModelTest` (16 tests):**
  - Initial UI state verification
  - Prompt text updates & empty prompt validation
  - Missing API key banner handling
  - Successful message roundtrip (FakeGeminiRepository)
  - Network failure handling & retry mechanism
  - Voice input concatenation logic
  - Clear chat history action
  - Theme mode toggling
  - Duplicate send prevention during active loading
- **`ChatMessageTest` (4 tests):**
  - Model defaults, timestamp generation, sender and status validation
- **`BoldMarkdownTest` (4 tests):**
  - Plaintext preservation, single bold spans, multiple bold spans, empty input

**Run Unit Tests:**
```bash
./gradlew test
```

### 6.2 Compose UI Tests
Located in `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt`:
- `emptyState_isDisplayedWhenNoMessages`
- `messageList_displaysUserAndGeminiMessages`
- `promptInput_triggersCallback`
- `sendButton_triggersOnSend`
- `retryBanner_isDisplayedWhenCanRetry`
- `studentInfoDialog_displaysStudentDetails`
- `clearConfirmDialog_isDisplayedWhenRequested`

**Compile Instrumentation Tests:**
```bash
./gradlew assembleDebugAndroidTest
```

---

## 7. Submission Details

- **Student:** Tanmay Sharma (Roll: N159)
- **Git Branch:** `N159-assignment-1`
- **Commit Message Format:** `N159: Complete Mobile Application Development Assignment 1`
