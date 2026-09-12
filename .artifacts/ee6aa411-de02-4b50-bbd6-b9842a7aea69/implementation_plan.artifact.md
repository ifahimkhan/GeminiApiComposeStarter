# Implementation Plan - Security Enhancements for Gemini API Key

This plan implements three critical security enhancements to protect the Gemini API key during CI builds, at rest on the device, and against basic reverse-engineering of the release APK.

## User Review Required

> [!IMPORTANT]
> The encryption at rest mechanism will encrypt the build-time API key on first launch and store it in SharedPreferences. Combined with R8 obfuscation, this makes it harder for automated tools to dump the key, though a dedicated attacker with memory inspection or a custom debugger can still find the key when it is decrypted in memory to initialize the `GenerativeModel`.

## Proposed Changes

---

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/aditr/AndroidStudioProjects/GeminiApiComposeStarter/app/build.gradle.kts)
- Update the API key retrieval logic to fall back to the `GEMINI_API_KEY` environment variable if `local.properties` does not contain it.
- Enable R8 obfuscation and minification (`isMinifyEnabled = true`) for the release build type to hinder decompilation.

---

### Security & Crypto Infrastructure

#### [NEW] [SecurityManager.kt](file:///C:/Users/aditr/AndroidStudioProjects/GeminiApiComposeStarter/app/src/main/java/com/fahim/geminiApiComposeStarter/security/SecurityManager.kt)
- Create a `SecurityManager` class that:
  - Generates an AES-256-GCM key inside the Android Keystore using `KeyGenParameterSpec`.
  - Encrypts the plain text API key.
  - Decrypts the ciphertext in memory on-demand.
  - Persists and retrieves the ciphertext from SharedPreferences.

---

### Application Logic & Dependency Injection

#### [MODIFY] [MainActivity.kt](file:///C:/Users/aditr/AndroidStudioProjects/GeminiApiComposeStarter/app/src/main/java/com/fahim/geminiApiComposeStarter/MainActivity.kt)
- Initialize `SecurityManager` on startup.
- On first launch, check if the key is already encrypted. If not, encrypt `BuildConfig.GEMINI_API_KEY` and persist the ciphertext, then ensure the plain string is never retained or logged.

#### [MODIFY] [ChatViewModel.kt](file:///C:/Users/aditr/AndroidStudioProjects/GeminiApiComposeStarter/app/src/main/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModel.kt)
- Update the ViewModel factory to pass a lambda or a provider that decrypts the key only when required, rather than holding the plaintext string in memory long-term.

#### [MODIFY] [GeminiRepositoryImpl.kt](file:///C:/Users/aditr/AndroidStudioProjects/GeminiApiComposeStarter/app/src/main/java/com/fahim/geminiApiComposeStarter/data/GeminiRepositoryImpl.kt)
- Update `GeminiRepositoryImpl` to take a function provider `apiKeyProvider: () -> String` so the key is resolved in-memory only when the text generation call or model instance needs it.

---

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to verify that the build succeeds with the new security infrastructure.
- Run `./gradlew app:assembleRelease` to verify that R8 minification configuration compiles successfully.

### Manual Verification
- Deploy and launch the application on the emulator/device to verify that text generation continues to function correctly.
- Verify that no plaintext logs or toasts expose the API key.
