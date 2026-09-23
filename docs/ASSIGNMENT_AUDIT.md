# Assignment requirement audit

Student: Merant Patel · N077
Source: Assignment-1.docx, read completely before source edits and re-read line by line during the final audit.
Branch: N077_Assignment1. Upstream baseline: 8d01c6862630a509e379d395220598e6f640e9ef. Original commits and authors are preserved.

All Kotlin paths below are relative to app/src/main/java/com/fahim/geminiApiComposeStarter.

| Requirement | Implementing files | Verification |
| --- | --- | --- |
| Extend the original starter and inspect its architecture | MainActivity.kt; ui/chat/*; data/GeminiRepository*; existing Gradle files | Existing package, application ID, theme and repository abstraction retained; original history preserved |
| Roll-number branch and repository-local author | Git branch/config | N077_Assignment1; Merant Patel; no global identity change |
| Local API key exposed through BuildConfig | app/build.gradle.kts | Real configured key loaded without output; direct API verification succeeded |
| Environment fallback when local key is absent/blank | app/build.gradle.kts | Explicit local-then-environment-then-empty precedence |
| Ignore local credentials and provide example | .gitignore; local.properties.example | git check-ignore; scripts/secret_audit.py; example contains only placeholder |
| AES-256-GCM and KeyGenParameterSpec | data/SecureApiKeyStore.kt | Instrumented encryption round-trip, persisted plaintext absence, key rotation and store recreation |
| Persist ciphertext only; decrypt for model creation | data/SecureApiKeyStore.kt; data/GeminiRepositoryImpl.kt | IV/ciphertext/tag envelope; model-time decryption; no key logging or UI display |
| R8 release minification | app/build.gradle.kts | assembleRelease and minifyReleaseWithR8 executed |
| Explain client-side limits and production protection | README.md | BuildConfig exposure, memory inspection, backend proxy, restricted keys and Firebase App Check documented |
| Exact Gemini model, without substitutions | data/GeminiRepositoryImpl.kt | Fixed MODEL_NAME = gemini-3.6-flash; direct Google response reported matching modelVersion |
| Material 3 LazyColumn bubbles | ui/chat/ChatScreen.kt | Compose bubble test; user/Gemini visual roles |
| Stable keys and auto-scroll | data/ChatDatabase.kt; ui/chat/ChatScreen.kt | Room-generated IDs; items key/contentType; latest-ID/loading LaunchedEffect |
| ChatUiState, StateFlow and lifecycle collection | ui/chat/ChatUiState.kt; ui/chat/ChatViewModel.kt; MainActivity.kt | 12 ViewModel tests; collectAsStateWithLifecycle; callbacks hoisted |
| WindowSizeClass and responsive layouts | MainActivity.kt; ui/chat/ChatScreen.kt | Calculated width/height classes; bounded reading width; compact-height input; expanded Compose test |
| Loading and safe errors | ui/chat/ChatScreen.kt; ui/chat/ChatViewModel.kt; data/GeminiRepositoryImpl.kt | Progress, disabled duplicate sends, safe Snackbar, timeout, exception handling and prompt restoration tests |
| Empty-input validation | ui/chat/ChatViewModel.kt; ui/chat/ChatScreen.kt | Unit and UI tests |
| System/dark mode and dynamic color | ui/theme/Theme.kt; MainActivity.kt; data/UserPreferences.kt | System/light/dark selection, DataStore theme recreation test |
| RecognizerIntent and result launcher | ui/chat/ChatScreen.kt | Voice callback test; recognition-result/cancellation integration tests; unavailable activity/security handling |
| Preferences DataStore | data/UserPreferences.kt | Saved theme observed by StateFlow and read by recreated preference repository |
| Room history, DAO, entity and database | data/ChatDatabase.kt; ChatApplication.kt | On-disk database close/reopen and persistent clear instrumentation test |
| Clear-history interaction | ui/chat/ChatScreen.kt; ui/chat/ChatViewModel.kt | Confirmation dialog; failure and busy-state guard tests; actual clear and process-restart verification |
| Off-main-thread I/O and responsive interactions | data/GeminiRepositoryImpl.kt; data/SecureApiKeyStore.kt; Room/DataStore | Dispatchers.IO; suspend DAO; DataStore async I/O; LazyColumn stable keys/content types |
| ViewModel tests with fake GeminiRepository | app/src/test/.../ChatViewModelTest.kt | 12 tests passed |
| Compose tests using createComposeRule | app/src/androidTest/.../ChatScreenTest.kt | 14/14 emulator tests passed, including real Gemini and voice intent results; final targeted test also passed |
| README setup/security/test instructions | README.md | Setup, fallback, Keystore, limitations, production advice, build, unit/UI tests and runtime checks covered |
| No secrets in source, diffs or history | scripts/secret_audit.py | Full working-file/index/diff/reachable-history audit passed; repeated before commits and against the published PR diff |
| Feature checklist, screenshots and PR | docs/screenshots; GitHub PR | Published in PR #84: https://github.com/ifahimkhan/GeminiApiComposeStarter/pull/84 |

## Verification results

Verified on 23 September 2026 with the Pixel_7 emulator, Android 15/API 35, and Gradle 9.3.1.

- **BUILD SUCCESSFUL** for debug APK, instrumentation APK, R8-minified release APK, unit tests and Android lint after the final system-bar theme fix.
- **12/12 ViewModel unit tests passed**, zero failures/errors/skips.
- **14/14 instrumentation tests passed**, zero failures/errors/skips: seven Compose tests, two voice-intent integration tests, two secure-storage/preference tests, one Room persistence test, one application-context test and one real Gemini test.
- After the final system-bar fix, only the affected live/theme screenshot test was repeated against the rebuilt APK: **OK (1 test)**. Existing unrelated test results were retained.
- The Google endpoint returned **modelVersion: gemini-3.6-flash**. The app also received and persisted real responses through its Keystore-backed repository. There is no model override/fallback.
- The real app launched, displayed bubbles and loading progress, retained conversation and dark theme after force-stop/relaunch, displayed empty-input validation, and adapted to landscape and a 1920 x 1200 / 240 dpi tablet-width configuration. Emulator display settings were restored.
- The real clear-history confirmation removed the test conversation; another force-stop/relaunch confirmed it remained cleared.
- Network/repository failures, storage failures, missing-key handling and duplicate-send protection were verified through deterministic unit/UI tests. An actual offline network transition was not separately exercised.
- Screenshots under screenshots/ are actual emulator captures, not mockups. The conversation content came from the real Gemini API. Loading capture asserts the indicator is displayed before capture; settled theme screenshots include system bars.
- Lint passed with no errors. Remaining warnings concern available dependency/target-SDK updates and unused starter resources; no warning baseline or suppression was added.
- The secret audit checked actual configured-key bytes, common Google API-key patterns, tracked/untracked nonignored files, index/diffs and all reachable Git commits/blobs. local.properties is ignored and not staged. No real key was displayed or committed.

## Verification limits

- RecognizerIntent launch, successful text delivery and cancellation passed with Espresso intent results. Physical microphone/acoustic transcription needs one spoken phrase on a speech-enabled device; no claim of that manual test is made.
- Android Studio was located and launched, but exposed no usable window to automation. Layout Inspector recomposition counts were therefore not measured. The implementation uses lazy items with stable keys/content types and background I/O, and README records the remaining inspector procedure.
- The entire DOCX text, including header/footer/note parts, was extracted and read. The bundled DOCX renderer could not run because bundled LibreOffice was unavailable; no assignment document was modified.
- Teams is explicitly deferred. No Turn In, Submit, attachment or comment action was performed.
