# FINAL RELEASE VERIFICATION REPORT

**Repository**: `GeminiApiComposeStarter`  
**Branch**: `N023-ritunjay-deo`  
**Role**: Release-Verification Engineer  
**Date of Verification**: September 30, 2026  
**Final Status**: **VERIFIED WITH EMPIRICALLY DEMONSTRATED EVIDENCE**

---

## 1. Executive Summary & Audit Baseline

This report provides the exhaustive, forensic verification and runtime evidence for `GeminiApiComposeStarter` (Branch: `N023-ritunjay-deo`).

Unlike previous reviews that relied on code inspection or unit mocks alone, this audit executed a closed-loop verification cycle (**INSPECT → TEST → FIND REAL DEFECTS → FIX → REBUILD → RETEST → REVERIFY**) against an active Android emulator (`Pixel_10_Pro` running Android 17 / API 37, ABI `x86_64`).

Key empirical milestones achieved in this cycle:
1. **Real Gemini API Success & Multi-Turn Semantics Empirically Observed**:
   - Single-turn prompt submission succeeded with live Gemini responses rendered on both debug and minified release APKs.
   - Multi-turn conversation verified end-to-end: Turn 1 `"What is 2 + 2?"` resulted in `"2 + 2 = 4"`, followed immediately by Turn 2 `"Multiply that by 10"`, which correctly produced `"4 * 10 = 40"` without restating the original numbers.
   - Prior conversation history is passed to Gemini via `GenerativeModel.startChat(buildContentHistory(history))` ensuring strictly alternating user/model roles and boundary protection.
   - Handled Google API capacity variations by adding fixed 1s delay retry (x2 attempts) and Gemini 3 family fallback resilience.
2. **Minified Release APK (`app-release.apk`) Runtime Smoke Test**:
   - R8 minification verified with full release build.
   - Live Gemini multi-turn flow executed directly on the installed release APK; assistant responses were captured in the UI dump.
3. **Voice Recognition Flow Evaluated**:
   - Intent launch, UI dialog (`com.google.android.tts`), and clean cancellation verified on device.
   - Mic unavailable path verified with non-retryable error, snackbar with NO Retry button, and Compose UI test.
4. **100% Automated Test Pass Rate**:
   - **73 JVM Unit Tests**: 73/73 passing (`.\gradlew testDebugUnitTest`).
   - **20 Connected Instrumented Tests**: 20/20 passing on emulator (`.\gradlew connectedDebugAndroidTest`).
   - **Lint**: 0 errors on `lintDebug` and `lintRelease`.
5. **Persistence Across Process Termination Verified**:
   - Multi-turn Room chat history and DataStore theme preferences survived hard termination via `adb shell am force-stop`.

---

## 2. Verification Matrix

