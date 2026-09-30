# Gemini API Compose Starter

## Configure the Gemini API key

Create an API key in [Google AI Studio](https://aistudio.google.com/app/apikey). Add it to the project-root `local.properties` file, which is ignored by Git:

```properties
GEMINI_API_KEY=<your-api-key>
```

Keep the real value private and do not commit `local.properties`. Gradle reads this property when building the app. In CI, provide `GEMINI_API_KEY` as a protected environment secret instead; Gradle uses that environment variable when the local property is missing or blank.

## API-key storage

At startup, `SecureApiKeyStorage` uses an AES-256 key held by Android Keystore and AES/GCM/NoPadding to encrypt the configured key. It stores the Base64 ciphertext and GCM initialization vector in DataStore. On later launches, it decrypts that ciphertext in memory and passes the plaintext key to the Gemini client; the decrypted value is not persisted by the storage layer. Android Keystore hardware backing depends on device support.

Client-side encryption protects the locally persisted value but does **not** make an API key embedded in a client application truly secret. For production, use a backend proxy, Firebase App Check, and/or appropriately restricted API keys.

## Chat history

Room stores user prompts and Gemini responses in the app's `chat_history.db` database, including conversation ID, role, content, and timestamp. The history is observed when the chat opens and survives app restarts. Loading and error state remain transient and are not stored in Room.

## Run the project

Open the project in Android Studio, configure the API key above, and run the app on an Android emulator or device. To build and install from a terminal with the Android SDK at its standard macOS location:

```sh
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew installDebug
```

## Tests

Run JVM unit tests with:

```sh
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew test
```

The Compose UI test is an instrumentation test and requires a running Android emulator or connected device:

```sh
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew connectedDebugAndroidTest
```
