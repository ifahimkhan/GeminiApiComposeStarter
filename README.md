# Gemini Compose Chat — C010 Veer Nagda (Div B)

## 1. Where to put the API key
1. Get a key at https://aistudio.google.com/app/apikey
2. `cp local.properties.example local.properties`
3. Add `GEMINI_API_KEY=your_api_key_here`
`local.properties` is git-ignored; the key is exposed only as
`BuildConfig.GEMINI_API_KEY` and never appears in source or resources.

## 2. How the encryption flow works
BuildConfig value -> (first launch) CryptoManager.encrypt() with an
AES-256-GCM key generated inside the Android Keystore -> iv + ciphertext
stored in DataStore. On later launches only the ciphertext is read and
decrypted in memory at the moment GenerativeModel is created. The plaintext
is never logged, toasted or written to disk. Release builds use
`isMinifyEnabled = true` so R8 obfuscates the code path.

## 3. Known limits of client-side protection
A key shipped inside an APK can always be recovered by a determined attacker
(hooking the process with Frida, dumping memory, or intercepting TLS on a
rooted device). Encryption only raises the cost. A production app would:
  * move Gemini calls behind a backend proxy that holds the key server-side;
  * attest callers with Firebase App Check;
  * use restricted API keys with per-app/package and quota limits;
  * rotate keys and monitor usage in AI Studio.

## 4. Running the tests
    ./gradlew testDebugUnitTest          # ChatViewModel + CryptoManager
    ./gradlew connectedDebugAndroidTest  # Compose UI tests (emulator required)
