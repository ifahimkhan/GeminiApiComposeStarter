# Gemini AI Study Workspace — Enhanced Android Jetpack Compose App

A comprehensive, distinctive, and production-quality **AI Study Workspace** built on top of the professor-provided **GeminiApiComposeStarter** repository.

The application elevates conventional conversational chat into a structured, pedagogical student companion powered by Google's Gemini API, complete with multi-turn conversation memory, 8 specialized educational study modes, multimodal diagram analysis, session management, offline study notes, text-to-speech reading, Room v2 database persistence with migrations, and Android Keystore AES-256-GCM encryption.

---

## 🌟 What Makes This Distinctive?

Unlike a basic chatbot, this application is engineered as an **interactive learning environment**:

1. **8 Dedicated AI Study Modes**: Not generic prompts, but dedicated pedagogical workflows (Concept Explainer, Socratic Study Tutor, Practice Quiz, Deep Challenge, Lecture Summarizer, Code Reviewer, Diagram Solver, and General Chat).
2. **True Conversational Memory**: Gemini maintains bounded multi-turn conversational context (`model.startChat`), so follow-up inquiries understand previous context seamlessly.
3. **Multimodal Diagram & Math Input**: Students can attach images of equations, textbook diagrams, circuits, or whiteboard code for step-by-step visual analysis.
4. **Rich Markdown & Code Block Viewer**: Monospace syntax formatting with horizontal scroll and a one-tap **Copy Code** button.
5. **Interactive Practice Quizzes & Insights**: Generates structured multiple-choice questions with answer evaluations, persistent attempt histories, and performance analytics.
6. **Offline Study Notes**: Star any high-yield Gemini explanation to persist it locally for offline revision.
7. **Text-To-Speech (TTS)**: Listen to explanations read aloud with native Android TextToSpeech.
8. **Multi-Session Management**: Create, switch, rename, and delete separate study conversations.

---

## 🏛️ Architecture Overview

The codebase adheres strictly to clean architectural separation between presentation, domain, and data layers:

```
UI Layer (Jetpack Compose & Material 3)
├── MainActivity (Edge-to-edge, Secure Key Init, TTS Lifecycle)
├── ChatScreen
│   ├── WorkspaceTopBar (Drawer Toggle, Session Title, Saved Notes, Insights, Clear)
│   ├── ModeAndActionHeader (Active Mode Badge, Socratic Quick Action Chips)
│   ├── ConversationDrawer (Multi-Session Switching & Creation)
│   ├── ChatMessageList (LazyColumn with stable UUID keys & auto-scrolling)
│   │   └── WorkspaceMessageBubble (User & Gemini)
│   │       ├── MarkdownViewer (Headings, bold text, styled code blocks + Copy button)
│   │       ├── Attached Image Thumbnail (Multimodal)
│   │       └── Gemini Action Bar (Copy, Read Aloud, Save Note, Simplify)
│   ├── WorkspaceLoadingBubble (Animated thinking state)
│   └── WorkspaceInputBar (Image picker, Voice mic, Mode selector, Multiline TextField, Send)
├── Modals & Sheets
│   ├── StudyModeSelectorBottomSheet (8 academic modes with icons & descriptions)
│   ├── SavedNotesDialog (Offline reference sheet)
│   └── SessionInsightsDialog (Quiz scores, strong areas, revision topics)
└── Theme (Material 3 Dynamic Dark/Light, responsive centered constraints up to 840dp)

ViewModel Layer
└── ChatViewModel (StateFlow<ChatUiState>, lifecycle-aware collection)
    ├── ContextEngine (Bounded multi-turn rolling history window + mode instructions)
    ├── ResponseValidator (Sanitization, validation, zero-crash guarantee)
    ├── Auto-Titling (Background session naming via Gemini)
    └── Repositories Coordination (GeminiGateway + Room v2 + Preferences)

Core AI & Processing Layer
├── ContextEngine (Curates rolling conversation context without token overflow)
├── StudyPromptBuilder (System instruction strategies for all 8 modes)
├── ResponseValidator (Output validation, sanitization, code extraction)
├── AiResponse & AiResponseParser (Structured code blocks, quiz items, suggestions)
├── GeminiGateway / GeminiRepository (SDK abstraction, multi-turn, multimodal, retry)
└── TextToSpeechManager (Android native TTS lifecycle management)

Data & Persistence Layer (Room v2)
├── AppDatabase (Version 2, with safe MIGRATION_1_2 preserving existing records)
├── ConversationEntity & ConversationDao (Sessions history)
├── ChatMessageEntity & ChatMessageDao (Conversations, saved notes)
├── QuizAttemptEntity & QuizAttemptDao (Study analytics)
├── ChatHistoryRepository (Entity ↔ Domain mapping)
└── UserPreferencesRepository (Jetpack Preferences DataStore)

Security Layer
└── SecureApiKeyManager
    ├── Android Keystore (Hardware-backed AES-256 master key)
    └── AES/GCM/NoPadding encrypted storage in private SharedPreferences
```

