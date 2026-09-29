# Gemini Android Chat Client

An enhanced Android chat application built with **Jetpack Compose**, integrating Google's **Gemini Generative AI API** with secure key storage and real-time streaming capabilities.

---

## Key Changes & Enhancements

### 1. Hardware-Backed API Key Security (Android Keystore)
- **`SecureKeyStorage`**: Implemented encrypted key persistence utilizing `EncryptedSharedPreferences` backed by the Android Keystore system with `AES256_GCM` master key encryption scheme.
- Prevented plaintext exposure of API credentials across APK extraction and device storage inspections.

### 2. Live Response Streaming & Conversational Memory
- **`GeminiRepository` & `GeminiRepositoryImpl`**:
    - Replaced one-shot non-streaming calls with `sendMessageStream` to emit responses progressively via Kotlin `Flow`.
    - Added session retention using Gemini's `startChat()` multi-turn context manager, allowing follow-up queries within the same conversation.
    - Added a `resetChat()` function to cleanly clear context on demand.
    - Configured stable low-latency model endpoint `gemini-3.5-flash-lite`.

### 3. Modern Jetpack Compose UI
- **Conversation Bubbles**: Replaced the static single-response layout with an interactive `LazyColumn` featuring distinct message bubbles for the user (primary container, right-aligned) and the assistant (surface container, left-aligned).
- **Auto-Scrolling**: Used `LaunchedEffect` paired with `LazyListState.animateScrollToItem` to smoothly track incoming stream chunks.
- **Code Block & Markdown Formatting**: Added inline code-block parsing that renders triple-backtick markdown inside high-contrast monospaced cards with horizontal scrolling and a one-tap clipboard copy action.
- **TopAppBar with Session Reset**: Added a Material 3 TopAppBar with a trash icon to reset the chat session and wipe current message state.

---

## Architecture Overview
- **UI Layer**: Jetpack Compose screens (`ChatScreen`, `ChatMessageItem`) driven by unidirectional data flow from `ChatViewModel`.
- **State Management**: `MutableStateFlow<ChatUiState>` exposed immutably and collected via `collectAsStateWithLifecycle()`.
- **Data Layer**: Clean Repository pattern (`GeminiRepository`) isolating network requests, stream emission, and error handling.
- **Security**: Dedicated `SecureKeyStorage` module.

---

## Setup Instructions
1. Clone the repository:
   ```bash
   git clone <REPO_URL>