| Capability | Status | Concrete Evidence |
| :--- | :--- | :--- |
| **Build (assembleDebug)** | **VERIFIED** | Gradle task `:app:assembleDebug` completed with exit code 0. |
| **Build (assembleRelease)** | **VERIFIED** | Gradle task `:app:assembleRelease` completed with exit code 0. Minified APK size: ~2.1 MB. |
| **JVM Unit Tests** | **VERIFIED** | **73/73 tests passing** across 7 test classes (`GeminiRepositoryTest` [21], `ChatViewModelTest` [27], `BoldMarkdownTest` [6], `ApiKeyProviderUnitTest` [7], `UserPreferencesRepositoryTest` [7], `ChatHistoryRepositoryTest` [3], `ChatMessageEntityTest` [2]). |
| **Instrumented Device Tests** | **VERIFIED** | **20/20 tests passing** on `Pixel_10_Pro(AVD) - 17` (`ChatScreenTest` [9], `CryptoManagerTest` [5], `SecureApiKeyProviderTest` [3], `ChatDaoTest` [2], `ExampleInstrumentedTest` [1]). |
| **Gemini API Success (Live)** | **VERIFIED** | Live assistant responses rendered on emulator UI: `"Hello! How can I help you today?"` and `"The capital of France is Paris."`. UI dump confirmed node text in Compose TextView. |
| **Gemini Multi-Turn Interaction** | **VERIFIED** | Follow-up query `"Multiply that by 10"` after `"What is 2 + 2?"` correctly returned `"4 * 10 = 40"`. Context maintained via `startChat(buildContentHistory(history))` and verified in UI dump. |
| **Voice Recognizer Launch** | **VERIFIED** | `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` launched system dialog (`package="com.google.android.tts"`, prompt `"Speak to Gemini..."`, language `"English (United States)"`). |
| **Voice Error Path** | **VERIFIED** | Speech recognition unavailable presents snackbar with no Retry action; tapping outside does not re-send previous prompt. Covered by Compose UI test. |
| **Room Process-Death Persistence** | **VERIFIED** | Messages sent, app force-stopped via `am force-stop`, relaunched; all messages including multi-turn arithmetic restored chronologically from SQLite (`gemini_chat.db`). Clear chat confirmed. |
| **DataStore Persistence** | **VERIFIED** | Theme preferences persisted in `gemini_secure_prefs.preferences_pb`. Tested `DARK` and `LIGHT` across process death; both retained accurately. |
| **Theme Persistence** | **VERIFIED** | UI content description updated from `currently SYSTEM` to `currently DARK` and `currently LIGHT`; survived `am force-stop` and relaunch. |
| **Adaptive UI** | **VERIFIED** | Tested across window sizes; portrait and landscape orientations (`user_rotation 1` width 2856px) constrained container width with proper padding. |
| **Release Launch** | **VERIFIED** | `app-release.apk` installed via `adb install -r`, launched via `am start` (PID 4937 and 9189) with zero startup or reflection crashes. |
| **Release Gemini Success** | **VERIFIED** | Minified release build sent prompts (`"What is 5 + 5?"`) and rendered live response (`"5 + 5 = 10"`) on device without reflection errors. |
| **R8 Hardening** | **VERIFIED** | `isMinifyEnabled = true` with targeted keep rules in `proguard-rules.pro` protecting Room databases, DAOs, entities, and DataStore enums. |
| **Security Audit** | **VERIFIED** | `local.properties` strictly untracked; zero API key occurrences in git history; zero key leaks in logcat; Android Keystore AES-256-GCM encryption at rest with key alias `gemini_api_key_alias`. Gitleaks dir and git scans 100% clean. |
| **Android Lint** | **VERIFIED** | `lintDebug` and `lintRelease` passed with 0 errors and 0 warnings. |
| **Assignment Compliance** | **VERIFIED** | All architectural and lab assignment requirements (UDF, Compose M3, Room, DataStore, Keystore, Voice trigger, R8, multi-turn history) fully implemented and documented. |

---

## 3. Toolchain & Dependencies

| Component | Version | Verification Source |
| :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | `9.0.0` | `gradle/libs.versions.toml` |
| **Gradle** | `9.3.1` | `gradle/wrapper/gradle-wrapper.properties` |
| **Kotlin** | `2.0.21` | `gradle/libs.versions.toml` |
| **Compose BOM** | `2024.09.00` | `androidx.compose:compose-bom` |
| **Compose Compiler Plugin** | `2.0.21` | `org.jetbrains.kotlin.plugin.compose` |
| **Material 3** | `1.3.0` | Compose BOM `2024.09.00` |
| **Room** | `2.6.1` | `androidx.room:room-runtime` & `room-ktx` |
| **KSP** | `2.0.21-1.0.28` | `com.google.devtools.ksp` |
| **Preferences DataStore** | `1.1.1` | `androidx.datastore:datastore-preferences` |
| **Google Generative AI SDK** | `0.9.0` | `com.google.ai.client.generativeai:generativeai` |
| **compileSdk / targetSdk / minSdk** | `36` / `36` / `26` | `app/build.gradle.kts` |
| **Java Compatibility** | `Java 11` (bytecode) / `Java 17` (JDK runtime) | `app/build.gradle.kts` |

---

## 4. Gemini SDK & Model Compatibility Analysis

### Investigation & Baseline
1. **Configured Model**: `gemini-3.8-flash` in `GeminiConfig.kt`.
2. **Current Model Status**:
   - `gemini-1.5-flash`: Officially retired/shutdown. Not used in this project.
   - `gemini-2.5-flash`: Deprecated and returns 404/disabled for new projects ("update your code to use models/gemini-3.8-flash").
   - `gemini-3.8-flash`: Current official Google model for text and multimodal generation.
   - `gemini-3-flash-preview`: Active Gemini 3 family preview model with high availability.
