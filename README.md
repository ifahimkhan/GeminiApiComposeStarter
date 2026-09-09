\# Gemini API Compose Starter



An enhanced Android chat application built with Kotlin and Jetpack Compose using the Gemini API.



\## Features



\- Gemini AI chat using `gemini-3.6-flash`

\- Material 3 chat interface

\- LazyColumn-based conversation history

\- User and AI chat bubbles

\- Loading and error states

\- Voice input using Android `RecognizerIntent`

\- Persistent chat history using Room

\- State hoisting with `ChatUiState` and `StateFlow`

\- Lifecycle-aware state collection

\- Responsive UI using Material 3 WindowSizeClass

\- Dark mode and dynamic colors

\- AES-256-GCM encryption for the API key

\- Android Keystore-based encryption key management

\- Unit tests for `ChatViewModel`

\- Compose UI tests

\- Release build minification



\## API Key Setup



The Gemini API key must never be hardcoded into Kotlin, XML, or committed Gradle files.



Create a local `local.properties` file in the project root:



&#x20;   GEMINI\_API\_KEY=your\_actual\_api\_key



The `local.properties` file is ignored by Git and must never be committed.



A safe template is provided in:



&#x20;   local.properties.example



It contains only:



&#x20;   GEMINI\_API\_KEY=your\_gemini\_api\_key\_here



\## API Key Security



On the first application launch, the API key is read from the build configuration and stored locally in encrypted form.



The security flow is:



1\. Read the API key from `local.properties` or the environment.

2\. Generate an AES-256 key using Android Keystore.

3\. Encrypt the API key using AES-256-GCM.

4\. Store only the encrypted value in Preferences DataStore.

5\. Decrypt the API key only in memory when creating the Gemini client.

6\. Never log or display the decrypted API key.



The Android Keystore protects the encryption key while DataStore stores only the encrypted API key.



\### Important Limitation



Because this application communicates directly with Gemini from the Android client, client-side encryption does not make the API key completely secret from a determined attacker who controls or inspects the device.



For a production application, the recommended approach is to use a backend proxy so that the Gemini API key remains on the server. Additional protections such as Firebase App Check and API key restrictions should also be considered.



\## Architecture



The application follows a simple separation of concerns:



\- UI: Jetpack Compose screens and Material 3 components

\- ViewModel: UI state, validation and chat operations

\- Repository: Gemini API communication

\- Room: Persistent chat history

\- DataStore: Encrypted API key storage

\- Android Keystore: AES-256 encryption key management



\## Testing



Unit tests cover:



\- Empty prompt validation

\- Successful Gemini response

\- Repository failure handling

\- Missing API key handling



Run unit tests with:



&#x20;   ./gradlew test



On Windows PowerShell:



&#x20;   .\\gradlew.bat test



Compose UI tests can be executed on a connected emulator or Android device:



&#x20;   .\\gradlew.bat connectedAndroidTest



\## Performance



The application uses:



\- LazyColumn for efficient chat rendering

\- Stable item keys

\- StateFlow for controlled state updates

\- Lifecycle-aware state collection

\- Stateless Compose UI components where applicable

\- Coroutine-based Room database operations

\- Suspend-based Gemini network operations

\- Loading-state protection against duplicate requests



No blocking network or database operations are performed on the main UI thread.



\## Build



Debug build:



&#x20;   .\\gradlew.bat assembleDebug



Release build:



&#x20;   .\\gradlew.bat assembleRelease



Release builds use code shrinking and resource optimization through R8.



\## Project Structure



&#x20;   app/

&#x20;     src/

&#x20;       main/

&#x20;         java/

&#x20;           .../data/

&#x20;           .../security/

&#x20;           .../ui/chat/

&#x20;         androidTest/

&#x20;         test/



Important components:



\- `MainActivity.kt` - Application entry point

\- `ChatScreen.kt` - Compose UI

\- `ChatViewModel.kt` - Chat state and logic

\- `GeminiRepositoryImpl.kt` - Gemini API integration

\- `SecureApiKeyStore.kt` - API key encryption and storage

\- `AppDatabase.kt` - Room database

\- `ChatMessageDao.kt` - Chat history DAO

\- `ChatMessageEntity.kt` - Room entity



\## Git and Secrets



The repository excludes:



\- `local.properties`

\- Android Studio project-specific files

\- Gradle build output

\- Signing keys



Before creating a pull request, verify that no API key or other secret is present in the Git history or staged changes.
