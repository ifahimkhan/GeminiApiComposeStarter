# Gemini API Compose Chat App

A secure Gemini-powered Android chat application built using
Kotlin and Jetpack Compose.

## Features

- Gemini API chat integration
- Material 3 chat interface
- User and assistant chat bubbles
- Persistent chat history using Room Database
- Speech-to-text input
- Dark mode
- Preferences DataStore
- Responsive/adaptive UI
- Loading indicator
- Error handling using Snackbar
- Secure API-key storage using Android Keystore
- Unit testing
- Compose UI testing
- R8/minification for release builds

## Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android ViewModel
- StateFlow
- Room Database
- Preferences DataStore
- Android Keystore
- Google Gemini API
- Kotlin Coroutines
- JUnit 4
- Compose UI Testing
- R8

## API Key Setup

The Gemini API key must NOT be hardcoded in Kotlin source code,
`strings.xml`, or committed configuration files.

Create or edit:

```text
local.properties