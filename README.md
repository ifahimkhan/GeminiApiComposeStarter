# Gemini AI Jetpack Compose Chatbot

**Student Roll Number:** `N142`  
**Course:** Mobile Application Development (Subject Code: `702AI0E002`)  
**Institution:** School of Technology Management & Engineering, SVKM's NMIMS, Mumbai  
**Lab Assignment:** Assignment 1  

---

## 📱 Project Overview

This project is a modern, responsive, and secure Android chatbot application built with **Jetpack Compose**, **Material 3**, and the **Google Generative AI SDK (Gemini)**. It expands upon the starter architecture with persistent chat history (Room SQLite), preferences management (Preferences DataStore), hardware-backed credential encryption (Android Keystore AES-256-GCM), voice input (Speech-to-Text), and responsive layouts.

---

## ✨ Features Implemented

### 1. 💬 Modern Chat UI (Jetpack Compose & Material 3)
- **LazyColumn Conversation:** Real-time chat bubbles distinguishing User messages (aligned right in `primaryContainer`) and Gemini responses (aligned left in `surfaceVariant`).
- **Stable Item Keys:** Fast, jitter-free list rendering keyed by unique message IDs.
- **Auto-Scroll:** Automatically scrolls smoothly to the latest message as new replies arrive.
- **State Hoisting:** Pure, unidirectional data flow via `StateFlow` from `ChatViewModel` collected with `collectAsStateWithLifecycle()`.
- **Responsive Layout:** Adapts dynamically across phone portrait, landscape, and large tablet screens using width constraints.
- **Empty State & Suggestions:** Interactive starter chips to quickly prompt Gemini with common questions.
- **Dark Mode Support:** Full support for system dark theme and Material 3 dynamic color palettes.

### 2. 🎙️ Voice Input (Speech-to-Text)
- Integrated microphone action in the prompt bar using `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- Launched via Compose `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- Speech recognition results populate the prompt field directly for effortless querying.

### 3. 💾 Data Persistence (Room Database & DataStore)
- **Room SQLite Database (`ChatDatabase`):** Persists all user questions and Gemini responses locally so conversations survive app restarts.
- **Chat DAO:** Exposes reactive `Flow<List<ChatMessageEntity>>` and asynchronous message insertion/clearing.
- **Clear Conversation:** Easy one-tap chat history clearing from the top app bar.
- **Preferences DataStore:** Manages user settings and UI preferences.

### 4. 🔐 Robust API Key Security (Mandatory Assignment Requirement)
- **Never Hardcoded:** Zero API keys are hardcoded in Kotlin files, XML resources, or committed Gradle scripts.
- **Git-Ignored Local Configuration:** The key is configured locally inside `local.properties` (listed in `.gitignore`).
- **Template Provided:** `local.properties.example` illustrates required property keys without leaking secrets.
- **CI Fallback:** `app/build.gradle.kts` falls back to environment variables (`System.getenv("GEMINI_API_KEY")`) for headless CI/CD builds.
- **Hardware-Backed Keystore Encryption (AES-256-GCM):** On first launch, the raw key is encrypted using an AES-256 key generated inside the `AndroidKeyStore`. Only the ciphertext and IV are persisted at rest.
- **In-Memory Decryption Only:** Decrypted strictly in-memory at the exact instant the Gemini `GenerativeModel` is instantiated. Decrypted values are never logged, toasted, or exposed to the UI.
- **Release Obfuscation:** R8 / ProGuard enabled in release builds (`isMinifyEnabled = true`) to hinder reverse engineering.

---

## 🛡️ Production Security Architecture & Limits

> [!NOTE]
> **Client-Side Encryption Limits:**
> While encrypting secrets using the hardware-isolated Android Keystore raises the bar against local disk extraction and rooted inspection, client-side encryption **cannot fully protect** an API key against a sophisticated attacker using dynamic runtime instrumentation (e.g. Frida or Xposed) on rooted devices.

