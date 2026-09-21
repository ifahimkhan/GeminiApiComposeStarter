# Gemini Compose Chat

An Android chat app built with Jetpack Compose that talks to the Gemini API.

## 1. Where to place the API key

1. Get a key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. In the project root, copy `local.properties.example` to `local.properties`
   (Android Studio usually creates this file already, so just add the line below).
3. Add your key on its own line, with no quotes and no spaces around `=`:

   ```
   GEMINI_API_KEY=your_api_key_here
   ```

4. Sync Gradle and run the app.

Notes:

- `local.properties` is git-ignored. Before every commit run `git status` and check that it is not staged.
- Never paste the key into a Kotlin file, `strings.xml` or `build.gradle.kts`.
- **CI builds:** if `local.properties` has no key, Gradle falls back to a `GEMINI_API_KEY`
  environment variable, so a repository secret can be used instead of a file.
- If the key is missing, the app shows "GEMINI_API_KEY is missing" when you try to send a message.

## 2. How the encryption flow works

The key is encrypted at rest with a key that lives in the Android Keystore.

1. **Build time:** Gradle reads `GEMINI_API_KEY` (from `local.properties`, else the environment)
   and exposes it as `BuildConfig.GEMINI_API_KEY`.
2. **First launch:** `KeystoreCipher` creates an AES-256-GCM key in the Android Keystore
   (alias `gemini_api_key_aes256`). The key is non-exportable: the app can use it to encrypt and
   decrypt, but it can never read the key material.
3. **Encrypt and store:** `ApiKeyStore` encrypts the API key with a fresh random 12-byte IV and saves
   only the ciphertext (`Base64(iv + ciphertext + tag)`) in Preferences DataStore (`secure_prefs`).
4. **Use:** when the first request is sent, `GeminiRepositoryImpl` asks `ApiKeyStore` for the key.
   It is decrypted in memory only at that moment, used to create the `GenerativeModel`, and is never
   logged, toasted or shown in the UI.
5. **Recovery:** if the stored ciphertext cannot be decrypted (for example after a backup restore
   onto a new device) or no longer matches the key in the build, it is encrypted again.

Release builds also have `isMinifyEnabled = true`, so R8 obfuscates the code.

Relevant files: `security/KeystoreCipher.kt`, `security/ApiKeyStore.kt`, `data/GeminiRepositoryImpl.kt`.

## 3. How to run the tests

### Unit tests (no device needed)

`ChatViewModelTest` tests `ChatViewModel` with `kotlinx-coroutines-test` and fake repositories.

- **Android Studio:** right-click `app/src/test` and choose **Run Tests in ...**, or open
  `ChatViewModelTest.kt` and click the green arrow next to the class name.
- **Gradle:**

  ```
  gradle testDebugUnitTest
  ```

### Compose UI tests (emulator or device required)

`ChatScreenTest` tests the chat screen with `createComposeRule()`.

1. Start an emulator or connect a device.
2. **Android Studio:** open `app/src/androidTest/.../ui/chat/ChatScreenTest.kt` and click the green
   arrow next to the class name.
3. **Gradle:**

   ```
   gradle connectedDebugAndroidTest
   ```

The tests do not call the Gemini API, so they do not need an API key or an internet connection.
