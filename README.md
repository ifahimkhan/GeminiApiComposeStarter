# Gemini Compose Chat

Assignment-1 Android application built with Kotlin, Jetpack Compose, and Material 3. It provides a Gemini chat UI with persistent local history and optional voice-to-text input.

## Setup and run

Copy `local.properties.example` to the root `local.properties` and set `GEMINI_API_KEY` to your own key. `local.properties` is ignored by Git; CI can instead supply the `GEMINI_API_KEY` environment variable. Never place a real key in source control.

Open the project in Android Studio, let Gradle sync, select an emulator/device, and run the `app` configuration. Run JVM tests with `./gradlew testDebugUnitTest` and Compose instrumented tests with `./gradlew connectedDebugAndroidTest` while an emulator is running.

## Security and storage

On first use, the app creates a 256-bit AES-GCM key in Android Keystore. It encrypts the build-provided API key and writes only ciphertext plus its IV to Preferences DataStore. The key is decrypted only in memory while creating Gemini's model, and is never displayed or logged. The app also uses Room to retain user and Gemini messages across restarts.

Voice input uses Android's `RecognizerIntent`; when available, recognized speech is inserted into the message field.

## Production note

Client-side encryption makes casual extraction harder but cannot make a client API key completely secret. A production app should send Gemini requests through a backend proxy and use restricted API keys and/or Firebase App Check.
