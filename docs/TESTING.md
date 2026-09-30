# Testing & Verification Guide

This document details the test suites, verification procedures, and runtime validation performed on `GeminiApiComposeStarter`.

---

## 1. Automated Test Suite Summary

The automated test suite contains **93 verified automated tests** with a 100% pass rate.

| Category | Suite | Test Count | Status | Environment |
| :--- | :--- | :---: | :---: | :--- |
| **JVM Unit Tests** | `testDebugUnitTest` | **73** | **PASS** | Local JVM (Coroutines Test Dispatcher) |
| **Connected Android Tests** | `connectedDebugAndroidTest` | **20** | **PASS** | Android Emulator (`Pixel_10_Pro`, API 36) |
| **Static Analysis** | `lintDebug` & `lintRelease` | **0 errors / 0 warnings** | **PASS** | AGP Lint Analyzer |
| **Total Automated Tests** | | **93** | **PASS** | |

---

## 2. Test Commands

### Running JVM Unit Tests
```bash
.\gradlew.bat testDebugUnitTest --rerun
```
*Executes all 73 JVM unit tests without relying on task output caching.*

### Running Connected Device Tests
```bash
.\gradlew.bat connectedDebugAndroidTest
```
*Runs all 20 instrumented Android tests on the attached emulator or physical device.*

### Assembling Debug and Release APKs
```bash
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```
*Compiles the debug APK and the R8-obfuscated, minified release APK.*

### Running Android Lint Analysis
```bash
.\gradlew.bat lintDebug lintRelease
```
*Validates static code correctness, Compose performance guidelines, and release readiness.*

---

## 3. Test Breakdown by Component

### A. JVM Unit Tests (73 Tests)

1. **`GeminiRepositoryTest` (21 tests)**:
   - Missing and blank API key error mapping.
   - HTTP 400 Bad Request error translation (`GeminiException.BadRequest`, non-retryable).
   - HTTP 401 Unauthorized / Invalid API Key translation (`GeminiException.Authentication`, non-retryable).
   - HTTP 403 Forbidden / Permission Denied translation (`GeminiException.PermissionDenied`, non-retryable).
   - HTTP 404 Model Not Found translation (`GeminiException.ModelNotFound`).
   - HTTP 429 Quota Exceeded / Rate Limit translation (`GeminiException.QuotaExceeded`).
   - HTTP 503 Service Unavailable / Overloaded translation (`GeminiException.Overloaded`).
   - Network timeout, host resolution failure, and cause chain traversal.
   - Transient 503 retry behavior (up to 2 attempts with delay).
   - Fallback model activation on primary model capacity spike (503), quota exhaustion (429), or 404.
   - Immediate abortion of retry loop on permanent authentication/client errors.
   - Content history mapping (`buildContentHistory`): user/assistant role alternating, leading assistant turn removal, and trailing user turn dropping.

2. **`ChatViewModelTest` (27 tests)**:
   - Initial UI state and greeting verification.
   - Empty prompt rejection and validation.
   - Optimistic message addition to UI state.
   - Voice transcription result appending without unintended auto-send.
   - Non-retryable error handling for voice failure.
   - Transient error snackbar presentation and retry action execution.
   - In-flight request cancellation on `clearChat()` preventing late stale message writes.
   - Concurrent `onSend` and `retry` guarding while a request is active.
   - Multi-turn conversation history passing to repository on subsequent turns.
   - Preservation of history during retry without duplicate user messages.
   - Stable unique message identifiers.
   - Typed `GeminiException` retryability mapping.

3. **`BoldMarkdownTest` (6 tests)**:
   - Fenced code block preservation without markdown mangling.
   - Arithmetic asterisk preservation (`2 * 3 * 4`) without accidental italicization.
   - Bulleted list formatting (`* ` and `- `).
   - Bold, italic, and bold-italic styled text annotations.
   - Inline monospace code span formatting.

4. **`ApiKeyProviderUnitTest` (7 tests)**:
   - Plaintext extraction from `BuildConfig` when DataStore is empty.
   - Key rotation lifecycle: re-encryption and reseeding when `BuildConfig` fingerprint changes.
   - Corrupted ciphertext recovery via safe reseeding.
   - Missing Keystore key recovery via safe reseeding.
   - Blank `BuildConfig` key detection and DataStore credential purging.
   - SHA-256 fingerprint storage verification.

