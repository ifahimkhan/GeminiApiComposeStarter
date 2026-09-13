# Gemini AI Chat - Android Mobile Application Development Lab

An Android application built with Kotlin, Jetpack Compose, and Material 3 that integrates Google's Gemini API for interactive AI text chat, room database persistence, AES-256-GCM Keystore security, voice input, and theme customization.

---

## 📌 Project Purpose

This project is a Mobile Application Development lab assignment demonstrating modern Android development practices, including declarative UI with Compose, StateFlow architecture, offline message persistence with Room, preference management with DataStore, voice recognition, unit/UI testing, and secure API key handling using Android Keystore.

---

## ✨ Features

- **Gemini AI Integration**: Interactive text generation using `gemini-1.5-flash`.
- **Material 3 Chat UI**: User & Gemini distinct message bubbles with `LazyColumn` and automatic scroll-to-latest message.
- **Voice Input (Speech-to-Text)**: Speak prompts using `RecognizerIntent` and edit recognized speech before sending.
- **Room Database Persistence**: Full conversation history persisted across app restarts using Room (`ChatMessageEntity`, `ChatMessageDao`, `AppDatabase`).
- **Preferences DataStore**: Dynamic Dark Mode theme preference stored using Preferences DataStore.
- **Android Keystore AES-256-GCM Security**: Hardware-backed AES-256-GCM encryption for storing sensitive API keys in memory.
- **Adaptive & Responsive Layout**: Responsive paddings and layouts supporting phone, foldable, and tablet screen widths.
- **Loading & Error Handling**: Global `CircularProgressIndicator`, non-blocking async network calls, and `Snackbar` with Retry capability.
- **Unit & UI Testing**: Comprehensive unit tests for `ChatViewModel` (`kotlinx-coroutines-test`) and Compose UI tests (`createComposeRule`).

---

## 🛠 Technologies & Libraries Used

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose, Material 3
- **Architecture**: MVVM / Single-Screen Flow (`ChatViewModel`, `ChatUiState`, `StateFlow`)
- **Lifecycle**: `collectAsStateWithLifecycle()`
- **Persistence**: Room Database 2.6, Preferences DataStore 1.1
- **Security**: Android Keystore (`KeyGenParameterSpec`), AES-256-GCM Cipher
- **API**: Google Generative AI SDK (`com.google.ai.client.generativeai:generativeai:0.9.0`)
- **Testing**: JUnit 4, `kotlinx-coroutines-test`, Compose Test Rule

---

## 🔑 Gemini API Key Setup & `local.properties` Security

### Where to Place Your API Key
1. Create a file named `local.properties` in the root directory of the project if it does not already exist.
2. Add your Gemini API Key:
   ```properties
   GEMINI_API_KEY=AIzaSy...your_actual_api_key_here
   ```
3. A template file `local.properties.example` is provided in the repository root for reference.

### `local.properties` Security & Git Protection
- `local.properties` is strictly added to `.gitignore`.
- It is **never** committed to Git or version control systems.
- The build script (`app/build.gradle.kts`) reads `GEMINI_API_KEY` from `local.properties` or environment variables at build time and exposes it via `BuildConfig.GEMINI_API_KEY`.

---

## 🔐 Keystore Encryption Flow & Security Considerations

### Encryption Flow
1. **At App Launch**: `MainActivity` reads `BuildConfig.GEMINI_API_KEY`.
2. **Encryption**: `CryptoManager` checks or generates a 256-bit AES key inside the hardware-backed **Android Keystore** (`AndroidKeyStore`).
3. **Cipher**: The raw API key is encrypted using `AES/GCM/NoPadding`.
4. **Memory Decryption**: `GeminiRepositoryImpl` decrypts the ciphertext in memory *only* when initializing the `GenerativeModel` instance.
5. **No Plaintext Storage**: The API key is **never** saved in plaintext in Room, DataStore, logs, Toasts, or XML files.

### ⚠️ Limitations of Client-Side API Key Protection
> **Important Security Notice**: While Android Keystore AES-256-GCM encryption protects API keys against static reverse-engineering of local database files, client-side encryption **cannot** provide complete protection against advanced runtime memory inspection or reverse-engineering of client binaries on rooted devices.
>
> **Production Recommendation**: Production applications should route requests through a secure backend proxy server, enforce App Check attestation, and apply restricted API key quotas rather than making direct client-to-API calls.

---

## 🚀 How to Run the Application

1. Open the project in **Android Studio**.
2. Create/update `local.properties` in the root folder with your valid `GEMINI_API_KEY`.
3. Perform a Gradle Sync (`File -> Sync Project with Gradle Files`).
4. Select an Android Emulator or connected physical device (Android 8.0 / API 24 or higher).
5. Click **Run 'app'** (`Shift + F10`).

---

## 🧪 How to Run Tests

### Unit Tests
Run unit tests for `ChatViewModel` using Gradle:
```bash
./gradlew :app:testDebugUnitTest
```
Or right-click `ChatViewModelTest.kt` in Android Studio and select **Run 'ChatViewModelTest'**.

### Compose UI Tests
Run UI tests using Gradle:
```bash
./gradlew :app:connectedAndroidTest
```
Or right-click `ChatScreenTest.kt` in Android Studio and select **Run 'ChatScreenTest'**.
