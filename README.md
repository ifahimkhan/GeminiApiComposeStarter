# Ahum AI — Gemini Jetpack Compose client

Lab Assignment 1, Mobile Application Development (702AI0E002).
Built on [`ifahimkhan/GeminiApiComposeStarter`](https://github.com/ifahimkhan/GeminiApiComposeStarter).

**Aadidev Raizada — Roll No. N152, Class E, Batch E2**
Branch: `N152_MAD_E2_ASSIGNMENT1`

---

## What this adds to the starter

| Area | Change |
| --- | --- |
| Conversation | Single prompt/response box replaced with a real multi-turn chat: `LazyColumn` of Material 3 bubbles, stable Room-row keys, auto-scroll to the newest turn |
| Persistence | Room stores the transcript; Preferences DataStore stores theme settings. Both survive app restarts |
| Security | API key encrypted at rest with an AES-256-GCM key held in the Android Keystore; only ciphertext is persisted |
| Multi-turn context | Prior turns are replayed to Gemini via `startChat(history = …)`, so follow-up questions work |
| Voice input | `RecognizerIntent` launched through `rememberLauncherForActivityResult` |
| Responsive | `WindowSizeClass` drives content width, bubble width, padding and mascot size |
| Theming | AhumLabs teal/slate/indigo Material 3 palette, light + dark, with an optional dynamic-colour toggle |
| Mascot | Animated assistant character driven by app state (see [Mascot](#mascot)) |
| Tests | 11 `ChatViewModel` unit tests + 7 Compose UI tests |

## Screenshots

| Multi-turn reply with markdown | Landscape / responsive | Keyboard open, header collapsed |
| --- | --- | --- |
| ![](docs/screenshots/01-response-markdown.jpg) | ![](docs/screenshots/02-landscape-responsive.jpg) | ![](docs/screenshots/03-keyboard-mascot-collapsed.jpg) |

---

## Running it

```bash
cp local.properties.example local.properties
# then edit local.properties and set GEMINI_API_KEY=<your key>
./gradlew installDebug
```

Get a key from [Google AI Studio](https://aistudio.google.com/app/apikey).

The app targets a **physical device over USB debugging**. Confirm the phone is attached and
authorised before installing:

```bash
adb devices -l
```

---

## Where the key lives, and how the encryption works

### 1. Build time

`app/build.gradle.kts` reads the key from `local.properties`, falling back to a `GEMINI_API_KEY`
environment variable so CI can inject a repository secret instead of shipping a file:

```kotlin
val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: "").trim()
```

It is exposed as `BuildConfig.GEMINI_API_KEY`. It never appears as a literal in a Kotlin file, in
`strings.xml`, or in any committed Gradle file. `local.properties` is in `.gitignore` and must stay
there; `local.properties.example` is the committed placeholder.

If no key is configured the build still succeeds — the app then shows
"GEMINI_API_KEY is missing…" instead of crashing.

### 2. First launch — encrypt

`security/ApiKeyStore.kt`:

1. Generates an AES-256-GCM key inside the **Android Keystore** via `KeyGenParameterSpec`
   (alias `gemini_api_key_aes`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, 256-bit). The key
   material is generated in, and cannot be exported from, the Keystore.
2. Encrypts `BuildConfig.GEMINI_API_KEY` with it.
3. Persists **only** the Base64 ciphertext and IV in a DataStore named `secure_store`.

### 3. Every later launch — decrypt in memory

`AppContainer` hands `GeminiRepositoryImpl` a `suspend () -> String` rather than a `String`. The
key is therefore decrypted at exactly one moment — when the `GenerativeModel` is constructed — and
is never stored in a field, logged, toasted or rendered.

### 4. Release build

`isMinifyEnabled = true` and `isShrinkResources = true`, so R8 obfuscates the release APK. The
ProGuard rules also strip every `android.util.Log` call from release, so no code path can print a
decrypted value even by accident.

### 5. What this does *not* do

Client-side encryption raises the bar; it does not defeat a determined attacker. The ciphertext,
the Keystore-backed decrypt operation and the plaintext in process memory all live on a device the
attacker controls. On a rooted or instrumented device the key can be recovered from memory or by
invoking the Keystore operation directly, and the request can always be read off the wire from a
device with a user-installed CA.

A production app would not ship the key at all. It would either:

- **Proxy the calls**: the app talks to a backend you own; the backend holds the Gemini key in a
  secret manager, authenticates the user, applies per-user rate limits, and forwards the request.
  The key never reaches the device. This is the only approach that actually protects the key.
- **Or, at minimum**, use **Firebase App Check** with an API key restricted to your app's package
  name and signing certificate, so a stolen key is unusable outside a genuine build of your app.

---

## Architecture

```
MainActivity            calculateWindowSizeClass, theme, ViewModel wiring
  └── ChatRoute         the one stateful composable: collects uiState
      └── ChatScreen    stateless; everything below takes plain values
          ├── MascotHeader → MascotAvatar
          ├── ConversationList (LazyColumn, stable keys, auto-scroll)
          └── PromptBar (text + voice + send)

ChatViewModel           combines three flows into one ChatUiState
  ├── GeminiRepository        network (interface → fake in tests)
  ├── ChatHistoryRepository   Room   (interface → fake in tests)
  └── UserPreferencesRepository DataStore (interface → fake in tests)
```

All UI state is hoisted into `ChatUiState`, exposed as a `StateFlow` and collected with
`collectAsStateWithLifecycle()`. Every screen-level composable has a `@Preview`.

Dependencies are wired by hand in `di/AppContainer.kt`, held by `GeminiApp`. One screen and four
collaborators does not justify a DI framework.

---

## Mascot

The assistant character sits above the conversation and is driven by app state, not by the view.

**State model** — `ui/mascot/MascotState.kt`: `IDLE`, `THINKING`, `HAPPY`, `EXCITED`, `CONFUSED`,
`SURPRISED`, `SLEEPY`. It is a field on `ChatUiState`, owned by `ChatViewModel`, so business logic
controls the character:

| Trigger | State |
| --- | --- |
| Prompt sent, response in flight | `THINKING` |
| Response received | `HAPPY` (2.5 s, then `IDLE`) |
| First response of a conversation, or a response over 400 characters | `EXCITED` |
| Request failed, empty prompt, or missing key | `CONFUSED` |
| Conversation cleared | `SURPRISED` |
| 60 s with no interaction | `SLEEPY` |
| Typing while asleep | back to `IDLE` |

`ChatViewModel.setMascot(state)` sets any expression directly, for features that do not go through
the chat flow.

**Implementation** — `ui/mascot/MascotAvatar.kt` is a plain Compose composable, *not* a Lottie view.
The supplied `ahumlabs_mascot_idle.json` is a single image layer wrapping a base64-embedded PNG,
with a transform-only loop (±1.5° rotation, ±3 px bob, 98–100 % scale, 2 s). There are no vector
shapes and no expression layers, so a Lottie runtime would add a dependency and a JSON parse purely
to replay a transform Compose drives natively. The PNG is used directly
(`res/drawable-nodpi/mascot_ahum.png`); the original JSON is kept in `docs/mascot/` for provenance.

Each state maps to a motion spec — loop period, bob, tilt, pulse, settled tilt/scale, glow and body
alpha. The looping values come from a single `rememberInfiniteTransition` phase; the settled values
spring on a state change, which is what makes a transition read as the character reacting rather
than the animation restarting. Every animated value is written inside `graphicsLayer`'s lambda,
so it is applied in the draw phase and the avatar never recomposes while animating.

The header collapses when the keyboard opens or the transcript is scrolled, so a long conversation
is never squeezed.

### Limitations of the supplied assets

- **One drawing, seven states.** The ZIP contains a single neutral/idle render. The character sheet
  of ten expressions described in the brief was not in the archive, so expressions are conveyed
  through motion and glow intensity, not through different artwork. Dropping per-expression PNGs
  into `res/drawable-nodpi/` and switching on `MascotState` in `MascotAvatar` is the natural next
  step once that art exists.
- **The Lottie is raster.** It is a 480×480 PNG in a JSON wrapper, so it cannot be recoloured,
  partially animated, or scaled beyond its native size without softening. A genuinely vector Lottie
  (or per-part PNGs — head, body, eyes — animated separately) would allow blinking and independent
  limb motion.
- **No transparent shadow/ground plane** is supplied, so the character floats on the background.

---

## Tests

```bash
./gradlew testDebugUnitTest          # ChatViewModel, no device needed
./gradlew connectedDebugAndroidTest  # Compose UI tests, needs the phone attached
```

Unit tests use `kotlinx-coroutines-test` with a `MainDispatcherRule` and fakes for all three
repositories (`app/src/test/.../fake/Fakes.kt`). They cover prompt validation, the success path,
history replay, failure handling, snackbar dismissal, the missing-key path, clearing history,
preference persistence, restoring an existing conversation, and mascot control.

UI tests use `createComposeRule()` against the stateless `ChatScreen`, driving it by test tag.

---

## Submission checklist

- [x] Branch name starts with the roll number — `N152_MAD_E2_ASSIGNMENT1`
- [x] No API key in any tracked file; `local.properties` is git-ignored and unstaged
- [x] `local.properties.example` committed with a placeholder
- [x] README explains key placement, the encryption flow, and how to run the tests
