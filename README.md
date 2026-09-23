# MAD Assignment 1 - Gemini Chat Application

**Student:** Neelansh Pandey  
**Roll Number:** N071  
**Assignment:** Mobile Application Development - Assignment 1

## Overview

This project is an Android chatbot application developed using Kotlin
and Jetpack Compose. The application integrates the Gemini API and
provides persistent conversation storage, encrypted API-key handling,
user preferences, voice dictation, and light/dark theme support.

## Features

- Gemini API integration
- Jetpack Compose user interface
- Room Database message persistence
- AES-based API key encryption
- Preferences DataStore
- Light and Dark Mode
- Voice Dictation
- Persistent user name/preferences
- MVVM architecture

## API Key Storage and Security

The Gemini API key is not used directly throughout the application.

The application implements an encryption layer responsible for
protecting the API key before it is used by the Gemini repository.

### Encryption Flow

The general encryption/decryption flow is:

API Key
↓
Encryption Layer
↓
Encrypted Representation
↓
Secure Local Storage
↓
Decryption when required
↓
GeminiRepositoryImpl
↓
Gemini API

The decrypted key is obtained only when it is required to initialize
the Gemini API client.

## Room Database

Chat messages are persisted locally using Room Database.

The persistence layer consists of:

- `ChatEntity.kt` - Defines the database entity representing a message.
- `ChatDao.kt` - Provides database operations.
- `ChatDatabase.kt` - Defines and initializes the Room database.
- `ChatViewModel.kt` - Coordinates database operations with the UI.

This allows conversation history to survive application restarts.

## User Preferences

The application uses Android Preferences DataStore to persist user
configuration.

The user's name can be configured through the Preferences interface and
is restored when the application is reopened.

## Light and Dark Mode

The chatbot provides both light and dark themes.

The interface dynamically changes its colours to maintain appropriate
contrast. Text entered into the message field is displayed using dark
text in light mode and light text in dark mode.

## Voice Dictation

Voice input is supported through Android's speech recognition
functionality.

The application requests microphone permission when required and
converts recognized speech into text that can be inserted into the
chat input field.

## Architecture

The project follows an MVVM-oriented structure:

UI (Jetpack Compose)
↓
ChatViewModel
↓
Repository / Persistence Layer
↓
Gemini API + Room Database + DataStore

This separation keeps UI logic, application state, persistent storage,
and external API communication modular.

## Technologies Used

- Kotlin
- Jetpack Compose
- Gemini API
- Room Database
- Preferences DataStore
- Kotlin Coroutines / Flow
- AES Encryption
- Android Speech Recognition
- Gradle / KSP