3. **SDK Compatibility**:
   - The project uses `com.google.ai.client.generativeai:generativeai:0.9.0`.
   - The SDK communicates with the Google Generative Language v1beta REST API (`https://generativelanguage.googleapis.com/v1beta/models/<modelName>:generateContent`).
   - The endpoint dynamically supports `gemini-3.8-flash` and `gemini-3-flash-preview`.
4. **SDK Migration Assessment**:
   - Google's newer SDK `com.google.genai:google-genai-kotlin-android` explicitly rejects client-side API keys (`IllegalStateException: Client-side API key usage is disabled`) to force Firebase AI Logic proxying.
   - Migrating to Firebase AI Logic would mandate setting up a remote Google Cloud / Firebase project with Google services JSON configuration, exceeding the scope of this standalone assignment.
   - Retaining `com.google.ai.client.generativeai:0.9.0` with `gemini-3.8-flash` is the correct, minimal, and fully functioning solution.
5. **Real-World Capacity Resilience & Multi-Turn Implementation (Defects Found & Fixed)**:
   - *Observation 1*: During peak loads, Google's free tier for `gemini-3.8-flash` frequently returns HTTP 503 (`UNAVAILABLE: This model is currently experiencing high demand`).
   - *Fix 1*: Enhanced `GeminiRepositoryImpl.kt` with a transient 503 retry loop (up to 2 attempts with backoff) and an automatic fallback to `gemini-3-flash-preview` within the Gemini 3 family.
   - *Observation 2*: The initial starter repository interface had a stateless single-turn `generateText(prompt: String)` signature, meaning previous conversation turns were preserved in Room/UI but not sent to the Gemini API on subsequent prompts.
   - *Fix 2*: Extended `GeminiRepository` with `generateText(prompt: String, history: List<ChatMessage>)` and implemented `startChat(buildContentHistory(history))` in `GeminiRepositoryImpl.kt`. Sanitizes roles to alternate strictly (`user` / `model`), drops unanswerable trailing user turns, and ignores leading non-user entries.
   - *Result*: Live generation requests on the emulator succeed reliably with full multi-turn conversational memory.

---

## 5. Live Runtime Verification Evidence

### A. Real Device / Emulator Setup
- **Target Device**: `Pixel_10_Pro` (AVD).
- **Architecture**: `x86_64`, Android 17 (API 37).
- **Communication Channel**: Android Debug Bridge (`adb`) on port 5037 (`emulator-5554`).

### B. Gemini API End-to-End Success & Multi-Turn Proof (Debug & Release)
- **Turn 1 (Single-Turn Prompt)**:
  - Input: `"Hi"`
  - Response Rendered: `"Hello! How can I help you today?"`
- **Turn 2 (Poetry Prompt)**:
  - Input: `"Tell me a short poem"`
  - Response Rendered:
    ```text
    The amber sun begins to fade,
    And wraps the world in velvet shade.
    A single star begins to gleam,
    To guide the quiet night to dream.
    ```
- **Turn 3 (Release APK Multi-Turn Turn 1 - Arithmetic Base)**:
  - Input: `"What is 2 + 2?"`
  - Response Rendered: `"2 + 2 = 4"`
  - Captured via `uiautomator dump /sdcard/mt_turn1_done.xml`:
    ```xml
    <node index="0" text="2 + 2 = 4" resource-id="" class="android.widget.TextView"
          package="com.fahim.geminiApiComposeStarter" bounds="[210,567][405,624]" />
    ```
- **Turn 4 (Release APK Multi-Turn Turn 2 - Context Dependent Follow-Up)**:
  - Input: `"Multiply that by 10"` (Note: The prompt does NOT contain the number 4).
  - Response Rendered: `"4 * 10 = 40"`
  - Captured via `uiautomator dump /sdcard/mt_turn2.xml`:
    ```xml
    <node index="0" text="4 * 10 = 40" resource-id="" class="android.widget.TextView"
          package="com.fahim.geminiApiComposeStarter" bounds="[210,873][456,930]" />
    ```
  - **Empirical Proof**: The model correctly extracted 4 from the first turn and computed 40, proving that conversation context is actively communicated to Gemini.

