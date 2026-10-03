# Gemini API Compose Starter

This is an Android application developed using Kotlin and Jetpack Compose as part of the Mobile Application Development assignment.

The application uses the Google Gemini API to generate AI responses based on questions entered by the user. It also provides a simple chat interface with conversation history and voice input.

## Features

- Gemini API integration for AI-generated responses
- Chat-based user interface
- Text input for sending questions
- Voice input using Android Speech Recognition
- Conversation history during the current session
- Automatic scrolling to the latest message
- API key configuration using local properties
- Environment variable support for API key configuration
- API key encryption using Android Keystore and AES-256-GCM
- R8 optimization for release builds

## Technologies Used

- Kotlin
- Android Studio
- Jetpack Compose
- Android ViewModel
- Kotlin Coroutines
- Google Gemini API
- Android Speech Recognition
- Android Keystore
- AES-256-GCM
- Gradle
- R8

## How the Application Works

The user can enter a question in the text field or use the voice input option. The question is sent to the Gemini API, which generates a response. Both the user's question and the generated response are displayed in the chat interface.

The chat screen keeps the previous messages visible so that the user can follow the conversation.

## Voice Input

Voice input is implemented using Android's Speech Recognition functionality.

The user can use the microphone option to speak a question. The recognized speech is converted into text and can then be sent as a prompt to Gemini.

## API Key Configuration

The Gemini API key is not stored directly in the source code.

A `local.properties` file can be used to provide the API key:

`'properties
GEMINI_API_KEY=your_api_key_here

## API Key Security
The project includes an API key security implementation using Android Keystore and AES-256-GCM encryption.
The encryption key is stored using Android Keystore, while the API key can be stored in encrypted form.
Release Configuration
R8 optimization is enabled for release builds to optimize the application before generating a release version.

## Project Structure
GeminiApiComposeStarter/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           └── res/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties.example
└── README.md

## Running the Application
1. Clone the repository.
2. Open the project in Android Studio.
3. Create a local.properties file in the project root.
4. Add the Gemini API key.
5. Sync the Gradle files.
6. Run the application using an Android emulator or a connected Android device.
7. 
## Assignment Branch
The assignment work is available in the following branch:
116-MAD-Assignment1

## Author
Ujwala Chinta
