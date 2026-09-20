# Gemini API Compose Starter

MAD Assignment 1 (`N101-Assignment1`): a Jetpack Compose chat client for Gemini.

## Changes

- Material 3 chat UI with bubbles, empty/loading states, snackbars, and a layout that adapts to `WindowSizeClass` (phone, tablet, landscape) plus light/dark and optional dynamic colour.
- Voice input via `RecognizerIntent`, launched with `rememberLauncherForActivityResult`.
- Chat history in Room; user preferences (auto-send voice, dynamic colour) in Preferences DataStore.
- Gemini calls on `Dispatchers.IO`. The API key is read at build time and stored with Android Keystore encryption.

## Where to place the key

Put the key in the **project root** `local.properties` (same folder as `settings.gradle.kts`). That file is git-ignored.

```properties
GEMINI_API_KEY=your_key_here
```

Rebuild after changing it. Gradle copies the value into `BuildConfig.GEMINI_API_KEY`. Never commit the key or paste it into source.

## Encryption flow

1. Gradle reads `GEMINI_API_KEY` from `local.properties` into `BuildConfig`.
2. On launch, `MainActivity` compares that with any key already stored on the device.
3. If the configured key is new or changed, `ApiKeyManager.saveApiKey()` encrypts it with **AES-256-GCM**. The AES key lives in the **Android Keystore** (`GeminiApiKeyEncryptionKey`) and does not leave the device.
4. Ciphertext and IV are Base64-encoded in private SharedPreferences (`secure_api_storage`).
5. Later launches call `getApiKey()`, which decrypts with the same Keystore key. The plaintext is used only in memory for Gemini requests (`x-goog-api-key`).

If `local.properties` has no key and nothing is stored, the UI shows a configuration error.

## Production: do not ship the key in the app

This sample still embeds the key in the APK via `BuildConfig`, which is fine for a lab build but **not** for production. Anyone can extract it from the binary.

A production app should:

- **Backend proxy (preferred):** the Android app talks only to *your* server. The server holds the Gemini key and calls `generateContent`. The client never sees the key.
- **Firebase App Check:** if you must call Google APIs from the device, attest the app with App Check so unauthenticated / cloned clients cannot use your quota.
- **Restricted API keys:** in Google Cloud / AI Studio, restrict the key by API (`Generative Language API`), app (package name + SHA-1), and if a backend is used, by IP or service account instead of a client key.

Keystore encryption here only hides the key at rest on a trusted device. It does not stop reverse engineering of the APK.

## Running the tests

Unit tests (`ChatViewModel` + fake `GeminiRepository`, `kotlinx-coroutines-test`):

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Compose UI tests (`createComposeRule()`), with an emulator or device running:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

In Android Studio, run classes under `app/src/test/` (unit) or `app/src/androidTest/` (instrumented / Compose).