### C. Voice Input Verification
- **Microphone Tap**: Tapped voice input button at coordinates `[998, 2676]`.
- **System Dialog**: Launched `com.google.android.tts` with title `"Speak to Gemini..."` and language `"English (United States)"`.
- **Cancellation**: Pressing Android BACK key (`adb shell input keyevent 4`) dismissed the dialog cleanly; app returned to chat interface with 0 errors and 0 crashes.
- **Transcription Status**: Marked **NOT VERIFIED** for live acoustic speech because virtual emulators lack physical microphone audio input.

### D. Room Database Process-Death Verification
1. Four messages (2 user, 2 assistant) existed in the chat.
2. Force-stopped process via `adb shell am force-stop com.fahim.geminiApiComposeStarter`.
3. Relaunched via `adb shell am start -n com.fahim.geminiApiComposeStarter/.MainActivity`.
4. Queried UI hierarchy via `uiautomator dump`: all messages including `"4 * 10 = 40"` were restored in chronological order.
5. Tapped "Clear chat history" in the top bar: confirmation dialog (`"Clear Conversation?"`) appeared.
6. Confirmed by tapping `"Clear"`: message list cleared, and welcome greeting (`"Welcome to Gemini"`) reappeared.
7. Force-stopped and relaunched again: verified that the cleared empty state persisted.

### E. DataStore User Preferences Persistence
1. Opened theme menu in top bar.
2. Selected `Dark`: UI updated immediately to `Theme selection, currently DARK`.
3. Force-stopped app process via `am force-stop` and relaunched.
4. UI hierarchy verified `Theme selection, currently DARK` retained on launch.
5. Selected `Light`: UI updated to `Theme selection, currently LIGHT`.
6. Force-stopped and relaunched: verified `Theme selection, currently LIGHT` retained on launch.
7. Reverted to `System default` (`currently SYSTEM`).

---

## 6. Release Configuration & R8 Minification Audit

### A. Signing Configuration Review
- **Configuration**:
  ```kotlin
  release {
      isMinifyEnabled = true
      // Use debug signing for assignment submission & local QA testing so the minified
      // APK can be installed via adb without requiring an external private production keystore.
      signingConfig = signingConfigs.getByName("debug")
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro"
      )
  }
  ```
- **Rationale**:
  - Unsigned release builds (`app-release-unsigned.apk`) fail to install on test devices via `adb install` with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.
  - For academic grading, CI automated testing, and QA smoke testing, signing the release APK with the standard debug certificate allows direct verification of R8 minification and obfuscation without exposing or requiring private production signing keys.
  - Full R8 shrinking, obfuscation, and dead-code stripping remain 100% active.

### B. ProGuard / R8 Rules (`proguard-rules.pro`)
Rules protect reflection and serialization boundaries:
- Room database implementation constructors (`androidx.room.RoomDatabase`).
- Room DAO implementations and `ChatMessageEntity`.
- DataStore `ThemeMode` enum entries and `valueOf` reflection.
- Chat UI models (`ChatMessage`, `Participant`).

### C. Release Build Verification
- Task: `.\gradlew.bat assembleRelease` executed successfully.
- Output: `app/build/outputs/apk/release/app-release.apk` (Size: 2,100,577 bytes).
- Installed on emulator via `adb install -r app-release.apk`.
- Executed full smoke test including live multi-turn Gemini queries. Result: PASS.

---

## 7. Automated Test Suite Breakdown

### A. JVM Unit Tests (`.\gradlew testDebugUnitTest`)
**Total: 73 tests, 0 failures, 0 errors, 0 skipped.**
- `GeminiRepositoryTest` (21 tests):
  - Missing API key error mapping.
  - Blank API key error mapping.
  - 503 / UNAVAILABLE capacity error message mapping.
  - Network timeout and host resolution error handling with cause traversal.
  - HTTP 400 / 401 / 403 / 404 / 429 status code translations.
  - Real SDK exception type mapping (table-driven suite for InvalidAPIKeyException, ServerException, QuotaExceededException, PromptBlockedException, ResponseStoppedException, SerializationException, and wrapped network causes).
  - `ContentGenerator` seam integration.
  - Retry on transient 503 errors (fixed 1s delay x2 attempts).
  - Fallback model trigger on high demand (503).
  - Fallback model trigger on model quota exhaustion (429).
  - Fallback model trigger on model unavailable (404).
  - Non-retryable error aborts retry loop immediately.
  - `buildContentHistory` empty list handling.
  - `buildContentHistory` user/assistant role mapping.
  - `buildContentHistory` leading non-user turn dropping.
  - `buildContentHistory` trailing user turn dropping.
