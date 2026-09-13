# Gemini AI Chat

Student:
Harsh Baniya

Roll Number:
C020

## Project Overview
This application is a secure Gemini-powered chat interface built with Jetpack Compose. It features persistent chat history using Room, user preferences using DataStore, and a robust security layer for protecting the Gemini API key.

## Features
- **Secure Gemini Integration**: AES-256-GCM encryption for the API key.
- **Persistent Chat History**: All conversations are stored in a Room database and survive app restarts.
- **User Preferences**: Choose between Simple, Detailed, or Technical response styles.
- **Voice Input**: Integrated speech-to-text for hands-free prompting.
- **Responsive UI**: Adaptive layout for phone, tablet, portrait, and landscape modes using Material 3.
- **Dark Mode Support**: Full support for system-wide dark and light themes.

## Security Architecture (AES-256-GCM)
The application implements a mandatory secure flow for the Gemini API key:
1. **Source**: The key is read from `local.properties` (or environment variable) during build.
2. **Transfer**: The key is exposed via `BuildConfig` only during the initial secure configuration.
3. **Encryption**: On first run, the key is encrypted using **AES-256-GCM** with a key generated and stored in the **Android Keystore**.
4. **Persistence**: Only the encrypted ciphertext is saved in **Preferences DataStore**.
5. **Decryption**: The plaintext key is decrypted **only in memory** and only when creating the `GenerativeModel` for a request.

## How to Configure
1. Create a `local.properties` file in the root directory.
2. Add your Gemini API key:
   ```
   GEMINI_API_KEY=your_actual_api_key_here
   ```
3. Never commit `local.properties` to version control. Refer to `local.properties.example` for the format.

## Technologies Used
- **Jetpack Compose**: For the modern, declarative UI.
- **Material 3**: For styling and adaptive components.
- **Kotlin Coroutines & Flow**: For asynchronous operations.
- **Room Database**: For local message persistence.
- **Preferences DataStore**: For storing user settings and encrypted secrets.
- **Android Keystore**: For hardware-backed security.
- **Google Generative AI SDK**: For Gemini API communication.

## How to Run
1. Open the project in Android Studio (Ladybug or newer).
2. Configure your API key in `local.properties`.
3. Sync Gradle and run the `:app` module on an emulator or physical device.

## Testing
### Unit Tests
Run the `ChatViewModel` tests to verify business logic:
```bash
./gradlew :app:testDebugUnitTest
```

### Compose UI Tests
Run the UI tests to verify component visibility and student information:
```bash
./gradlew :app:connectedAndroidTest
```

## Production Security Discussion
For a production application, additional security measures should be considered:
- **Backend Proxy**: Move API calls to a secure backend server to avoid shipping any API keys (even encrypted) in the client binary.
- **Firebase App Check**: If using Firebase, App Check can ensure only your authentic app can access the AI services.
- **Restricted API Keys**: Ensure the Gemini API key is restricted to specific IP addresses or Android package names in the Google Cloud Console.
