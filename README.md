# Gemini API Compose Starter — Assignment 1

## Gemini API key setup

1. Get a key from Google AI Studio.
2. Copy `local.properties.example` to `local.properties` (git-ignored) and set `GEMINI_API_KEY=...`.
3. Build and run. For CI, set a repository secret named `GEMINI_API_KEY`; `build.gradle.kts` falls back to that environment variable.
4. If you change the key later, clear the app's data (or reinstall) so the old encrypted copy is dropped.

## How the key is protected

- The key is read from `local.properties` (or the env var) at build time and exposed as `BuildConfig.GEMINI_API_KEY`. It is never in Kotlin sources, XML or committed Gradle files.
- On first launch an AES-256-GCM key is generated inside the Android Keystore. The API key is encrypted with it and only the ciphertext (Base64 of IV + ciphertext) is saved in Preferences DataStore.
- The key is decrypted in memory only when the `GenerativeModel` is created, and is never logged or shown.
- Release builds use R8 (`isMinifyEnabled = true`).

## Limits of client-side protection

Encryption and obfuscation only raise the bar; a determined attacker can still extract a key from a running app or intercept the traffic. A production app should call Gemini through a backend proxy that holds the key, and/or use Firebase App Check with an API key restricted to the app's package name and signing certificate.

## Running the tests

- Unit tests (ViewModel): `./gradlew testDebugUnitTest`
- Compose UI tests (emulator/device needed): `./gradlew connectedDebugAndroidTest`