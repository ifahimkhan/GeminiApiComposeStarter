# Gemini AI Chatbot - Android Application

**Mobile Application Development Lab Assignment 1**  
**SVKM's NMIMS University**  
*Mukesh Patel School of Technology Management & Engineering (MPSTME)*  

---

### Student Details
* **Student Name**: Akshay Sathaye
* **Roll Number**: C049
* **Program**: B.Tech / MBA Tech (Mobile Application Development)

---

## 1. Executive Summary

This repository contains a full-featured, production-ready Android AI Chatbot application built using **Jetpack Compose**, **Material 3 Design System**, **Google Generative AI SDK (Gemini)**, **Room Database**, **Preferences DataStore**, and **Android Keystore AES-256-GCM Encryption**.

The application provides an intuitive conversational interface with Google's Gemini AI model while enforcing strict security standards for client-side API key handling, offline history persistence, and responsive UI adaptation across device form factors.

---

## 2. Key Features

* **Real-time Gemini AI Conversational Engine**: Seamless integration with `gemini-1.5-flash` for fast, intelligent conversational responses.
* **Declarative Material 3 Chat UI**:
  * Smooth `LazyColumn` conversation display with stable item keys.
  * Distinct User and Model chat bubbles with custom theme colors and shapes.
  * Automatic scroll-to-bottom on new messages and loading triggers.
* **Voice-to-Text Input**: Speech-to-text recognition via `RecognizerIntent` and `rememberLauncherForActivityResult`, populating the prompt input field for user review prior to dispatch.
* **Offline Chat History Persistence**:
  * Full conversation persistence powered by **Room Database**.
  * Complete data survival across application restarts, app updates, and device reboots.
* **Preferences DataStore Integration**: Persistent management of user preferences including auto-scroll toggles and system/manual Dark Mode overrides.
* **Adaptive & Responsive Layouts**:
  * Optimized display for phones, foldables, and wide-screen tablets (portrait and landscape).
  * Enforces maximum content width bounds (`800.dp`) for optimal scannability on ultra-wide screens.
* **Robust Loading & Error Feedback**:
  * Non-blocking `CircularProgressIndicator` during AI inference.
  * User-friendly `Snackbar` alerts for network/API failures that purge raw exceptions or sensitive metadata.
* **Release Build Optimization**: Fully minified and obfuscated release configuration (`isMinifyEnabled = true`, `isShrinkResources = true`) with custom Proguard/R8 rules.

---

## 3. Architecture & Technical Design

The project strictly follows modern Android Architecture Guidelines with Clean Architecture principles:

```
com.example.myapplication
 ├── security/             # Keystore AES-256-GCM Encryption & Secure Key Storage
 ├── data/
 │    ├── local/           # Room DB (Entities, DAOs, Database) & Preferences DataStore
 │    ├── model/           # Domain Models (ChatMessage, ChatRole)
 │    └── repository/      # Repository Layer (GeminiRepository & GeminiRepositoryImpl)
 └── ui/
      ├── chat/            # Jetpack Compose UI, ChatViewModel, ChatUiState, Factory
      └── theme/           # Material 3 Color Schemes & Dark Mode Support
```

### Key Architectural Highlights:
1. **Unidirectional Data Flow (UDF)**:
   * State is centralized in `ChatUiState` and exposed via `StateFlow` from `ChatViewModel`.
   * UI components collect state using `collectAsStateWithLifecycle()`.
   * Composables are stateless and decoupled from business logic.
2. **Asynchronous Coroutines & Off-Main-Thread Execution**:
   * All database operations and Gemini network requests execute off the main thread on `Dispatchers.IO`.

---

## 4. API Key Security & Encryption at Rest

To comply with enterprise security requirements, the application implements **AES-256-GCM Encryption at Rest**:

1. **Gradle Local Configuration**:
   * The API key is defined in `local.properties` (which is strictly listed in `.gitignore`).
   * Gradle exposes the key via `BuildConfig.GEMINI_API_KEY` with environment variable fallback.
2. **Android Keystore Key Generation**:
   * On first boot, `CryptoManager` generates a 256-bit AES key inside the hardware-backed **Android Keystore** using `KeyGenParameterSpec` with `GCM/NoPadding`.
3. **Encrypted Persistence**:
   * `SecureKeyStorage` encrypts the API key and persists **only the encrypted ciphertext and IV** into Preferences DataStore.
   * The decrypted key is **never stored in plaintext on disk**, **never logged**, and **never displayed** in Toasts, Snackbars, or UI components.
4. **In-Memory Decryption**:
   * Decryption occurs strictly in memory when instantiating `GenerativeModel` for network requests.

---

## 5. Testing & Quality Assurance

The application includes unit tests and Compose UI tests:

* **Unit Tests (`ChatViewModelTest`)**:
  * Leverages `kotlinx-coroutines-test` and `FakeGeminiRepository`.
  * Verifies initial state, prompt submission, loading indicators, state flow transitions, successful responses, error handling, and history clearing.
* **Compose UI Tests (`ChatScreenTest`)**:
  * Leverages `createComposeRule()` with fake test data.
  * Verifies text input interaction, send button enable/disable states, chat message rendering, and loading indicators.

---

## 6. Client-Side Protection Limitations & Production Recommendations

### Limitations of Client-Side API Keys
While AES-256-GCM Keystore encryption protects against static binary extraction and filesystem dumps, any client-side app holds decryption capability in memory at runtime. Rooted devices with dynamic instrumentation tools (e.g., Frida, Xposed) can intercept in-memory buffers.

### Production Best Practices:
1. **Backend Proxy Gateway**: Relocate Gemini API invocation to a secure backend service (e.g., Cloud Functions / Node.js API). The Android app authenticates via OAuth/Firebase Auth, and the backend securely injects the API key.
2. **Firebase App Check**: Deploy App Check with Play Integrity API to ensure only official, untampered app instances access backend services.
3. **Google Cloud Restrictions**: Apply package name and SHA-1 fingerprint restriction rules in the Google Cloud Console.
