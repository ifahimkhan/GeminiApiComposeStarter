# Gemini Jetpack Compose Chat App

**Course:** Mobile Application Development (Subject Code: 702AI0E002)  
**Student:** Krris Gandhi  
**Roll Number:** N028  
**Institution:** SVKM's NMIMS, School of Technology Management & Engineering  
**Branch:** `N028_Krris_Gandhi`

---

## 📌 Project Overview
An advanced, secure, multi-modal conversational Android application built with **Jetpack Compose (Material 3)** powered by the **Google Gemini Generative AI SDK**. 

This application builds upon the starter template to introduce:
* **Hardware-Backed Cryptographic Security:** Android KeyStore AES-256-GCM encryption at rest.
* **Persistent Chat History:** SQLite/Room-compatible reactive storage ensuring messages survive app restarts.
* **Modern Material 3 UI:** LazyColumn of animated chat bubbles (User vs. Gemini), auto-scrolling, and responsive layouts.
* **Voice Speech-to-Text Modality:** Hands-free input via `RecognizerIntent`.
* **Personalized Experience:** Dynamic settings and user personalization via DataStore preferences.
* **Automated Test Coverage:** ViewModel coroutine unit tests and Compose UI testing.

---

## 🔐 API Key Security Architecture

```
                   +---------------------------+
                   |     local.properties      |  (Git-Ignored / Developer Machine)
                   +-------------+-------------+
                                 |
                                 v
+------------------+     [ BuildConfig ]     +--------------------+
| System.getenv()  | --> (Build-time field)  | local.properties.  |
|   (CI Secret)    |                         |      example       |
+------------------+                         +--------------------+
                                 |
                                 v [First App Launch]
                   +---------------------------+
                   |   Android KeyStore (TEE)  |
                   |      (AES-256-GCM)        |
                   +-------------+-------------+
                                 |
                        (Encrypts Key at Rest)
                                 |
                                 v
                   +---------------------------+
                   |    SecureKeyStorage       | (Encrypted SharedPreferences /
                   |   (Stores Ciphertext)     |  DataStore)
                   +-------------+-------------+
                                 |
                     [In-Memory Decryption Only]
                                 v
                   +---------------------------+
                   |      GenerativeModel      |
                   +---------------------------+
```

### 1. Key Isolation
* **Zero Hardcoded Secrets:** The API key never appears in Kotlin files, strings.xml, or committed Gradle scripts.
* **VCS Exclusion:** `local.properties` is strictly ignored by `.gitignore`. A template file `local.properties.example` is committed for configuration instructions.
* **CI/CD Compatibility:** `app/build.gradle.kts` falls back to `System.getenv("GEMINI_API_KEY")` when building in headless CI environments.

### 2. Encryption at Rest (Android KeyStore)
* On first launch, `KeyStoreManager` creates or retrieves a 256-bit AES key inside the **Android KeyStore** provider (`AndroidKeyStore`).
* The API key is encrypted using `AES/GCM/NoPadding` with a unique Initialization Vector (IV).
* Only the Base64-encoded ciphertext and IV are persisted at rest in `SecureKeyStorage`.
* The key is decrypted into memory **only** when initializing `GeminiRepositoryImpl` and is never logged, toasted, or displayed.

### 3. Obfuscation
* `isMinifyEnabled = true` is enabled in release builds to invoke R8 code shrinking, optimization, and obfuscation.

### 4. Production Security Limits & Best Practices
> **⚠️ Security Note:** While client-side KeyStore encryption prevents static APK decompilation and casual data extraction on rooted devices, determined attackers with physical access can theoretically hook memory during runtime.
>
> In production architectures, enterprise apps should:
> 1. **Backend Proxy:** Route all AI calls through a secure backend (e.g., Cloud Functions / API Gateway) that holds the master key and enforces user authentication, rate limiting, and quota management.
> 2. **Firebase App Check:** Attest device integrity using Play Integrity API so only genuine, untampered app instances can access AI services.
> 3. **API Key Restrictions:** Restrict Google Cloud keys by Android package name and SHA-1 certificate fingerprint.

---

## 🎨 Jetpack Compose UI & UX Features
* **LazyColumn Conversation:** Displays dialogue turns as Material 3 cards. User messages are right-aligned using `primaryContainer`, while Gemini responses are left-aligned using `surfaceVariant` with an AI avatar.
* **Auto-Scroll:** Uses `rememberLazyListState()` combined with `LaunchedEffect` to automatically scroll to the newest message whenever a response arrives.
* **Responsive Layouts (`BoxWithConstraints`):** Dynamic bubble widths and padding adapting across phones, tablets, and landscape orientation.
* **Visual States:** Seamless transitions between empty state, real-time typing indicators, and floating error Snackbars.

---

## 🎙️ Speech-to-Text Input
* Integrated microphone action button inside the prompt bar.
* Utilizes `rememberLauncherForActivityResult` with `ActivityResultContracts.StartActivityForResult()` to dispatch Android's native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
* Automatically inputs transcribed speech into the query field for instantaneous prompt dispatch.

---

## 🚀 Setup & Execution Guide

### Prerequisites
* Android Studio Ladybug / Meerkat (or newer)
* JDK 17+ or JDK 21+
* Android SDK (API 34/36)

### 1. Clone & Checkout
```bash
git clone https://github.com/Krris10/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
git checkout N028_Krris_Gandhi
```

### 2. Configure API Key
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. Copy `local.properties.example` to `local.properties`:
   ```properties
   GEMINI_API_KEY=AIzaSy...YourActualKeyHere
   ```

### 3. Run Automated Tests
Execute the unit test suite:
```bash
./gradlew test
```
Execute instrumented Compose UI tests:
```bash
./gradlew connectedAndroidTest
```

### 4. Build & Install
```bash
./gradlew assembleDebug
```
Or open the project in Android Studio and click **Run 'app'** (`Shift + F10`).
