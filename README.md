# Gemini Assistant – Jetpack Compose (Lab Assignment 1)

Chat client for Google's Gemini API, built on top of
[GeminiApiComposeStarter](https://github.com/ifahimkhan/GeminiApiComposeStarter).

## What it does
- Conversation screen with avatars, timestamps and **bold** formatting for Gemini replies
- Welcome screen with tap-to-ask suggestion chips
- Adapts to screen size (`WindowSizeClass`): appearance dialog on phones, fixed side panel on tablets
- "typing…" status, progress bar while waiting, error banner with Retry, automatic back-off on 503/429
- Light / Dark / Follow-system theme and wallpaper colours (Android 12+)
- Speech input through the system recogniser (`RecognizerIntent`)
- Messages kept in Room, appearance choices kept in Preferences DataStore
- API key encrypted on the device with an Android Keystore key

## Setting up the API key
1. Create a key in Google AI Studio.
2. Copy `local.properties.example` to `local.properties` (already in `.gitignore`).
3. Add `GEMINI_API_KEY=<your key>` and sync Gradle.
4. Optional: `GEMINI_MODEL=<model name>` switches the model without editing code
   (default `gemini-flash-latest`).

If `local.properties` has no key, the build reads the `GEMINI_API_KEY` environment variable
instead, so a CI server can inject the key from a secret.

The key must never appear in source files, resources or committed Gradle files. A git hook
that refuses commits containing `local.properties` or anything shaped like a Google key is
included:
```
cp scripts/hooks/pre-commit .git/hooks/pre-commit
```

## How the key is protected on the device
1. On the first start, `ChatApplication` asks `SecureKeyStore` to protect the key.
2. `AesGcmKeystore` creates (once) a 256-bit AES key inside the Android Keystore with
   `KeyGenParameterSpec` – GCM mode, random 12-byte IV, 128-bit tag. That AES key cannot be
   exported from the Keystore.
3. Only `Base64(IV + ciphertext)` is saved, in the `key_vault` DataStore, together with a
   truncated SHA-256 fingerprint so a changed key in `local.properties` gets re-encrypted.
4. `GeminiRepositoryImpl` decrypts the key in memory only when it builds the
   `GenerativeModel`. The key is never logged or shown on screen.
5. Release builds run R8 (`isMinifyEnabled`, `isShrinkResources`).

**Why this still isn't production-grade:** the plaintext key is compiled into
`BuildConfig`, so anyone who decompiles the APK can read it. A real product should keep the
key on a server (the app calls your backend, which calls Gemini), or use Firebase AI Logic
with App Check, and restrict the key in Google Cloud Console to the app's package name and
signing certificate.

## Project layout
```
ui/chat      ChatScreen (stateless UI), ChatViewModel, ChatUiState
ui/theme     GeminiTheme (teal/coral palette + dynamic colour)
data         GeminiRepository, MessageRepository (Room), PreferencesRepository (DataStore)
security     SecureKeyStore, AesGcmKeystore
ServiceLocator.kt   hand-written dependency wiring
```

## Tests
- `ChatViewModelTest` – 11 JVM tests with fake repositories and `kotlinx-coroutines-test`
- `ChatScreenTest` – 8 Compose UI tests using `createComposeRule()` (needs a device/emulator)

Run them from Android Studio: right-click the test class → **Run**.

## Performance notes
- Network on `Dispatchers.IO`; Room runs its own background executor.
- `LazyColumn` items use stable `key`s and `contentType`s; markdown parsing and timestamp
  formatting are `remember`ed per message.
- Verified with Layout Inspector recomposition counts: older messages are not recomposed
  when a new one arrives.
