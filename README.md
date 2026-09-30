# Gemini Generative AI Assistant (Jetpack Compose)

**Course:** Mobile Application Development (Subject Code: 702AI0E002)  
**Student:** Arnav Gundawar  
**Roll Number:** N032  
**Institution:** SVKM's NMIMS, School of Technology Management & Engineering  
**Branch:** `N032_ArnavGundawar`

---

## 📱 Project Summary
An end-to-end, privacy-centric AI chat application engineered with **Jetpack Compose (Material 3)**, integrating Google's **Gemini Generative AI SDK**.

This project implements:
1. **Hardware-Backed Cryptography:** Android KeyStore AES-256-GCM encryption for zero plain-text API key exposure at rest.
2. **Local Persistence:** Reactive Room/SQLite message store ensuring chat histories survive app lifecycle terminations and restarts.
3. **Responsive Material 3 UI:** Fluid `LazyColumn` conversation stream with tailored user/AI bubbles, animated auto-scroll, and adaptive multi-device layouts.
4. **Hands-Free Speech Input:** Native Android `RecognizerIntent` integration for real-time speech-to-text querying.
5. **Quality Assurance:** Coroutine-tested ViewModel suite and automated Compose UI testing.

---

## 🔒 Security Architecture: Keystore AES-256-GCM

```
[ Developer Environment ]
       |
       +---> local.properties (Git-Ignored)
                   |
                   v
            [ BuildConfig ] (Build-time compilation)
                   |
                   v (First Launch)
        +----------------------------------------+
        |  Android KeyStore (Hardware TEE / SE)   |
        |  - AES-256-GCM Master Key               |
        |  - Random 12-byte IV per encryption     |
        +-------------------+--------------------+
                            |
                   (Encrypted at rest)
                            |
                            v
        +----------------------------------------+
        |       Secure Storage (Ciphertext)      |
        +-------------------+--------------------+
                            |
                   (Decrypted in-memory)
                            |
                            v
        +----------------------------------------+
        |       Gemini GenerativeModel           |
        +----------------------------------------+
```

### Key Security Safeguards
* **Zero Hardcoded Credentials:** The Gemini API key is isolated in `local.properties` and never committed to version control.
* **Encrypted at Rest:** Raw API key is encrypted using AES-256 in Galois/Counter Mode (GCM) with authenticated encryption.
* **Hardware Isolation:** Cryptographic keys reside exclusively within the Android KeyStore boundary (TEE/StrongBox), ensuring keys cannot be exported even on rooted devices.
* **In-Memory Lifespan:** The plain-text key exists only in short-lived memory during client network instantiation.

---

## 🛠️ Tech Stack & Architecture

* **UI Framework:** Jetpack Compose, Material 3, Material Icons Extended
* **Architecture:** MVVM (Model-View-ViewModel) with unidirectional data flow (UDF) via Kotlin Coroutines `StateFlow`
* **Local Database:** SQLite / Room-compatible schema with asynchronous Flow observables
* **AI Model:** Google Gemini SDK (`generativeai:0.9.0`)
* **Voice Recognition:** Android Speech Recognizer (`RecognizerIntent`)
* **Testing:** JUnit4, Coroutines Test Dispatcher, Compose Test Rule

---

## 🚀 Getting Started

### 1. Prerequisites
* Android Studio Meerkat or Ladybug (or latest stable)
* Android SDK 36 (compileSdk) / Min SDK 26
* JDK 17 or JDK 21

### 2. Configuration
1. Open or create `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
2. Build and run the project:
   ```bash
   ./gradlew assembleDebug
   ```

### 3. Running Automated Tests
Run unit tests for ViewModel and persistence:
```bash
./gradlew testDebugUnitTest
```
