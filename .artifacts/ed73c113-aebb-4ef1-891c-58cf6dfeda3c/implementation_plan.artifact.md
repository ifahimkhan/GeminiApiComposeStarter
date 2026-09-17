# Implementation Plan - Complete Assignment Requirements

This plan aims to fix the build issues and complete the remaining assignment requirements for the Gemini Chat App.

## User Review Required

> [!IMPORTANT]
> I have not found the assignment document you mentioned in the project files. I am proceeding with common requirements for a complete Gemini Chat App (Multi-turn conversation, Persistence, Unit Testing, and UI improvements). If you have specific requirements not covered here, please let me know.

## Proposed Changes

### Build and Infrastructure
Fixing the Gradle wrapper and AGP version issues to ensure the project compiles.

#### [MODIFY] [gradle-wrapper.properties](file:///Users/ananyanavani/Desktop/mad ass 1/gradle/wrapper/gradle-wrapper.properties)
- Downgrade Gradle to `8.10.2` for stability.

#### [MODIFY] [libs.versions.toml](file:///Users/ananyanavani/Desktop/mad ass 1/gradle/libs.versions.toml)
- Downgrade AGP to `8.7.3`.

#### [MODIFY] [build.gradle.kts](file:///Users/ananyanavani/Desktop/mad ass 1/app/build.gradle.kts)
- Set `compileSdk` and `targetSdk` to `35` (Android 15).
- Fix non-standard `release(36)` syntax.

### Gemini Integration Enhancements
Implementing multi-turn conversation support so the model remembers context.

#### [MODIFY] [GeminiRepository.kt](file:///Users/ananyanavani/Desktop/mad ass 1/app/src/main/java/com/fahim/geminiApiComposeStarter/data/GeminiRepository.kt)
- Add support for chat sessions and history.

#### [MODIFY] [GeminiRepositoryImpl.kt](file:///Users/ananyanavani/Desktop/mad ass 1/app/src/main/java/com/fahim/geminiApiComposeStarter/data/GeminiRepositoryImpl.kt)
- Implement `startChat` and message history tracking.

#### [MODIFY] [ChatViewModel.kt](file:///Users/ananyanavani/Desktop/mad ass 1/app/src/main/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModel.kt)
- Update to maintain conversation state and pass it to the repository.

### Persistence (Assignment Requirement)
Adding Room database to save chat history across app restarts.

#### [NEW] [ChatDatabase.kt](file:///Users/ananyanavani/Desktop/mad ass 1/app/src/main/java/com/fahim/geminiApiComposeStarter/data/local/ChatDatabase.kt)
#### [NEW] [ChatMessageEntity.kt](file:///Users/ananyanavani/Desktop/mad ass 1/app/src/main/java/com/fahim/geminiApiComposeStarter/data/local/ChatMessageEntity.kt)

### Testing
Adding unit tests to verify ViewModel logic.

#### [NEW] [ChatViewModelTest.kt](file:///Users/ananyanavani/Desktop/mad ass 1/app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt)

### Documentation
#### [NEW] [README.md](file:///Users/ananyanavani/Desktop/mad ass 1/README.md)
- Add project setup, features, and assignment completion details.

## Verification Plan

### Automated Tests
- Run `./gradlew test` to verify ViewModel logic.

### Manual Verification
- Deploy to an emulator/device.
- Verify chat history persists after restart.
- Verify Gemini responds correctly to follow-up questions (context awareness).
