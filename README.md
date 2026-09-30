# Gemini API Compose Starter 🚀

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Gemini](https://img.shields.io/badge/Gemini%20API-3.6%20Flash-blue.svg?logo=google)](https://ai.google.dev/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-brightgreen.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-brightgreen.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A modern, production-hardened Android starter template demonstrating how to integrate Google's **Gemini API** into a **Jetpack Compose** application using clean architecture, Material 3, and industry-standard security practices.

---

## ✨ Features

- **🎨 Modern Jetpack Compose UI**: Built entirely with Compose and Material 3, supporting Dynamic Color (Material You), edge-to-edge layout, and adaptive dark/light theming.
- **🤖 Powered by Gemini**: Direct integration with Google's official Generative AI Client SDK (`com.google.ai.client.generativeai`) using `gemini-3.6-flash`.
- **🔐 Hardware-Backed Secure API Key Storage**:
  - In-app Bring-Your-Own-Key (BYOK) setup dialog so release APKs never require hardcoded secrets.
  - Stored securely in `EncryptedSharedPreferences` backed by AES-256-GCM / AES-256-SIV via the **Android Keystore**.
  - Optional `local.properties` fallback for rapid local developer prototyping.
- **🛡️ Built-in Content Safety & Boundary Enforcement**:
  - Configured safety thresholds for Harassment, Hate Speech, Sexually Explicit content, and Dangerous Content.
  - Output token ceilings (`maxOutputTokens = 2048`) and prompt input caps (4,000 characters) with real-time UI character counters.
- **🔒 Production-Hardened Build**:
  - **R8 / ProGuard minification** and resource shrinking enabled for release builds.
  - Automatic `android.util.Log` stripping in release to prevent sensitive operational logs in Logcat.
  - `network_security_config.xml` strictly disallowing cleartext HTTP traffic.
  - Application auto-backup disabled (`android:allowBackup="false"`) with explicit data extraction exclusion rules.
- **🧱 Clean Architecture & Reactive State**: MVVM architecture using Kotlin Coroutines, `StateFlow`, and repository abstractions for seamless testability.
- **🧪 Unit Tested**: Complete unit test suite with `kotlinx-coroutines-test` covering prompt validation, error sanitization, and key management.

---

## 🏗️ Project Architecture

```text
app/src/main/java/com/fahim/geminiApiComposeStarter/
├── MainActivity.kt                  # Single-activity host with DI wiring
├── data/
│   ├── GeminiRepository.kt          # Interface for Gemini API text generation
│   ├── GeminiRepositoryImpl.kt      # Implementation with safety filters & generation limits
│   └── security/
│       └── ApiKeyStorage.kt         # Secure storage using EncryptedSharedPreferences & Keystore
└── ui/
    ├── chat/
    │   ├── ChatRoute.kt / ChatScreen.kt  # Compose chat screen, prompt input & API Key dialog
    │   ├── ChatViewModel.kt              # Reactive ViewModel, input validation & error sanitization
    │   └── ChatUiState.kt                # Immutable UI state and error definitions
    ├── text/
    │   └── BoldMarkdown.kt          # Lightweight markdown parser for bold spans
    └── theme/
        ├── Color.kt, Theme.kt, Type.kt  # Material 3 design system tokens
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2+) or newer
- **JDK**: Java 17+ (Java 21/25 supported)
- **Android SDK**: Compile & Target SDK 36 (Min SDK 26)
- **Gemini API Key**: Obtain a free API key from [Google AI Studio](https://aistudio.google.com/)

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/vedikakaki/GeminiApiComposeStarter.git
   cd GeminiApiComposeStarter
   ```

2. **Configure your API Key** *(Choose either method)*:

   - **Method A: In-App UI (Recommended)**:
     Launch the app on an emulator or device. Tap the **Settings** icon in the top right corner and paste your Gemini API key. It will be stored in encrypted storage on the device.

   - **Method B: Local Properties (Developer Mode)**:
     Create or update `local.properties` in the project root:
     ```properties
     GEMINI_API_KEY=your_gemini_api_key_here
     ```
     *(Note: `local.properties` is automatically git-ignored so your key never enters version control.)*

3. **Build and Run**:
   Open the project in Android Studio and click **Run** (or execute via CLI):
   ```bash
   ./gradlew installDebug
   ```

---

## 🧪 Testing & Verification

Run the unit tests:
```bash
./gradlew testDebugUnitTest
```

Verify release compilation, R8 minification, and resource shrinking:
```bash
./gradlew assembleRelease
```

---

## 🛡️ Security Architecture Highlights

| Security Concern | Implementation Detail |
| :--- | :--- |
| **API Key Protection** | Managed via `SecureApiKeyStorage` backed by Android Keystore (`MasterKey.KeyScheme.AES256_GCM`). Release builds do not bake keys into the APK bytecode. |
| **Error Sanitization** | `ChatViewModel` sanitizes all exceptions into user-friendly messages, preventing backend query URLs or credentials from surfacing in UI snackbars. |
| **Log Leak Prevention** | `GeminiRepositoryImpl` guards logs behind `BuildConfig.DEBUG`, and R8 ProGuard rules strip `android.util.Log` from release builds. |
| **Network Security** | `network_security_config.xml` enforces TLS with `cleartextTrafficPermitted="false"` trusting only system certificate authorities. |
| **Data Backup** | `android:allowBackup="false"` with `data_extraction_rules.xml` prevents extraction of device data via ADB or cloud backups. |
| **Model Abuse Prevention** | Token generation bounds (`maxOutputTokens = 2048`) and safety block listeners prevent prompt flooding and unmoderated responses. |

---

## 📦 Tech Stack & Dependencies

- **Language**: [Kotlin](https://kotlinlang.org/) (2.0.21)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **AI SDK**: [Google Generative AI Client SDK](https://github.com/google-gemini/generative-ai-android) (`0.9.0`)
- **Security**: [AndroidX Security Crypto](https://developer.android.com/jetpack/androidx/releases/security) (`1.1.0-alpha06`)
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Testing**: JUnit 4, Kotlinx Coroutines Test, Compose Testing Manifest

---

## 📄 License

```text
Copyright 2026 Vedika Kaki

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
