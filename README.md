# Gemini API Compose Starter

This is a starter project for the Mobile Application Development LAB ASSIGNMENT-1.

## Features Implemented
* Secure API key handling via `local.properties`.
* LazyColumn Material 3 chat UI with auto-scroll.
* StateFlow and State hoisting for UI state.
* Responsive WindowSizeClass layout (adapts max-width on Expanded screens).
* Loading and error states.
* Voice input using `RecognizerIntent`.
* DataStore Preferences to save user configurations (e.g. User Name).
* Room Database for persisting chat history.
* Unit tests using `kotlinx-coroutines-test` and UI tests using Compose Test rules.
* R8 minification enabled for release.
* Stability improvements via `@Immutable` annotations.

## Build Instructions
1. Get a Gemini API key.
2. Create `local.properties` in the root folder and add:
   `GEMINI_API_KEY=your_api_key_here`
3. Build and run using Android Studio.