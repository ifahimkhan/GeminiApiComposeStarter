# Gemini Compose Starter

An Android Jetpack Compose chat app with a black Material 3 interface, local conversation files, encrypted device-local credentials, and a StateFlow ViewModel.

## Setup

1. Open this project in Android Studio and install Android SDK Platform 36.
2. Use Gradle 9.3.1. The checked-in/local daemon configuration requests JDK 25; install it if automatic provisioning is unavailable.
3. Sync and run the app on an emulator or device with Android 8 / API 26 or newer.
4. Open the left drawer, then **Settings**. Enter your own Gemini API key and a model your account supports, then Save.
5. Type a prompt and send. Internet access is required for Gemini responses.

The existing model name is retained as the initial setting and can be changed without rebuilding. No API key is compiled into the APK. An old GEMINI_API_KEY entry in local.properties is no longer read; enter the key on the device instead. Keep local.properties for Android Studio's SDK path.

## Local data and privacy

- Conversations and drafts are saved atomically as JSON in the app's private no-backup directory. There is no remote chat database and no Room database.
- The drawer supports New chat, search, reopening a chat, and confirmed deletion. Deletion is permanent.
- Preferences DataStore stores keyboard preference, selected model, and active conversation.
- API keys are encrypted using AES-GCM with an Android Keystore key. Ciphertext lives in the no-backup directory.
- Android cloud backup is disabled for the app. Clearing app data or uninstalling removes local histories and credentials.
- Gemini requests include the selected conversation's text. Google still processes those messages remotely.
- Keystore protection improves local storage security; it does not make a user's key invulnerable on a compromised device. This is a bring-your-own-key client, not a way to hide a shared developer key.

## Speech status

The microphone and audio-mode buttons remain disabled. Audio mode is explicitly deferred.

The requested faster-whisper Python/CTranslate2 runtime does not provide the native Android integration used by this project. whisper.cpp has an official Android example and is the proposed local runtime, pending confirmation. No microphone audio is uploaded or sent to an OS/cloud recognizer.

## UI

- Muted-blue user messages align right.
- Gemini prose appears directly on black.
- Triple-backtick code/text blocks have their own container, horizontal scrolling, text selection, and Copy button.
- Swipe right across the conversation or tap the menu to open the left drawer. Swipe left, tap outside, press Back, or use Close to dismiss.
- Keyboard-on-launch can be changed in Settings.
- WindowSizeClass caps wide layouts and simplifies compact-height screens.
- Requests show loading/error feedback and prevent duplicate sends.

## Build and tests

This checkout lacks gradle/wrapper/gradle-wrapper.jar. Use an installed Gradle 9.3.1 executable directly, or regenerate the wrapper with that installation:

```powershell
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
```

After the wrapper is restored:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Alternatively replace .\gradlew.bat with your installed Gradle 9.3.1 executable. Device tests need a running emulator or connected Android device. A first run needs network access for test-runner dependencies; offline mode only works once those dependencies are cached.

To run already-built device tests directly:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.fahim.geminiApiComposeStarter.test/androidx.test.runner.AndroidJUnitRunner
```

Automated tests use fake Gemini responses. They do not consume API quota.

Reports and artifacts:

- Unit report: app/build/reports/tests/testDebugUnitTest/index.html
- Gradle device-test report: app/build/reports/androidTests/connected/debug/index.html
- Direct instrumentation results: terminal output
- Debug APK: app/build/outputs/apk/debug/app-debug.apk

Manual checks:

1. Save a key/model in Settings and send a prompt.
2. Ask a follow-up that depends on the previous message.
3. Request a fenced code block and test Copy.
4. Start another chat, reopen the first, and verify messages and drafts stay separate.
5. Force-stop and relaunch: confirm history restores.
6. Search and delete a test conversation, confirming the deletion dialog.
7. Rotate with the keyboard open and test a narrow/landscape window.
8. Disable networking, send, and verify an error appears.

## Implementation files

| File | Responsibility |
| --- | --- |
| MainActivity.kt | Wires the repository, file storage, preferences, and key vault. |
| data/ChatStorage.kt | Conversation model and atomic local JSON persistence. |
| data/AppPreferences.kt | Preferences DataStore. |
| data/ApiKeyVault.kt | Android Keystore encryption and credential removal. |
| data/GeminiRepository.kt | Testable generation contract including conversation context. |
| data/GeminiRepositoryImpl.kt | Gemini requests using a runtime key and model, with sanitized failures. |
| ui/chat/ChatUiState.kt | Messages, conversations, restoration, settings, loading, and error state. |
| ui/chat/ChatViewModel.kt | Restore/save, drafts, chat switching/deletion, settings, and API orchestration. |
| ui/chat/ChatScreen.kt | Lifecycle collection, drawer, responsive layout, composer, and bubbles. |
| ui/chat/SettingsDialog.kt | Device-local credential/model/keyboard settings. |
| ui/chat/ResponseContent.kt | Prose and copyable code/text rendering. |
| ui/text/ResponseBlocks.kt | Fenced-block parsing. |
| AndroidManifest.xml | Disables Android backup. |
| app/build.gradle.kts | Removes embedded credentials; adds DataStore and window-size-class support. |
| ChatViewModelTest.kt | Validation, requests, history restoration/switching/deletion, IDs, and context. |
| ResponseBlocksTest.kt | Prose/code separation and incomplete fences. |
| ChatScreenTest.kt | Drawer gestures, UI feedback, scrolling, and copying. |
| LocalStorageTest.kt | Real Android file round-trips and Keystore encryption/removal. |

No Gradle, AGP, Kotlin, or Gemini SDK upgrade was required.
