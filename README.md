# Gemini MAD Lab Assignment 1 - C025

## Setup Instructions
1. Obtain a Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
2. In the root directory of this project, create a file named `local.properties`.
3. Add the following line to that file:
   `GEMINI_API_KEY=your_actual_api_key_here`
4. Build and Run the project on an Android device.

## Security Implementation
To satisfy the mandatory security requirements:
- **Encryption at Rest**: On the first launch, the API key is read from `BuildConfig` and encrypted using **AES-256-GCM**.
- **Android Keystore**: The encryption key is generated and stored securely inside the Android Keystore System (`KeyGenParameterSpec`), ensuring it cannot be extracted.
- **Persistence**: The resulting ciphertext is persisted in **Preferences DataStore**.
- **In-Memory Decryption**: The API key is only decrypted in memory at the exact moment the `GenerativeModel` is initialized. It is never logged or displayed.
- **Obfuscation**: R8 minification is enabled (`isMinifyEnabled = true`) to prevent easy reverse engineering of the key handling logic.

## Features
- **UI**: Material 3 Chat interface using `LazyColumn` with auto-scroll and stable keys.
- **Responsive Design**: Uses `LocalConfiguration` to adapt layouts for different screen widths.
- **Dark Mode**: Full support for Dark Theme and Material 3 Dynamic Colors.
- **Voice Modality**: Speech-to-Text integration via `RecognizerIntent`.
- **History**: Integrated **Room Database** to persist chat conversations across app restarts.

## How to Run Tests
- **Unit Tests**: Run `ChatViewModelTest.kt` to verify ViewModel logic and the Fake Repository.
- **UI Tests**: Connect a physical device and run `ChatUiTest.kt` to verify the Compose UI elements.