# Gemini AI Chat Application - Android Assignment 1

An Android application built with **Jetpack Compose** and **Material 3** that integrates Google's **Gemini AI API**, persistent chat history via **Room Database**, theme preferences via **Preferences DataStore**, **Voice Input**, and **AES-256-GCM KeyStore Encryption** for API key security.

---

## 1. Project Overview

This project was developed for **Mobile Application Development Lab Assignment 1**. It transforms a basic starter application into a full-featured, responsive, secure, and persistent AI chat assistant. Users can type or speak prompts to Gemini AI, view real-time responses, toggle between light and dark modes, and access persistent chat history surviving application restarts.

---

## 2. Implemented Features

- 💬 **Conversation UI**: Material 3 chat bubbles (User end-aligned in Primary Container; Gemini start-aligned in Secondary Container) using `LazyColumn` with stable item keys.
- 📜 **Room Database Chat History**: All user queries and Gemini responses are persisted asynchronously using Room DAO (`ChatDao`, `ChatMessageEntity`).
- 🎨 **Preferences DataStore**: Theme preference (Light/Dark mode) persists across app restarts.
- 🎤 **Voice-to-Text Input**: Speech recognition powered by Android `RecognizerIntent` with microphone button and fallback error handling.
- 🔐 **AES-256-GCM Keystore Encryption**: Secure API key encryption using `AndroidKeyStore` Master Key. Decryption occurs strictly in memory when initializing `GenerativeModel`.
- ⚡ **State Hoisting & Reactive UI**: State flow exposed via `StateFlow` and collected using `collectAsStateWithLifecycle()`.
- 📱 **Responsive Design**: Auto-centering and max-width layout bounds supporting small phones, large tablets, portrait, and landscape.
- 🧪 **Unit & Instrumentation Testing**: Comprehensive unit tests (`ChatViewModelTest`) using `kotlinx-coroutines-test` and Compose UI tests (`ChatScreenTest`).
- 📦 **R8 / Minification**: Enabled in `release` build type with custom ProGuard rules.

---

## 3. How to Open and Run the Project

1. Open **Android Studio** (2024.1 or newer recommended).
2. Select **File -> Open...** and navigate to the project directory: `C008_Nishit_Assignment1`.
3. Wait for Gradle Sync to complete.
4. Select `app` run configuration and run on an Android Emulator or connected device running Android 7.0 (API 24) or higher.

---

## 4. API Key Configuration (`GEMINI_API_KEY`)

To use Gemini AI features:
1. Obtain a Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
2. Create a `local.properties` file in the root directory (if not present).
3. Add your key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
4. Build and run the app. `app/build.gradle.kts` automatically reads `GEMINI_API_KEY` from `local.properties` (or environment variable `System.getenv("GEMINI_API_KEY")`) and exposes it through `BuildConfig.GEMINI_API_KEY`.

---

## 5. Security & Git Guidelines

- **`local.properties` is strictly Git-ignored**: The `.gitignore` file contains `local.properties` to ensure secrets are never committed to version control.
- **`local.properties.example`**: Provided as a template for developers with placeholder values.
- **No Hardcoded Keys**: The source code contains zero hardcoded API keys or plaintext secrets.

---

## 6. AES-256-GCM Android Keystore Encryption Flow

1. On first app launch, `ApiKeyManager` initializes a 256-bit AES key inside the hardware-backed **`AndroidKeyStore`** using `KeyGenParameterSpec` configured with `GCM` mode and `NoPadding`.
2. The initial API key from `BuildConfig` is encrypted using `Cipher.getInstance("AES/GCM/NoPadding")`.
3. The resulting ciphertext and Initialization Vector (IV) are encoded in Base64 and saved in `SharedPreferences`.
4. When `GeminiRepositoryImpl` needs to make an API call, `ApiKeyManager.getDecryptedApiKey()` decrypts the key using the Keystore SecretKey and IV **strictly in memory**.
5. The decrypted key is passed to `GenerativeModel` constructor and is never logged, printed, or exposed in the UI.

---

## 7. Storage Architecture

### A. Room Database (Chat History)
- **Entity**: `ChatMessageEntity` (`id`, `text`, `sender`, `timestamp`).
- **DAO**: `ChatDao` (`getAllMessages(): Flow<List<ChatMessageEntity>>`, `insertMessage()`, `clearAllMessages()`).
- **Database**: `ChatDatabase` (Singleton instance).
- Operations execute off the main thread via Coroutines `Dispatchers.IO`.

### B. Preferences DataStore
- **Repository**: `PreferencesRepository`.
- Persists user dark theme preference (`is_dark_mode` boolean key) using `androidx.datastore.preferences`.

---

## 8. Voice Input Implementation

- Built using Android's built-in `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- Launched cleanly in Compose using `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- Places spoken text directly into the prompt text field.
- Displays a `Toast` message gracefully if speech recognition service is unavailable on the host device.

---

## 9. Testing & How to Run Tests

### A. Unit Tests (`ChatViewModelTest.kt`)
Tests state hoisting, prompt validation, successful API handling, network error handling, and clearing history using `FakeGeminiRepository`, `FakeChatDao`, and `FakePreferencesRepository`.

**Run Unit Tests command:**
```bash
./gradlew app:testDebugUnitTest
```

### B. Compose UI Tests (`ChatScreenTest.kt`)
Tests rendering of user and Gemini chat bubbles, text formatting, and sender labels using `createComposeRule()`.

**Run UI Tests command (Requires connected device/emulator):**
```bash
./gradlew app:connectedDebugAndroidTest
```

---

## 10. Release Build & R8 Minification

Release builds have R8 minification and resource shrinking enabled in `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```
Custom keep rules in `app/proguard-rules.pro` preserve Room generated code and Google Generative AI / Ktor SDK classes while stripping unused code and obfuscating application logic.

---

## 11. Security Limitations & Production Recommendations

> [!WARNING]
> **Client-Side Security Limitations**: While AES-256-GCM encryption with Android Keystore prevents casual key extraction from static APK decompilation or unencrypted local storage, client-side encryption **cannot completely protect an API key** from a determined attacker with root access or memory inspection tools on an Android device.

### Production Best Practices:
1. **Backend Proxy**: Move all Gemini API requests to a secure backend server (Cloud Functions, Ktor Server, Firebase Backend) so the API key never touches the Android client.
2. **Firebase App Check**: Use Firebase App Check to attest device legitimacy before granting access to AI services.
3. **API Key Restrictions**: Restrict the Gemini API key in Google Cloud Console by package name and SHA-1 certificate fingerprint.

---

## 12. Architecture Diagram & Flow

```
+-------------------------------------------------------------+
|                      Jetpack Compose UI                     |
|          (ChatScreen, ChatBubble, PromptBar)                 |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                        ChatViewModel                        |
|   (StateFlow<ChatUiState>, collectAsStateWithLifecycle)     |
+---------------+--------------+------------------------------+
                |              |
                v              v
+---------------+--+        +--+------------------------------+
| Room Database    |        | Preferences DataStore           |
| (ChatHistory)    |        | (Dark Mode Preference)          |
+------------------+        +---------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                      GeminiRepository                       |
|                   (GeminiRepositoryImpl)                    |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                      ApiKeyManager                          |
|         (AndroidKeyStore AES-256-GCM Encryption)           |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                     Google Generative AI                    |
|             (GenerativeModel gemini-1.5-flash)              |
+-------------------------------------------------------------+
```
