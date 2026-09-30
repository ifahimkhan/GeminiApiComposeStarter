# Gemini Generative AI Assistant (Jetpack Compose)

**Course:** Mobile Application Development (Subject Code: 702AI0E002)  
**Student:** Rajvir Hansoti  
**Roll Number:** N034  
**Institution:** SVKM's NMIMS, School of Technology Management & Engineering  
**Branch:** `N034-assignment-1`

---

## 📱 Project Summary
A privacy-centric, multi-modal generative AI chat application developed with **Jetpack Compose (Material 3)**, integrating Google's official **Gemini SDK**.

### Features Implemented:
1. **Hardware-Isolated Cryptography:** Android KeyStore AES-256-GCM encryption at rest with unique IV vectors for zero plain-text API key exposure.
2. **Persistent Conversation Store:** Reactive Room-compatible SQLite database storing dialogue turns with auto-timestamping.
3. **Responsive Material 3 UI:** Fluid `LazyColumn` conversation stream with Royal Indigo styling, animated auto-scroll, and message timestamps.
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
        |  - Alias: n034_gemini_secure_vault     |
        |  - AES-256-GCM Master Key               |
        |  - Random 12-byte IV per encryption     |
        +-------------------+--------------------+
                            |
                   (Encrypted at rest)
                            |
                            v
        +----------------------------------------+
        |       Secure Storage (Ciphertext)      |
        |       Prefs: n034_vault_storage        |
        +-------------------+--------------------+
                            |
                   (Decrypted in-memory)
                            |
                            v
        +----------------------------------------+
        |       Gemini GenerativeModel           |
        +----------------------------------------+
```

---

## 🚀 Getting Started

### 1. Configuration
Open or create `local.properties` in the project root:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
```

### 2. Building & Testing
Run automated unit tests:
```bash
./gradlew testDebugUnitTest
```
Build debug APK:
```bash
./gradlew assembleDebug
```
