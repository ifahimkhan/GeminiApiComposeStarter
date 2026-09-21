# Run and check your N103 assignment

## Open the correct project

In Android Studio, choose **File > Open** and select:

`/Users/dhvani/Downloads/GeminiApiComposeStarter-master`

This is the folder containing `settings.gradle.kts`, `app`, `gradlew`, and this file. Do not select the nested `GeminiApiComposeStarter` folder: that is the untouched downloaded copy.

## Run the app

1. Wait for Gradle sync to finish. If necessary, use **File > Sync Project with Gradle Files**.
2. Accept Android Studio's prompt to install required SDK components. The project compiles against SDK 36.
3. Your API key is already configured locally in the ignored `local.properties`. Do not share that file. If you rotate the key in AI Studio, replace only its `GEMINI_API_KEY` value and rebuild.
4. Open **Tools > Device Manager**, create or start a phone emulator, and wait for its home screen. You can also connect an Android phone with USB debugging enabled.
5. Select **app** and your device in Android Studio's top toolbar.
6. Click the green **Run** triangle. The app should show **Gemini chat** with **N103** below the title.

## Check the features

- Type `Explain Jetpack Compose in two sentences` and tap Send. A user bubble appears, followed by a progress indicator and the Gemini reply.
- Tap the microphone and speak. Recognized text should fill the message field; tap Send to submit. If the emulator has no speech recognizer, try a physical Android phone. The app displays an explanation when recognition is unavailable.
- Turn on **Compact bubbles**, close the app, and launch it again. The preference and messages should remain.
- Rotate the emulator and try a tablet emulator. Content should remain readable with a constrained width on large screens.
- Switch the emulator/device to dark theme and reopen the app to check Material 3 dark colors.
- Disable the test device's connection and send a message. A safe error should appear and your draft should be restored. Re-enable connectivity before trying again.
- Tap **Clear chat** to remove test conversations. This deletes local history.

## Run automated tests

In Android Studio's Terminal, from this project root:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

The second command requires an emulator or connected phone. The ordinary test suite uses fake data and does not call Gemini. A live smoke test is opt-in and consumes a small amount of API quota:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveGemini=true
```

To check both build variants:

```bash
./gradlew assembleDebug assembleRelease lintDebug
```

If the terminal cannot find Java, use Android Studio's Gradle tool window to run these tasks, or select the IDE's bundled JDK in **Settings > Build, Execution, Deployment > Build Tools > Gradle**.

## Where the code is

Under `app/src/main/java/com/fahim/geminiApiComposeStarter/`:

- `ui/chat/ChatScreen.kt`: interface, voice launcher and adaptive layout.
- `ui/chat/ChatViewModel.kt` and `ChatUiState.kt`: input, messages, progress, errors and preferences.
- `data/local/`: Room database, entity and DAO.
- `data/UserPreferences.kt`: saved compact-bubble preference.
- `security/ApiKeyVault.kt`: Keystore encryption and encrypted DataStore storage.
- `data/GeminiRepositoryImpl.kt`: Gemini requests.

Unit tests are under `app/src/test/`; emulator tests are under `app/src/androidTest/`. Read `docs/VALIDATION.md` for the actual verification results.