---

## 📚 The 8 AI Study Modes

| Mode | Pedagogical Purpose | Output Structure |
|---|---|---|
| **💬 General Chat** | Open-ended academic assistant | Balanced explanations, clear formatting |
| **💡 Concept Explainer** | Systematic concept breakdown | Core Concept &rarr; Intuition &rarr; Key Takeaways &rarr; Example &rarr; Pitfalls &rarr; Self-Check |
| **🎓 Socratic Study Tutor** | Active step-by-step guided learning | Teaches one micro-concept at a time, then quizzes you before moving forward |
| **🎯 Practice Quiz** | Automated examination generator | Multiple-choice questions with 4 options, answer key, and detailed rationale |
| **🔥 Deep Challenge** | Critical evaluation & debate | Spotting subtle bugs, counter-arguments, performance trade-offs |
| **📝 Lecture Summarizer** | Exam revision condensation | Executive TL;DR &rarr; Key Takeaways &rarr; Glossary &rarr; Exam Revision Notes |
| **💻 Code Reviewer** | Static analysis & optimization | Code Summary &rarr; Bugs & Edge Cases &rarr; Big-O Complexity &rarr; Refactored Code Block |
| **👁️ Image Analysis** | Multimodal visual problem solver | Diagram breakdown, equation transcription, step-by-step mathematical solution |

---

## 🔒 Security Model & Secrets Management

Security is a primary requirement. The API key is safeguarded through a multi-tier defense:

1. **Zero Committed Secrets**: `local.properties` is explicitly git-ignored. The repository contains only safe templates (`local.properties.example`).
2. **Gradle Ingestion Chain**: Gradle reads `GEMINI_API_KEY` from `local.properties` (or environment variables) and injects it into `BuildConfig.GEMINI_API_KEY`.
3. **Android Keystore Encryption**: On first app launch, `SecureApiKeyManager` generates an AES-256 key inside the hardware-backed `AndroidKeyStore`. The key is encrypted with `AES/GCM/NoPadding` and stored as ciphertext in private `SharedPreferences`.
4. **In-Memory Decryption**: On subsequent launches, the ciphertext is decrypted only in memory when instantiating `GenerativeModel`.
5. **Leak Prevention**: The plaintext API key is never logged, never saved in plaintext, never exposed in error snackbars, and output is actively sanitized by `ResponseValidator`.

> **⚠️ Security Limitation:** Client-side encryption protects keys at rest on device. On rooted devices or under advanced memory analysis, client-side secrets can be intercepted. Production deployments should route requests through an authenticated backend proxy with Firebase App Check.

---

## 🗄️ Database Architecture & Migrations

The application uses Room version 2 with a non-destructive migration (`MIGRATION_1_2`):

- **`conversations`**: Stores session ID, title, timestamp, and mode.
- **`chat_messages`**: Stores message ID, `conversationId` (defaults to `'default'`), role, content, timestamp, `isSaved` bookmark flag, and optional `imageUri`.
- **`quiz_attempts`**: Stores quiz session topic, score, question count, percentage, strong areas, and revision recommendations.

