# Mobile Application Development — Lab Assignment 1

> **Compatibility patch included:** this corrected overlay forces
> `androidx.core:core` and `core-ktx` to `1.10.1` so transitive dependencies
> do not upgrade them to `1.12.0`, which would require compileSdk 34.

## Gemini Jetpack Compose Starter Enhancement (Android Studio Flamingo compatible)

This submission enhances the instructor starter project with:

- Material 3 chat bubbles in a `LazyColumn`, stable keys and auto-scroll.
- `ChatUiState` + `StateFlow` + `collectAsStateWithLifecycle()` state hoisting.
- Responsive UI using `WindowSizeClass`.
- Loading indicator and Snackbar errors.
- System dark mode + Android 12+ dynamic colour.
- Speech-to-text input with `RecognizerIntent` and `rememberLauncherForActivityResult`.
- Room database chat history that survives app restarts.
- Preferences DataStore for a saved display name.
- AES-256-GCM encryption key generated in Android Keystore with `KeyGenParameterSpec`.
- Only encrypted API-key ciphertext/IV are stored in `EncryptedSharedPreferences`.
- `local.properties` + environment-variable fallback for the Gemini key.
- R8 enabled in the release build.
- Unit tests for `ChatViewModel` and a Compose UI test.

## Flamingo compatibility

This project is pinned for Android Studio **Flamingo 2022.2.1**:

- Android Gradle Plugin: `8.0.2`
- Gradle: `8.0.2`
- Kotlin: `1.9.0`
- Compose compiler: `1.5.2`
- `compileSdk` / `targetSdk`: `33`
- JDK: `17` (Flamingo's bundled JDK works)

Do not use the current starter repository's newer SDK/AGP settings unchanged in Flamingo.

## API key setup

1. Create a Gemini API key in Google AI Studio.
2. In the project root, open `local.properties` (create it if needed).
3. Add:

   `GEMINI_API_KEY=your_real_key_here`

4. Do **not** add quotation marks.
5. Do **not** commit `local.properties`.
6. `local.properties.example` intentionally contains only a placeholder.

At build time Gradle exposes the configured key through `BuildConfig.GEMINI_API_KEY`. On first app launch, `ApiKeyVault` encrypts it with a 256-bit AES-GCM key held by Android Keystore and persists only the encrypted payload/IV. The repository decrypts it in memory only immediately before creating `GenerativeModel`.

### Security limitation

Client-side encryption raises the difficulty of casual extraction but cannot fully protect a key embedded in a mobile client. A production design should place Gemini calls behind a trusted backend proxy, or use a supported Firebase AI architecture with App Check and appropriately restricted credentials.

## Run

1. Open the project in Android Studio Flamingo.
2. Let Gradle sync finish.
3. Confirm SDK Platform 33 is installed in SDK Manager.
4. Create/start an API 33 emulator (or connect a physical Android device).
5. Run the `app` configuration.
6. Type a prompt or tap the microphone.

## Tests

From the project root on Windows:

- Unit tests: `gradlew.bat testDebugUnitTest`
- Instrumented Compose test: `gradlew.bat connectedDebugAndroidTest`

You can also run the tests from Android Studio by right-clicking the test classes.

## Pre-commit secret check

Before every commit:

1. Run `git status` and ensure `local.properties` is **not** staged.
2. Run `git diff --cached` and search for `GEMINI_API_KEY=` and the beginning of your real key.
3. Never paste the real key into Kotlin, XML, README, screenshots, Gradle files or the PR description.

## Suggested screenshots for the pull request

Capture at least:

1. Empty chat screen.
2. User + Gemini chat bubbles.
3. Loading state.
4. Voice input result in the text field.
5. Preferences dialog/display name.
6. Chat history still present after closing and reopening the app.
7. Dark mode or a tablet/landscape responsive-layout view.

## Notes on the Gemini SDK

This lab intentionally keeps the instructor starter's `com.google.ai.client.generativeai` API and `GenerativeModel` usage. For new production applications, follow Google's current migration/security guidance rather than treating this lab architecture as production credential storage.

## If the instructor repository is missing `gradle-wrapper.jar`

The upstream starter may contain `gradlew` / `gradlew.bat` without the binary wrapper JAR. Android Studio can often still sync after this overlay supplies `gradle/wrapper/gradle-wrapper.properties`, but command-line `gradlew.bat` may show:

`Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

If that happens:

1. In Android Studio Flamingo, create a temporary **Empty Compose Activity** project using AGP 8.0.x / Gradle 8.0.x.
2. Close both projects.
3. Copy only `gradle/wrapper/gradle-wrapper.jar` from the temporary project into this repository's `gradle/wrapper/` folder.
4. Keep this project's supplied `gradle-wrapper.properties` (Gradle 8.0.2).
5. Re-open the assignment project and Sync Project with Gradle Files.

Do not copy the temporary project's app code or `.idea` folder.
