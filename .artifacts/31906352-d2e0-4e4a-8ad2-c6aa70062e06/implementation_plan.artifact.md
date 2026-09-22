# Implementation Plan - Testing and Optimization

Implement unit tests for `ChatViewModel` using `kotlinx-coroutines-test` and a fake `GeminiRepository`, and write Compose UI tests using `createComposeRule()` for `ChatScreen`.

## User Review Required

> [!IMPORTANT]
> - Unit Testing: We will create `ChatViewModelTest.kt` using `StandardTestDispatcher` / `TestDispatcher`, `runTest`, and a fake implementation of `GeminiRepository`.
> - UI Testing: We will create `ChatScreenTest.kt` using `createComposeRule()` to verify rendering of chat messages, loading indicators, and user interactions.

## Proposed Changes

### Unit Tests

#### [NEW] [ChatViewModelTest.kt](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt)
- Implement unit tests covering:
  - Initial state and loading prompt changes.
  - Sending a prompt successfully and updating messages.
  - Handling API error states and missing API key messages.

### Compose UI Tests

#### [NEW] [ChatScreenTest.kt](file:///C:/Users/DELL/AndroidStudioProjects/N116_Akshat_Assignment1/app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt)
- Implement Compose UI tests covering:
  - Chat screen rendering initial state.
  - Chat bubbles (user vs Gemini) display correctly.
  - Loading indicator display when `isLoading = true`.

## Verification Plan

### Automated Tests
- Run unit tests: `gradle_build("app:testDebugUnitTest")`.
- Run instrumented UI tests: `gradle_build("app:connectedAndroidTest")` or unit test verification.