### Production Best Practices:
1. **Backend Proxy / Gateway:** Move all direct Gemini API calls behind a trusted backend server (Cloud Run, Firebase Cloud Functions, or Go/Node.js API). The mobile app authenticates to your backend, which then attaches the Gemini API key securely in the cloud.
2. **Firebase App Check:** Attest device integrity using Play Integrity API before granting access to AI endpoints.
3. **Restricted API Keys:** In Google Cloud Console, restrict the API key by package name (`com.fahim.geminiApiComposeStarter`) and SHA-1 signing certificate fingerprint.

---

## 🚀 Getting Started

### 1. Prerequisites
- **Android Studio:** Ladybug (2024.2+) or newer.
- **JDK:** Java 17 or Java 21 (or Android Studio's bundled JetBrains Runtime).
- **Android SDK:** Compile SDK 36, Min SDK 26.
- A physical Android device with USB cable (or an Android Virtual Device / Emulator).

### 2. Obtain a Gemini API Key
1. Visit [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account.
3. Click **Get API key** and create a new key.
4. Copy your API key (supports both standard `AIza...` and new `AQ...` key formats).


### 3. Configure `local.properties`
In the project root directory, open (or create) the `local.properties` file and add your key:

```properties
sdk.dir=/path/to/your/Android/sdk
GEMINI_API_KEY=AIzaSyYourActualKeyHere
```

> [!IMPORTANT]
> `local.properties` is git-ignored and will never be tracked or committed to GitHub.

---

## 📲 Running the App

### Option A: From Android Studio (Desktop)
1. Launch **Android Studio**.
2. Select **Open** and choose this project folder (`chatbot`).
3. Allow Gradle to sync dependencies.
4. Select your target device (Emulator or connected phone) in the top toolbar.
5. Click **Run 'app'** (Green play button ▶ or `Shift + F10`).

### Option B: On a Physical Android Phone via USB Cable
1. **Enable Developer Options on Phone:**
   - Go to **Settings > About Phone**.
   - Tap **Build Number** 7 times until you see "You are now a developer!".
2. **Enable USB Debugging:**
   - Go to **Settings > System > Developer Options**.
   - Turn on **USB Debugging**.
3. **Connect to Computer:**
   - Plug the phone into your Mac/PC using a USB cable.
   - When prompted on your phone screen, select **"Always allow from this computer"** and tap **Allow**.
4. **Verify ADB Connection:**
   ```bash
   adb devices
   ```
   You should see your device listed (e.g. `XXXXXXX device`).
5. **Install and Run:**
   ```bash
   ./gradlew installDebug
   ```
   Or select your phone in Android Studio's device dropdown and click **Run**.

---

## 🧪 Running Tests

### Unit Tests
Execute unit tests for `ChatViewModel`, state handling, Room DAO integration, and fake repositories:

```bash
./gradlew testDebugUnitTest
```

### Compose UI Tests
Run instrumented Compose UI tests on an emulator or connected device:

```bash
./gradlew connectedAndroidTest
```

---

## 📋 Pull Request & Submission Checklist

- [x] Branch name starts with roll number: `N142-gemini-chatbot`.
- [x] No secrets in git history (`local.properties` ignored, `local.properties.example` committed).
- [x] Material 3 Chat UI implemented using `LazyColumn` with stable keys.
- [x] Voice input via `RecognizerIntent` and `rememberLauncherForActivityResult`.
- [x] Room database persistence for chat history surviving restarts.
- [x] Preferences DataStore integrated.
- [x] Android Keystore AES-256-GCM encryption for credentials at rest.
- [x] Release build obfuscation configured (`isMinifyEnabled = true`).
- [x] Comprehensive Unit tests (`ChatViewModelTest`) and Compose UI tests (`ChatScreenTest`).
- [x] README explaining key setup, architecture, and production security limits.