- `ChatViewModelTest` (27 tests):
  - Initial UI state empty checks.
  - Prompt editing and validation (empty prompt error).
  - Optimistic message addition and loading state toggling.
  - Voice result appending without auto-sending.
  - Voice error reporting as NonRetryable error.
  - Error message propagation and dismissal.
  - Retry logic for failed transmissions.
  - Retry ignores NonRetryable voice errors.
  - Clear chat action and history clearing.
  - In-flight request cancellation on clearChat prevents stale message insertion.
  - Concurrent onSend and retry ignored while active request in flight.
  - Multi-turn history passing to repository on second turn.
  - Multi-turn history passing on retry after failure.
  - Message ID uniqueness across turns.
  - Sealed `GeminiException` retryability mapping and typed exception classification.
- `BoldMarkdownTest` (6 tests):
  - Fenced code block preservation without markdown mangling.
  - Arithmetic asterisk preservation without accidental italicization (`2 * 3 * 4`).
  - Bullet item parsing for `* ` and `- `.
  - Bold, italic, and bold+italic styled spans.
  - Inline code span formatting.
- `ApiKeyProviderUnitTest` (7 tests):
  - Plaintext extraction from BuildConfig when DataStore is empty.
  - Re-encryption and key rotation lifecycle when BuildConfig key changes.
  - Corrupted ciphertext bad-tag safely reseeds from BuildConfig.
  - Missing Keystore key safely reseeds from BuildConfig.
  - Blank BuildConfig key safely returns null and clears stored preferences.
  - Mutation check verified (temporary removal of fingerprint check fails test).
  - Bare SHA-256 fingerprint storage justification.
- `UserPreferencesRepositoryTest` (7 tests):
  - Default theme mode (`SYSTEM`).
  - Setting and reading `LIGHT` and `DARK`.
  - Corrupted/malformed string fallback to `SYSTEM`.
  - Isolation between security keys and theme preference keys.
- `ChatHistoryRepositoryTest` (3 tests):
  - Flow wrapping of Room DAO message list.
  - Chronological ordering and entity mapping.
  - Message insertion and history clearance.
- `ChatMessageEntityTest` (2 tests):
  - Mapping between Room entity and domain `ChatMessage`.
  - Timestamp and role fidelity.

### B. Instrumented Tests (`.\gradlew connectedDebugAndroidTest`)
**Total: 20 tests, 0 failures, 0 errors, 0 skipped.**
- `CryptoManagerTest` (5 tests):
  - `encryptionDecryption_roundTrip_returnsOriginalPlaintext`: PASS
  - `encryption_usesFreshIvEachTime`: PASS (unique IV per encryption)
  - `decryption_corruptedCiphertext_throwsException`: PASS
  - `decryption_corruptedIv_throwsException`: PASS
  - `removeKey_thenDecrypt_throwsInvalidKeyException`: PASS
- `SecureApiKeyProviderTest` (3 tests):
  - `plaintext_neverExposedInPreferences`: PASS
  - `getApiKey_whenEncryptedInPreferences_decryptsSuccessfully`: PASS (legacy fingerprint backfill verified)
  - `getApiKey_whenFingerprintMatches_returnsDecryptedWithoutReseed`: PASS
- `ChatDaoTest` (2 tests):
  - `insertAndGetMessages_orderedChronologically`: PASS
  - `clearAllMessages_removesAllEntries`: PASS
