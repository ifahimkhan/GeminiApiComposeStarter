# Gemini Chat App - Jetpack Compose

A fully functional Gemini AI chat application built with Kotlin and Jetpack Compose. This project demonstrates modern Android development practices, including StateFlow, Room persistence, and Google's Generative AI SDK.

## Features

- **Gemini AI Integration**: Real-time text generation using `gemini-1.5-flash`.
- **Multi-turn Conversation**: The app maintains chat history, allowing for context-aware follow-up questions.
- **Persistent Chat History**: All messages are saved locally using a Room database, so your conversations remain available after app restarts.
- **Modern UI/UX**:
    - Clean Material 3 design with chat bubbles.
    - Automatic scrolling to the newest message.
    - Loading indicators while the AI is thinking.
    - Error handling via SnackBar and text field validation.
- **Privacy & Management**: Option to clear chat history from the top navigation bar.
- **Robust Build Setup**: Configured with Gradle 9.3.1, AGP 8.9.1, and KSP for optimal performance and compatibility.

## Setup Instructions

1. **Get a Gemini API Key**: Visit [Google AI Studio](https://aistudio.google.com/app/apikey) to generate your free API key.
2. **Configure Local Properties**:
    - Create a file named `local.properties` in the root directory (if not already present).
    - Add the following line:
      ```properties
      GEMINI_API_KEY=your_api_key_here
      ```
3. **Build & Run**: Open the project in Android Studio (Ladybug or newer) and run it on an emulator or physical device.

## Technologies Used

- **Kotlin**: Primary programming language.
- **Jetpack Compose**: Declarative UI toolkit.
- **Room Database**: Local data persistence for chat history.
- **KSP (Kotlin Symbol Processing)**: For efficient Room compilation.
- **Coroutines & Flow**: For asynchronous operations and reactive UI state management.
- **Google Generative AI SDK**: Direct integration with Gemini models.
- **MockK**: For comprehensive unit testing of the ViewModel logic.

## Project Structure

- `data/`: Contains the Gemini repository implementation and Room database setup.
- `ui/`: Contains the Compose UI components, themed with Material 3.
- `ui/chat/`: Chat-specific screens, StateFlow-based ViewModel, and UI state models.
- `test/`: Unit tests for the business logic.

---
*Developed as part of the Android Jetpack Compose Assignment.*