5. **`UserPreferencesRepositoryTest` (7 tests)**:
   - Default theme mode (`SYSTEM`).
   - Setting and reading `LIGHT` and `DARK` theme preferences.
   - Corrupted/unrecognized preference string fallback to `SYSTEM`.
   - Complete isolation between theme preference keys and cryptographic storage keys.

6. **`ChatHistoryRepositoryTest` (3 tests)**:
   - Reactive Flow observation of Room DAO queries.
   - Deterministic chronological ordering (`timestamp ASC, id ASC`).
   - Message insertion and history clearance operations.

7. **`ChatMessageEntityTest` (2 tests)**:
   - Bidirectional mapping between Room `ChatMessageEntity` and domain `ChatMessage`.
   - Participant role and timestamp integrity.

---

### B. Connected Android Device Tests (20 Tests)

- **`ChatScreenTest` (9 Compose UI tests)**:
  - Initial empty state greeting display.
  - Theme selection button presence in top app bar.
  - Send button disabled when prompt is blank or whitespace.
  - Send button enabled when prompt contains text.
  - Voice input button presence and click response.
  - Voice error snackbar display with no retry action.
  - Thinking indicator display during `isLoading` state.
  - Message bubble rendering and scroll container stability.
  - Clear chat action and confirmation dialog display.

- **`CryptoManagerTest` (5 KeyStore security tests)**:
  - Round-trip encryption and decryption fidelity.
  - Unique, fresh 12-byte IV generation for every encryption call.
  - Authentication tag verification: corrupted ciphertext throws exception.
  - Corrupted IV throws exception.
  - Key removal and invalid alias handling.

- **`SecureApiKeyProviderTest` (3 security integration tests)**:
  - Verification that plaintext key is never stored in Preferences DataStore.
  - Decryption of existing encrypted key from DataStore on launch.
  - Matching fingerprint avoids unnecessary reseeding.

- **`ChatDaoTest` (2 Room database tests)**:
  - Message insertion and chronological query retrieval.
  - Clear table operation removes all messages.

- **`ExampleInstrumentedTest` (1 test)**:
  - Application package context verification.

---

## 4. Emulator & Runtime Smoke Test Scenarios

The following end-to-end scenarios were verified on an active Android emulator (`Pixel_10_Pro`, API 36, ABI `x86_64`):

1. **Cold Launch**:
   App boots cleanly, loads theme preference from DataStore, checks `AndroidKeyStore`, and renders the welcome banner.

2. **Live Gemini Request (Single-Turn)**:
   Submitted `"What is 2 + 2?"`. Received live response `"2 + 2 = 4"` from the Gemini API.

3. **Multi-Turn Conversational Memory**:
   Immediately submitted `"Multiply that by 10"`. The model understood the implicit reference to 4 and answered `"4 * 10 = 40"`.

4. **Process Death & History Restoration**:
   Sent messages, executed `adb shell am force-stop com.fahim.geminiApiComposeStarter`, then relaunched the application. All previous conversation bubbles were restored in chronological order from Room SQLite.

5. **Clear Chat Lifecycle**:
   Tapped "Clear chat history" in the top bar. Confirmed via dialog. Messages and Room database were completely cleared, and welcome screen reappeared. Relaunched to verify empty state persisted.

6. **Theme Persistence**:
   Toggled theme between `SYSTEM`, `LIGHT`, and `DARK`. Force-stopped and relaunched; the selected theme remained active.

7. **Voice Input Intent**:
   Tapped the microphone icon. System `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` dialog (`"Speak to Gemini..."`) opened cleanly and dismissed upon pressing Back without errors.

8. **R8 Minified Release Build**:
   Installed `app-release.apk` (built with `isMinifyEnabled = true`). Launched the app, verified Keystore decryption, Room persistence, and executed a live Gemini request (`"What is 5 + 5?"` -> `"5 + 5 = 10"`).

---

## 5. Environment & Hardware Limitations

1. **Virtual Emulator Audio Input**:
   While the speech recognition intent (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) and error handling paths are fully tested, live acoustic voice-to-text dictation requires a physical microphone and active speech engine configured on the host device.
2. **Third-Party API Rate Limits**:
   The Google Gemini API free-tier enforces a rate limit (20 requests per minute). The app implements retry logic and fallback model switching (`gemini-3-flash-preview`) to mitigate capacity spikes.
3. **Client-Side Key Protection Boundary**:
   The key is encrypted at rest in DataStore via KeyStore AES-256-GCM. However, because the initial key is compiled into `BuildConfig.GEMINI_API_KEY`, decompiling the APK bytecode can reveal the build constant. In production, credentials should be proxied through an authenticated backend service.
