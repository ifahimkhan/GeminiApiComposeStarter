# N103 validation results

## Verified locally

- Debug APK compilation.
- Release APK compilation with R8 minification (unsigned release APK).
- Android lint: no errors. Warnings include newer dependency/SDK versions and unused starter resources.
- Five ViewModel unit tests passed.
- Eight offline instrumented tests passed on an Android 17 / API 37.2 ARM64 emulator: four Compose UI tests, three persistence/security tests and the starter app-context test.
- Gemini API key and `gemini-3.6-flash` endpoint: a small HTTPS request from the Mac returned HTTP 200 with a response candidate.
- Secret scan: the configured key was absent from Git history and tracked/submittable files; `local.properties` is ignored.

## Live emulator limitation

The opt-in live chat test did not receive a successful Gemini response. A direct HTTPS POST from the emulator also timed out, independently of the Gemini SDK; a basic HTTPS connection to the Google endpoint succeeded. The same generation request from the Mac succeeded. This isolates the observed failure to generation requests made from the emulator environment, but does not establish its exact network cause.

The app now sets an SDK request timeout of 30 seconds and an outer coroutine timeout of 45 seconds. When a request fails, the progress state ends, the draft is restored (unless a new draft was entered), and a safe error is shown. No raw exception or key is displayed.

Recheck live replies on your Android device or your usual emulator/network before submission. Do not claim that a successful end-to-end live reply was verified here. `LiveChatTest` is opt-in; the default connected suite skips it and requires no live API call.

## Manual checks still needed

- Real microphone recognition (the UI callback is tested; actual speech depends on the installed recognizer).
- Actual phone/tablet rotation and dark-mode appearance (the expanded layout is covered by a Compose test).
- Layout Inspector recomposition counts and real-device latency.
- Screenshots of successful live replies once connectivity is working.

## Commands

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug connectedDebugAndroidTest
```

For the optional live check, which consumes API quota:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveGemini=true
```

Run instructions are in `../START_HERE.md`. No GitHub submission has been made.