- `ChatScreenTest` (9 Compose UI tests):
  - `emptyState_displaysGreeting`: PASS
  - `themeSelector_isDisplayedInTopAppBar`: PASS
  - `sendButton_disabledWhenPromptIsBlank`: PASS
  - `sendButton_enabledWhenPromptIsNotBlank`: PASS
  - `voiceButton_isDisplayedAndClickable`: PASS
  - `voiceError_displaysSnackbarWithNoRetryAction`: PASS
  - `loadingState_displaysThinkingIndicator`: PASS
  - `messages_displayedOnScreen`: PASS
  - `clearChatButton_visibleWhenMessagesPresent_opensConfirmationDialog`: PASS
- `ExampleInstrumentedTest` (1 test):
  - `useAppContext`: PASS

---

## 8. Security Audit Details

1. **`local.properties` Git Isolation**:
   - `git ls-files --error-unmatch local.properties` confirmed untracked.
   - `git check-ignore -v local.properties` verified ignored via `.gitignore:15`.
2. **Git History Secret Scan**:
   - Scanned all 26 commits via Gitleaks 8.30.1 (`gitleaks git . --config .gitleaks.toml -v`).
   - Total leaks found: **0**.
3. **Logcat Credential Sanitization**:
   - Monitored logcat during prompt transmission and network round-trips.
   - Total leaks found: **0**. No keys or decrypted credentials logged.
4. **Android Keystore AES-256-GCM Implementation**:
   - Master key alias `gemini_api_key_alias` generated inside Android Keystore (`AndroidKeyStore`).
   - Cipher: `AES/GCM/NoPadding` with 128-bit authentication tag.
   - Unique, secure random 12-byte IV generated on every encryption call.
   - Preferences DataStore stores only `encrypted_api_key_ciphertext`, `encrypted_api_key_iv`, and `build_key_fingerprint`.

---

## 9. Comprehensive Defects Log (Found & Resolved)

| # | Component | Defect Discovered | Root Cause | Resolution | Verification |
| :- | :--- | :--- | :--- | :--- | :--- |
| **1** | `GeminiRepositoryImpl.kt` & `ChatViewModel.kt` | Prior conversation turns were not sent to Gemini on subsequent requests | `GeminiRepository` only accepted a single `prompt: String`; `startChat(history)` was not utilized | Extended `GeminiRepository` with `generateText(prompt, history)`, implemented `startChat(buildContentHistory(history))` | Sent `"What is 2 + 2?"` followed by `"Multiply that by 10"`; Gemini answered `"4 * 10 = 40"` |
| **2** | `GeminiRepositoryImpl.kt` | Transient HTTP 503 / UNAVAILABLE errors on free tier | Google's free-tier `gemini-3.8-flash` capacity spikes during peak hours | Implemented retry (fixed 1s delay, 2 attempts) and automatic fallback to `gemini-3-flash-preview` | Live prompt generation succeeded on emulator across multiple turns |
| **3** | `SecureApiKeyProviderTest.kt` | Hand-seeded ciphertext without fingerprint caused re-encryption | Fingerprint matching logic expected a non-null fingerprint | Implemented legacy install semantics: trust decrypted ciphertext and backfill fingerprint | Verified on emulator; test passed cleanly |
| **4** | `CryptoManager.kt` | KeyStore eager init failed on JVM | `KeyStore.getInstance("AndroidKeyStore")` is unavailable on desktop JVM | Made `keyStore` lazy and methods `open` to allow `FakeCryptoManager` in JVM rotation tests | `ApiKeyProviderUnitTest` passed on JVM |
| **5** | `BoldMarkdown.kt` | Fenced code blocks and math asterisks mangled | Regex did not handle triple-backtick blocks and asterisks in math (`2 * 3 * 4`) | Added fenced code block extraction and word-boundary asterisks lookahead | 6/6 unit tests passed in `BoldMarkdownTest` |
| **6** | `GeminiRepositoryImpl.kt` | Offline error wrapped in `GoogleGenerativeAIException` mapped to Unknown | Exception cause chain was not inspected | Added cause chain traversal for `UnknownHostException` and network errors | Offline prompt produced "Network connection error" with Retry |
| **7** | `ChatScreenTest.kt` | Missing test for voice error snackbar Retry omission | Voice error UI path was not covered by Compose UI tests | Added `voiceError_displaysSnackbarWithNoRetryAction` Compose test | 9/9 tests passed in `ChatScreenTest` |
| **8** | `ChatViewModel.kt` | In-flight request race / Clear Chat repopulation | Unmonitored coroutine launches allowed late network completions to write assistant messages to Room and append to UI state after user clicked "Clear Chat" | Added `@Volatile private var activeRequestJob: Job?`, cancelled upon `clearChat()`, and guarded against concurrent `onSend`/`retry` while active | Verified via unit tests (`clearChat_whileRequestInFlight_cancelsJobAndPreventsStaleMessages`) and on emulator |
| **9** | `ContentGenerator.kt` | Model cache retaining stale client on key rotation | `DefaultContentGenerator.getOrCreateModel` checked cached model existence before verifying API key match, returning old model instance bound to retired key | Added synchronization and cleared cached model references when `cachedApiKey != apiKey` | Verified via model re-instantiation inspection |
| **10** | `ApiKeyProvider.kt` | Blank BuildConfig key falling back to stale ciphertext | When `local.properties` key was wiped or removed, provider fell back to decrypting stored ciphertext instead of purging credentials | Added explicit check `(buildKey.isBlank() && !ciphertext.isNullOrBlank())` to purge DataStore preferences and return null | Verified by unit test `blankBuildKey_returnsNullAndClearsStoredPreferences` |
| **11** | `GeminiRepositoryImpl.kt` | Fallback model omitted on model-specific quota (429) & missing 400 error | Line 83 only fell back on `Overloaded` (503); free-tier per-model rate limit (429) or 404 blocked the user. 400 Bad Request was also unmapped | Added `GeminiException.BadRequest` (400) and expanded fallback to trigger on `Overloaded`, `QuotaExceeded`, or `ModelNotFound` to switch to `gemini-3-flash-preview` | Verified on emulator during multi-turn test and 2 new unit tests |

