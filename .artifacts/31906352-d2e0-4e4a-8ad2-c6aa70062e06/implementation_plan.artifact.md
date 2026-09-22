# Implementation Plan - Secure the API Key (Mandatory)

Implement secure API key handling, environment fallback for CI, `local.properties.example`, Android Keystore encryption at rest (`AES-256-GCM`), R8 release obfuscation (`isMinifyEnabled = true`), and documentation on production backend proxy protection in `README.md`.

## User Review Required

> [!IMPORTANT]
> - CI Environment Fallback: `app/build.gradle.kts` will be updated to fall back to `System.getenv("GEMINI_API_KEY")` when `local.properties` is missing or blank.
> - Android Keystore Encryption: We will implement an `ApiKeyManager` that encrypts the API key using Android Keystore (`KeyGenParameterSpec` with `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`, `ENCRYPTION_MODE_GCM`) and stores ciphertext securely.
> - Release Obfuscation: `isMinifyEnabled` will be set to `true` for release builds.
> - Documentation: `README.md` will be updated to discuss backend proxy and Firebase App Check considerations for production.

## Proposed Changes

### Build Configuration & CI Fallback

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/app/build.gradle.kts)
- Update `geminiApiKey` extraction to fall back to environment variable:
  `val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")?.trim() ?: System.getenv("GEMINI_API_KEY")?.trim().orEmpty()`
- Set `isMinifyEnabled = true` in `release` build type.

#### [NEW] [local.properties.example](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/local.properties.example)
- Create template file with placeholder `GEMINI_API_KEY=your_api_key_here`.

### Keystore Encryption & Security

#### [NEW] [ApiKeyManager.kt](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/app/src/main/java/com/fahim/geminiApiComposeStarter/security/ApiKeyManager.kt)
- Manages AES-256-GCM key generation inside Android Keystore (`KeyStore.getInstance("AndroidKeyStore")`).
- Encrypts and decrypts API key values in memory; never logs or displays the key.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/app/src/main/java/com/fahim/geminiApiComposeStarter/MainActivity.kt)
- Retrieve decrypted API key securely via `ApiKeyManager` at runtime to initialize `GeminiRepositoryImpl`.

### Documentation

#### [MODIFY] [README.md](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/README.md)
- Add section explaining API key security (Android Keystore encryption at rest, environment fallback for CI, R8 obfuscation, and production backend proxy / Firebase App Check best practices).

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure build configuration and security manager compile correctly.
- Run unit tests (`app:testDebugUnitTest`).
