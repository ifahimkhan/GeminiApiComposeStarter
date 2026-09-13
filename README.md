# Gemini Jetpack Compose Starter - Lab Assignment 1

**Name:** Ananya Kapote  
**Roll Number:** C051  
**Subject:** Mobile Application Development (702AI0E002)  
**Academic Year:** 2026–27

---

## 1. Where to Place the API Key

### Local Development Setup
1. Obtain a free Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Open or create the `local.properties` file in the project root directory (this file is excluded by `.gitignore` and must **never** be committed to Git).
3. Add your key without quotes:
   ```properties
   GEMINI_API_KEY=AIzaSyYourActualApiKeyHere
A template file named local.properties.example has been committed to the repository so team members know how to configure their environment.
CI/CD Environment Fallback
In app/build.gradle.kts, a fallback mechanism is implemented so automated continuous integration builds can inject the API key as an environment variable without requiring a local.properties file:
code
Kotlin
val localProperties = Properties().apply {
val file = rootProject.file("local.properties")
if (file.exists()) file.inputStream().use { load(it) }
}
val rawKey = localProperties.getProperty("GEMINI_API_KEY")
?: System.getenv("GEMINI_API_KEY")
?: ""
val geminiApiKey = rawKey.replace("\"", "").trim()
2. Key Encryption Architecture (Step 3 Requirement)
   How Encryption at Rest Works
   To avoid storing the plain-text API key in shared storage or preferences:
   Keystore-Backed AES-256-GCM Key: On first launch, SecurityManager generates a 256-bit AES key inside the hardware-backed AndroidKeyStore using KeyGenParameterSpec configured with BLOCK_MODE_GCM and ENCRYPTION_PADDING_NONE.
   First-Run Encryption: The app reads BuildConfig.GEMINI_API_KEY, encrypts it using AES-256-GCM (packing the 12-byte initialization vector with ciphertext), and saves only the Base64 ciphertext in private SharedPreferences (secure_settings).
   In-Memory Decryption Only: The key is decrypted into memory strictly at the moment GeminiRepository and GenerativeModel are instantiated. The decrypted key is never logged, toasted, or written to disk.
   Code Obfuscation: Release builds enable R8 code shrinking and minification (isMinifyEnabled = true) in app/build.gradle.kts to prevent the key handling logic from being trivially reverse-engineered.
   Production Limitations ("Know the Limits")
   While client-side Keystore encryption prevents extraction via file system inspection and physical storage dumps, it cannot completely protect secrets against an attacker with root privileges and dynamic instrumentation tools (e.g., Frida or Xposed) inspecting process memory.
   In an enterprise production app, the following architecture would be employed:
   Backend Proxy: The mobile client would never hold a third-party API key directly. Instead, the app authenticates with Firebase or OAuth, and all Gemini requests pass through a secured backend server (e.g., Cloud Functions).
   Firebase App Check: Verifies app and device attestation via Play Integrity to ensure only genuine, untampered copies of the app can reach the backend.
   Google Cloud API Restrictions: Constraints applied directly in the Google Cloud Console restricting API keys by package name, SHA-256 signing certificate fingerprint, and Gemini API scope.
3. Implemented Features Summary
   Material 3 Chat Bubbles (Step 4): Conversation rendered as an interactive LazyColumn with stable keys (key = { it.id }) for smooth recomposition, distinct User vs. Gemini styling, auto-scroll via LaunchedEffect, and responsive width constraints.
   Loading & Error Feedback (Step 4): Centered CircularProgressIndicator during generation and a Snackbar for network or API error messages.
   Speech to Text (Step 5): Voice input integrated via RecognizerIntent launched with Compose rememberLauncherForActivityResult.
   Room Database Persistence (Step 5): Implemented AppDatabase, ChatDao, and ChatEntity so chat conversations persist across app restarts.
4. How to Run the Tests (Step 6 Requirement)
1. Local Unit Tests (ChatViewModelTest.kt)
   Tests ViewModel state updates, prompt error validation, and Room message insertion using kotlinx-coroutines-test and a fake repository.
   In Android Studio: Navigate to app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt, right-click and select Run 'ChatViewModelTest'.
   Via Terminal:
   code
   Bash
   ./gradlew testDebugUnitTest
2. Compose UI Tests (ChatScreenTest.kt)
   Validates that conversation chat bubbles are correctly composed and visible on the screen using Compose's createComposeRule().
   In Android Studio: Navigate to app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt, right-click and select Run 'ChatScreenTest' (with your phone or emulator connected).
   Via Terminal:
   code
   Bash
   ./gradlew connectedDebugAndroidTest