> **Zero Data Loss:** `MIGRATION_1_2` executes `ALTER TABLE` and `INSERT OR IGNORE INTO conversations` to preserve all existing messages on previously tested devices under a `"Main Study Session"`.

---

## 🧪 Testing Suite

Automated tests require **zero API keys** and execute in complete offline isolation using deterministic fakes:

### Unit Tests (28 Tests — 100% Pass)
Command:
```powershell
.\gradlew.bat testDebugUnitTest
```
- **`ChatViewModelTest` (18 tests)**: Initial state, prompt change, empty prompt validation, missing API key handling, send flow, loading states, Gemini response handling, error dismissals, clear history, Room persistence, history restoration on launch, mode selection, Socratic quick actions, saved note toggling, session creation and switching, session deletion, quiz score recording, and multi-turn conversational history passing.
- **`ContextEngineTest` (3 tests)**: Bounded rolling window limits, prompt wrapping, and saved notes inclusion.
- **`ResponseValidatorTest` (4 tests)**: Null/empty validation, code block extraction, and secret redaction.
- **`StudyPromptBuilderTest` (3 tests)**: Mode coverage, system instructions completeness, and prompt preservation.

### Compose UI Tests (15 Tests)
- **`ChatScreenTest`**: Empty state rendering, input field, send button, voice button, mode selector button, image picker button, saved notes button, insights button, text input, message display, loading indicator, disabled states, and role labels.

---

## 🚀 Setup & Build Instructions

### 1. Configure the API Key
1. Copy the example properties file:
   ```powershell
   cp local.properties.example local.properties
   ```
2. Open `local.properties` and add your Google Gemini API key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```

### 2. Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Build Minified Release APK (R8 Enabled)
```powershell
.\gradlew.bat assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

### 4. Run Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

---

## 📱 Interactive Demo Flow for Evaluation

1. **Launch App**: Observe the welcoming AI Study Workspace empty state with study prompt suggestions.
2. **Multi-Turn Conversation**:
   - Send: `"My name is Shubh."`
   - Send follow-up: `"What is my name?"`
   - Observe: Gemini remembers your name via multi-turn context!
3. **Change Study Mode**:
   - Tap `[⚡ Chat]` in the top bar to open the **Academic AI Study Modes** bottom sheet.
   - Select **💡 Concept Explainer**.
   - Send: `"Explain Database Normalization."`
   - Observe: Gemini formats output with Core Concept, Intuitive Explanation, Key Takeaways, Examples, Pitfalls, and Quick Check.
4. **Socratic Study Tutor**:
   - Switch to **🎓 Socratic Study Tutor**.
   - Send: `"Teach me Binary Search Trees."`
   - Tap the Socratic action chips (`[💡 Hint]`, `[▶ Continue]`, `[🔄 Explain Simpler]`).
5. **Practice Quiz**:
   - Switch to **🎯 Practice Quiz**.
   - Send: `"Quiz me on OS Scheduling."`
   - Tap **Study Insights** (`📊`) in the top app bar to view session analytics.
6. **Code Review & Copy**:
   - Switch to **💻 Code Reviewer**.
   - Paste a code snippet.
   - Tap the **Copy Code** icon in the code block header to copy refactored code.
7. **Multimodal Diagram Analysis**:
   - Tap the image attachment icon (`🖼️`).
   - Select a diagram or equation screenshot.
   - Send: `"Explain this architecture."`
8. **Save Study Notes**:
   - Tap the bookmark icon (`🔖`) on any Gemini response.
   - Tap the **Saved Notes** icon in the top app bar to review your saved notes offline.
9. **Text-To-Speech (TTS)**:
   - Tap the speaker icon (`🔊`) on any response to hear it read aloud.
10. **Multi-Session Management**:
    - Tap the menu icon (`☰`) to open the sessions drawer.
    - Tap **New Study Session** to start a clean conversation.
