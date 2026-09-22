\# N081 — Gemini Chat Assignment



An Android chat application built with Kotlin, Jetpack Compose, and the Gemini API.



\## Features



\- Send text prompts and display Gemini responses.

\- Display conversations in a LazyColumn with stable message IDs.

\- Automatically scroll to new messages.

\- Show loading indicators and error messages.

\- Prevent duplicate requests while a response is loading.

\- Enter prompts using speech recognition.

\- Choose System, Light, or Dark theme.

\- Save theme preferences using DataStore.

\- Save completed conversations using Room.

\- Restore saved conversations when the app starts.

\- Adapt layout spacing and message widths to window size.



\## Architecture



The application separates UI, state management, and data access:



\- \*\*ChatScreen:\*\* Displays state and forwards user actions.

\- \*\*ChatViewModel:\*\* Manages StateFlow, validation, loading, and responses.

\- \*\*GeminiRepository:\*\* Provides an interface for generating responses.

\- \*\*GeminiRepositoryImpl:\*\* Makes Gemini API requests.

\- \*\*UserPreferencesRepository:\*\* Persists theme preferences.

\- \*\*ChatHistoryRepository:\*\* Loads and saves conversations.

\- \*\*ApiKeyStorage:\*\* Encrypts the API key using Android Keystore.



The UI observes ViewModel state using lifecycle-aware collection.

Asynchronous operations use Kotlin coroutines.



\## Setup



1\. Clone this repository and open its root folder in Android Studio.

2\. Install the Android SDK version requested by the project.

3\. Use the Gradle JDK configured for the project.

4\. Add your Gemini API key to the root `local.properties` file:



&#x20;  ```properties

&#x20;  GEMINI\_API\_KEY=YOUR\_API\_KEY\_HERE

&#x20;  ```



&#x20;  Preserve any existing `sdk.dir` entry.



5\. Sync the project with Gradle.

6\. Select an emulator or connected Android device.

7\. Run the `app` configuration.



The build also supports the `GEMINI\_API\_KEY` environment variable

as a fallback when the local property is unavailable.



API-key changes require rebuilding the app.



Do not commit `local.properties` or a real API key.

The example properties file must contain placeholders only.



\## Using the App



1\. Type a message into the input field.

2\. Tap Send.

3\. Wait for the response to appear.

4\. Tap the microphone to use speech recognition.

5\. Review the recognized text before sending it.

6\. Use the theme controls to change the appearance.



Speech recognition requires a compatible recognition service

on the device.



Completed exchanges are saved locally. If a request fails,

the prompt is restored so it can be retried.



\## Tests



\### Local ViewModel tests



Run from the project root in Windows PowerShell:



```powershell

.\\gradlew.bat :app:testDebugUnitTest --tests "com.fahim.geminiApiComposeStarter.ChatViewModelTest"

```



The eight ViewModel tests cover:



1\. Loading saved history.

2\. Rejecting an empty prompt.

3\. Rejecting requests without an API key.

4\. Displaying and saving a successful response.

5\. Showing loading state and preventing duplicate requests.

6\. Restoring the prompt after a failed request.

7\. Keeping a response visible when saving fails.

8\. Preventing requests when chat history cannot be loaded.



These tests use a fake Gemini repository and mocked storage.

They require neither an emulator nor a real API key.



Open the report:



```powershell

Start-Process ".\\app\\build\\reports\\tests\\testDebugUnitTest\\index.html"

```



Recorded result: \*\*8 tests passed, 0 failures, 0 skipped.\*\*



\### Instrumented tests



Start an emulator or connect an Android device, then run:



```powershell

.\\gradlew.bat :app:connectedDebugAndroidTest

```



Compose UI tests check chat rendering, input, loading state,

theme selection, voice callbacks, and error presentation.



The voice callback test checks the button callback; it does

not verify the device's speech-recognition service.



Open the report:



```powershell

Start-Process ".\\app\\build\\reports\\androidTests\\connected\\debug\\index.html"

```



The instrumented test run completed successfully with eight tests.



\## API-Key Security



The app encrypts its stored API key using AES-256-GCM with

a key managed by Android Keystore. DataStore stores the

ciphertext and initialization vector. The API key is decrypted

when creating the Gemini model.



The encrypted key storage is excluded from Android backup.

Release builds enable R8 code shrinking and obfuscation.



\### Limitations



These controls do not make a client-side API key impossible

to extract.



The API key supplied during the build is included in the

application through BuildConfig. An attacker may recover it

from the application or inspect it at runtime. R8 obfuscation

does not provide secret storage.



For production, Gemini requests should go through an

authenticated backend that holds the API key. The backend

should enforce authorization, rate limits, and usage quotas.

App attestation, such as App Check where supported, can provide

an additional protection layer.



Any exposed API key must be revoked and replaced.



API-key encryption does not encrypt the Room conversation

database. Chat history is stored in the app's private storage.



\## Screenshots



App screenshots and test-result screenshots are stored in:



`submission/screenshots/`



The evidence includes light theme, dark theme, landscape layout,

voice input, restored chat history, and test results.



\## Current Limitations



\- Generating responses requires an internet connection and a valid API key.

\- Speech recognition availability depends on the device.

\- The stored conversation is displayed locally; requests currently send

&#x20; the individual prompt rather than the full conversation history.

\- Uninstalling the app or clearing its data removes local history

&#x20; and preferences.

