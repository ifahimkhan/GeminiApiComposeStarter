# Gemini Compose Chat

A Jetpack Compose Android chat app built around the Gemini API. The project started as a Gemini chat starter and was extended into a polished ChatGPT-style mobile experience with local chat history, file/image attachments, Markdown rendering, search, voice input, and an experimental voice conversation mode.

The app uses a minimal black-first UI, Material 3 components, Room persistence, DataStore settings, and a `StateFlow` based `ChatViewModel`.

## Demo media

Add screenshots and video here:

- Screenshot 1: main chat screen
- Screenshot 2: attachment picker / image preview
- Screenshot 3: left chat history panel
- Screenshot 4: search highlights
- Video: full app walkthrough

## Main features

- ChatGPT-like dark interface with a clean black background and minimal chrome.
- Material 3 chat layout with right-aligned muted-blue user messages.
- Gemini responses render as open text on the black background instead of heavy containers.
- Markdown-style response rendering with formatted prose and separated copyable code/text blocks.
- Copy, share, and regenerate controls on responses.
- Loading and error states while Gemini is responding.
- Haptic feedback on important buttons and interactions.
- Light/dark mode support through app settings.
- Responsive Compose layout for different phone sizes and orientations.

## Chat history

- Chats are saved locally on the device using Room.
- Chat titles are generated from the first user prompt instead of staying as generic names.
- Left-side ChatGPT-style history panel.
- Create new chats.
- Reopen previous chats.
- Delete chats with confirmation.
- Search chats from the side panel.
- Search inside the current conversation.
- Matching words in chat results can be highlighted.
- Chats can be exported/shared as a `.md` Markdown file.

## Attachments and image features

- Attach images from Photos.
- Attach files from the Android file picker.
- Capture images from the camera.
- Selected images show as small previews in the input bar.
- Sent images appear above the user prompt in the chat.
- Gemini can answer questions about attached images/files when supported by the selected model.
- Image generation UI is included with loading/error states.

Note: image generation depends on Google AI Studio model access and quota. If the image model shows `0 / 0` quota in Google AI Studio, image generation will fail even if normal text chat works.

## Voice features

- Microphone input can be used to dictate prompts into the text box.
- Live voice conversation mode is included.
- Voice mode opens into a dedicated full-screen orb UI.
- The voice mode attempts automatic listening, short Gemini replies, and text-to-speech playback.

Known limitation: the voice conversation mode is experimental and can feel clunky. It currently uses Android speech recognition, normal Gemini text generation, and Android text-to-speech, so it is not true realtime voice. Latency and occasional failed turns may happen depending on network speed, device speech recognition behavior, and Gemini response time.

## Models

- Text chat uses the editable model name stored in Settings.
- The current default text model in the project is `gemini-3.6-flash`.
- Image generation uses `gemini-2.5-flash-image`.

You can change the text model from the in-app Settings dialog.

## Local data and privacy

- Conversation history is stored locally in a Room database.
- Preferences DataStore stores app preferences such as selected model, keyboard behavior, active conversation, and theme choice.
- The Gemini API key is stored on-device using Android Keystore-backed encryption.
- Android cloud backup is disabled for app data.
- Clearing app data or uninstalling the app removes saved chats, preferences, and credentials.
- Attached/generated media is stored or referenced locally on the device.

## Setup

1. Open the project in Android Studio.
2. Install the required Android SDK platform if Android Studio asks for it.
3. Get a Gemini API key from Google AI Studio.
4. Add your key to `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

5. Sync Gradle.
6. Run the app on an emulator or Android device.

Internet access is required for Gemini text responses and image generation.

## Running tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

For connected Android tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Key implementation files

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Wires app dependencies, repository, storage, preferences, and API key vault. |
| `data/GeminiRepository.kt` | Gemini generation contract. |
| `data/GeminiRepositoryImpl.kt` | Gemini text, attachment, and image generation requests. |
| `data/RoomChatStorage.kt` | Room-backed local chat persistence. |
| `data/AppPreferences.kt` | DataStore settings. |
| `data/ApiKeyVault.kt` | Android Keystore API key encryption. |
| `ui/chat/ChatUiState.kt` | Chat screen state models. |
| `ui/chat/ChatViewModel.kt` | Chat orchestration, sending, regeneration, title generation, history, and voice response handling. |
| `ui/chat/ChatScreen.kt` | Main Compose chat UI, drawer, composer, bubbles, attachments, search, sharing, and actions. |
| `ui/chat/LiveVoiceScreen.kt` | Experimental voice conversation mode. |
| `ui/chat/ResponseContent.kt` | Formatted response rendering and copyable blocks. |
| `ui/chat/SettingsDialog.kt` | API key/model/theme/settings UI. |

## Current limitations

- Voice conversation mode is not true realtime voice and can have noticeable latency.
- Image generation requires model access and quota from Google AI Studio.
- Some attached file types may not be useful to Gemini depending on model support.
- The app is designed as a student assignment/project prototype, not a production chat client.

