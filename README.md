<img width="408" height="908" alt="importfiles-2" src="https://github.com/user-attachments/assets/89559837-68a6-4808-92d5-e8b7767e0a59" /># Gemini Compose Chat

A Jetpack Compose Android chat app built around the Gemini API. The project started as a Gemini chat starter and was extended into a polished ChatGPT-style mobile experience with local chat history, file/image attachments, Markdown rendering, search, voice input, and an experimental voice conversation mode.

The app uses a minimal black-first UI, Material 3 components, Room persistence, DataStore settings, and a `StateFlow` based `ChatViewModel`.

## Demo media

DEMO VIDEO : 
https://github.com/user-attachments/assets/06116dfb-3402-4732-b078-6c8350fa865e

<img width="720" height="1600" alt="mainscreen-1" src="https://github.com/user-attachments/assets/4df38fc8-d594-40fd-8592-fe13b9dea1c8" />
<img width="408" height="908" alt="importfiles-2" src="https://github.com/user-attachments/assets/d0229963-d4cb-46fd-97cb-bac1ff1977e9" />
<img width="415" height="903" alt="selectfile-3" src="https://github.com/user-attachments/assets/46eb8a6c-68b0-42b1-8aa2-fe9998cde7ca" />
<img width="408" height="898" alt="importedpic-4" src="https://github.com/user-attachments/assets/241b6538-9545-41c1-9f49-94ae4c32f358" />
<img width="410" height="902" alt="prompting-5" src="https://github.com/user-attachments/assets/b14bf724-309f-4e85-b117-fb1809347149" />
<img width="402" height="903" alt="prompted-6" src="https://github.com/user-attachments/assets/64b0a3a3-aea9-4c84-a727-63332141ad4e" />
<img width="397" height="903" alt="response-7" src="https://github.com/user-attachments/assets/22dd5f8b-9dfe-4994-9bb9-5a0a7fc7f7d5" />
<img width="410" height="900" alt="extrafucntionswithresponse-8" src="https://github.com/user-attachments/assets/b9778523-585e-4dc8-ba31-d35728d94e6e" />
<img width="407" height="906" alt="sidepanel-9" src="https://github.com/user-attachments/assets/997c740f-0a42-495d-815c-da2c30aca48b" />
<img width="417" height="902" alt="searchfunc-10" src="https://github.com/user-attachments/assets/ed485862-72a2-4c2a-88c3-32c1428b45ed" />
<img width="407" height="900" alt="searched-11" src="https://github.com/user-attachments/assets/f3da529b-0708-4adf-9638-bfb9c90d48ce" />
<img width="720" height="1600" alt="voicemode-12" src="https://github.com/user-attachments/assets/30e97e57-cd67-410c-aab4-9231d303bf2f" />


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