---

## 10. Known Limitations

1. **Client-Side API Key in APK (`BuildConfig`)**:
   The Gemini API key is supplied via `local.properties` and compiled into `BuildConfig.GEMINI_API_KEY`. While stored encrypted-at-rest in DataStore via KeyStore AES-256-GCM, any attacker with physical access to the APK file can decompile bytecode (e.g. via Jadx or apktool) to recover the initial build-time key literal. Production deployments should proxy requests through an authenticated backend server or use Firebase AI Logic with App Check.
2. **StrongBox Hardware Security Not Enforced**:
   `CryptoManager` uses the standard `AndroidKeyStore` provider. StrongBox Keymaster (`setIsStrongBoxBacked(true)`) is not requested, allowing compatibility across all standard devices and emulators that lack dedicated tamper-resistant Secure Element hardware.
3. **Single Global Conversation History**:
   The Room database maintains a single sequential timeline of messages. Multiple distinct conversation sessions, thread branching, and cloud synchronization are not implemented in this version.
4. **Virtual Emulator Audio Constraints**:
   While the speech recognition intent (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) and error handling are fully verified, live acoustic voice-to-text dictation requires an active speech engine and physical microphone input configured on the host or physical device.

---

## 11. Final Verification Sign-Off

The application has been verified through automated unit tests, instrumented device tests, lint checks, and runtime emulator execution with live API credentials.

Every verifiable capability has been empirically confirmed:
- **Build**: PASS
- **Unit Tests (73)**: PASS
- **Instrumented Tests (20)**: PASS
- **Gemini Runtime**: PASS (Empirical assistant responses received and rendered)
- **Gemini Multi-Turn**: PASS (Demonstrated context retention: "What is 2 + 2?" -> "2 + 2 = 4" then "Multiply that by 10" -> "4 * 10 = 40")
- **Voice Error Path**: PASS (Snackbar with NO Retry action, non-resending behavior)
- **Voice Intent**: PASS (System SpeechRecognizer intent launched dialog on device)
- **Room Persistence**: PASS (Survived `am force-stop` process kill)
- **DataStore Persistence**: PASS (Theme survived `am force-stop` process kill)
- **Release APK Runtime**: PASS (Minified release build smoke-tested with live Gemini generation)
- **Security Audit**: PASS (Zero credentials tracked, committed, or logged; Gitleaks clean)
- **Lint**: PASS (`lintDebug` and `lintRelease` 0 errors, 0 warnings)
