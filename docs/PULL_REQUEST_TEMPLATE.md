## <ROLL_NO> – Lab Assignment 1: Gemini Compose Chat

### Implemented features
- [x] **API key security** – key read from `local.properties` / `GEMINI_API_KEY` env var into `BuildConfig`; never hardcoded; `local.properties.example` added; pre-commit secret-scanning hook
- [x] **Encryption at rest** – AES-256-GCM key generated in Android Keystore (`KeyGenParameterSpec`); only ciphertext stored in DataStore; decrypted in memory only when `GenerativeModel` is created; never logged/shown
- [x] **R8** – `isMinifyEnabled = true` + `isShrinkResources = true` for release
- [x] **LazyColumn chat** – Material 3 user/Gemini bubbles, stable keys, `contentType`, auto-scroll
- [x] **State hoisting** – `ChatUiState` exposed as `StateFlow`, collected with `collectAsStateWithLifecycle()`; composables are stateless
- [x] **Responsive** – `WindowSizeClass`: bottom-sheet settings on phones, permanent settings pane on tablets, max-width conversation column
- [x] **Loading / error** – progress bubble while waiting; error card with Retry / Dismiss
- [x] **Dark mode** – System / Light / Dark + Material You dynamic colour
- [x] **Voice input** – `RecognizerIntent` via `rememberLauncherForActivityResult`
- [x] **Persistence** – preferences in Preferences DataStore; chat history in Room (survives restarts)
- [x] **Tests** – `ChatViewModelTest` (coroutines-test + fakes), `ChatScreenTest` (`createComposeRule`)

### Screenshots
| Phone – light | Phone – dark | Loading | Error | Tablet | Voice |
|---|---|---|---|---|---|
| <img src="" width="180"> | <img src="" width="180"> | <img src="" width="180"> | <img src="" width="180"> | <img src="" width="300"> | <img src="" width="180"> |

### Secret check
- [x] `git log -p | grep -c AIza` → 0
- [x] `local.properties` not in the diff
