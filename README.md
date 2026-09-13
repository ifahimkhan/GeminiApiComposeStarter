# Gemini API Jetpack Compose Starter - Assignment 1

**Student Roll Number:** C015

## Features Implemented
- **Android Keystore (Mandatory Security):** Encrypts API keys at rest using AES-256-GCM via hardware-backed Keystore (`CryptoManager`). In-memory decryption only when instantiating `GenerativeModel`.
- **Speech-to-Text Input:** Voice queries via `RecognizerIntent` launched with `rememberLauncherForActivityResult`.
- **Material 3 UI:** Conversation rendered with `LazyColumn`, customized user/model message bubbles, auto-clearing input, and `CircularProgressIndicator` loading states.

## Setup & Security Configuration

### API Key Setup
Place your Gemini API key in `local.properties`:
```properties
GEMINI_API_KEY=your_api_key_here