# Gemini API Compose Starter

A production-ready Android application built with **Jetpack Compose**, **Material 3**, **Google Gemini AI Client SDK**, **Room Database**, and **Android KeyStore AES-256-GCM Security**.

---

## 🔒 API Key Setup & Security Architecture

### 1. Where to Place the Key
1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. Open `local.properties` in the root directory (or create it from `local.properties.example`) and add:
   ```properties
   GEMINI_API_KEY=YOUR_ACTUAL_GEMINI_API_KEY
   ```
3. `local.properties` is listed in `.gitignore` and is **never** committed to version control.

### 2. CI/CD Environment Fallback
In `app/build.gradle.kts`, the build logic falls back to the `GEMINI_API_KEY` environment variable when `local.properties` has no key:
```kotlin
val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: "").trim()
```

### 3. Encryption at Rest (AES-256-GCM + Android KeyStore)
* **First Launch:** The app generates a 256-bit AES key inside the hardware-backed **Android KeyStore** using `KeyGenParameterSpec` with `KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT` and `GCM` block mode.
* **Storage:** The API key is encrypted using AES-GCM, and only the Base64-encoded initialization vector (IV) and ciphertext are persisted in **Preferences DataStore**.
* **In-Memory Decryption:** Decryption occurs strictly in memory inside `GeminiRepositoryImpl` when initializing `GenerativeModel`. The decrypted key is never logged, toasted, or exposed in UI.

### 4. Release Build Obfuscation
Release builds enable R8 minification and resource shrinking in `app/build.gradle.kts`:
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

### 5. Client-Side Security Limits & Production Best Practices
> [!IMPORTANT]
> **Client-Side Limits:** Client-side encryption and R8 obfuscation significantly raise the reverse-engineering bar. However, any client application containing an API key or secret can theoretically be analyzed by a determined attacker using root tools, memory dumpers, or dynamic instrumentation (e.g., Frida).
>
> **Production Recommendation:** A production app should:
> 1. Proxy all Gemini API requests through a secure **Backend Service** (e.g., Cloud Functions, Cloud Run, or App Engine). The backend holds the API key securely in Secret Manager.
> 2. Protect backend endpoints using **Firebase App Check** or Play Integrity API to ensure only authentic app instances can call the backend.
> 3. Apply strict domain, package, and quota restrictions on Google Cloud Console API keys.

---

## ✨ Features & Architecture

* **UI (Jetpack Compose & Material 3):**
  * `LazyColumn` conversation list with Material 3 chat bubbles (user vs. assistant) and stable keys.
  * Auto-scrolling to the latest message.
  * Speech-to-Text (Voice Input) using `RecognizerIntent` and `rememberLauncherForActivityResult`.
  * Loading state (`CircularProgressIndicator`) and error handling via `Snackbar`.
  * Support for Light/Dark mode and Material 3 dynamic color scheme.

* **Data & Persistence:**
  * **Room Database (`AppDatabase`):** Persistent conversation history (`ChatMessageEntity`) that survives app restarts.
  * **Preferences DataStore:** Manages encrypted key storage and user preferences.

* **Architecture:**
  * MVVM architecture using `StateFlow`, `ChatUiState`, `ChatViewModel`, and `collectAsStateWithLifecycle()`.

---

## 🧪 Running Tests

### Unit Tests
Executes unit tests for `ChatViewModel` using `kotlinx-coroutines-test` and `FakeGeminiRepository`:
```bash
./gradlew test
```

### Compose UI Tests
Executes Compose UI tests for `ChatScreen` using `createComposeRule()`:
```bash
./gradlew connectedAndroidTest
```
