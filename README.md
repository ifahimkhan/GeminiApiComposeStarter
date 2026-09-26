# Gemini API Compose Starter

A Gemini-powered Android chat application built using Kotlin and Jetpack Compose.

## Features

- Gemini AI text generation
- Conversation-style chat UI
- Material 3 chat bubbles
- LazyColumn with stable message keys
- Automatic scrolling to the latest message
- Voice input using Android Speech Recognition
- Room database for persistent conversation history
- Preferences DataStore for user preferences
- Responsive UI using WindowSizeClass
- Loading and error states
- Secure API-key handling using Android Keystore
- AES-256-GCM encryption for persisted API-key ciphertext
- Release build configured with R8/minification

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Jetpack
- Gemini Generative AI SDK
- Room
- Preferences DataStore
- Android Keystore
- AES-256-GCM
- Gradle Version Catalog

## API Key Setup

The Gemini API key must never be hardcoded in Kotlin, XML, or Gradle source files.

For local development, create a `local.properties` file in the project root:

```properties
GEMINI_API_KEY=your_api_key_here