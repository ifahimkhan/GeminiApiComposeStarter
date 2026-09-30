# Gemini Chat (Jetpack Compose) — Aiyana Agrawal, N143

An Android chat app built on the Gemini Jetpack Compose starter.
You can type or speak a question and get replies from Google Gemini.
Chats are saved on the device, and the API key is encrypted at rest.

## Features

| Area | What was implemented |
|---|---|
| Chat UI | `LazyColumn` of Material 3 chat bubbles with stable keys and auto-scroll |
| State | `ChatUiState`, `StateFlow`, and `collectAsStateWithLifecycle()` |
| Responsive Layout | `WindowSizeClass` for phone, tablet and landscape layouts |
| Loading & Errors | `CircularProgressIndicator`, Snackbar and empty-field validation |
| Dark Mode | Dark mode switch with saved preference |
| Voice Input | `RecognizerIntent` with `rememberLauncherForActivityResult` |
| Chat History | Room database with history surviving app restarts |
| Preferences | Dark mode saved using Preferences DataStore |
| Security | API key from `local.properties`, encrypted using Android Keystore AES-256-GCM |
| Release | R8 minification and resource shrinking enabled |
| Tests | ViewModel unit tests and Compose UI tests |

## 1. Where to put the API key

Add the Gemini API key to `local.properties`:

```text
GEMINI_API_KEY=your_api_key_here