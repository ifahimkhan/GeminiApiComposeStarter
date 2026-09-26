# Local verification

Date: 25 September 2026. Branch: `C016-assignment-1-chatbot`. This record accompanies the local implementation; no GitHub push or PR has been made.

## Environment

Android Studio bundled JDK 21, Gradle 9.3.1, AGP 9.0.0, Kotlin Compose compiler 2.2.10, KSP 2.3.12, Compose BOM 2025.09.01 and Room 2.8.4. Emulator: Medium Phone API 36.1, Android 16, arm64, 1080 by 2400 pixels. A user-supplied API key is configured only in ignored local.properties for local testing.

## Automated checks

The latest unit run passed 12 ViewModel/context tests. The complete direct emulator run passed 15 instrumented tests, including Compose screens, Room persistence, preferences and Keystore behavior. Debug lint reported no errors. Debug and R8-minified unsigned release APKs built successfully. Three focused follow-up tests also passed, covering keyboard visibility, loading visibility for a long prompt and disabled Send during loading. Across the full and focused runs, 16 unique instrumented tests passed. The final device review is recorded below.

- Blank input and repeated Send taps do not issue duplicate requests.
- Retry and regeneration keep one user turn and one associated answer.
- Follow-up context includes completed turns, excludes unrelated conversations and stays within a defined budget.
- Switching conversations during a pending request preserves the new draft and stores the answer in its original conversation.
- Voice cancellation/unavailability do not submit a prompt; recognized text requires explicit Send.
- Room reopen preserves order and content; unfinished requests become interrupted.
- DataStore preferences survive repository recreation.
- Credential tests verify ciphertext storage, decryption, new IV on rotation, tamper recovery and controlled missing-key handling.
- Compose tests verify Send, loading, Retry, restored-history selection and voice review.

Reports: `app/build/reports/tests/testDebugUnitTest/index.html` and `app/build/reports/lint-results-debug.html`. The final direct instrumented run is preserved in `evidence/instrumented-test-results.txt`; Gradle-managed emulator reports under `app/build/reports/androidTests/` reflect earlier runs. Build outputs and reports are ignored by Git.

A first instrumented run found a LazyColumn first-layout crash with two overlapping scroll effects. The effects were consolidated and the Compose BOM aligned; the subsequent full instrumented run passed. This is a fixed defect, not an ignored failure.

## Screenshot evidence

`evidence/screenshots/home-reference.png`, `voice-reference.png` and `chat-reference.png` are deterministic Compose test renders using the video's sample content. They demonstrate layout, not a live Gemini response. The original supplied video frames remain in the workspace's `docs/reference` folder.

The UI was visually inspected for content hierarchy, clipped controls, readable message text, card arrangement, dock placement and particle artwork. Reconstructed artwork and font/icon substitutions remain visible differences. These images are not a claim of pixel identity.

## Device and performance review

The R8-minified APK was signed with the local debug certificate solely for smoke testing, installed and launched successfully. This is not a production signing configuration. Home and Voice rendered without a startup crash; a real Gemini request still requires a key.

WindowSizeClass adaptation was inspected using emulator display overrides: 1600 by 2560 at 240 dpi for an expanded tablet window and 2400 by 1080 at 420 dpi for a compact-height landscape window. The history side panel appears in expanded width. Compact-height Home scrolls beneath its persistent navigation dock. Both overrides were restored to the emulator's original 1080 by 2400 at 420 dpi. These are window-size checks, not a substitute for testing tablet hardware.

The voice orb uses cached trigonometry and reused native point buffers, reducing particles from 6400 to 4096 and removing per-particle Offset allocations. Two exploratory samples on this emulator were:

| Sample | Frames | Reported janky frames | Median frame time |
| --- | --- | --- | --- |
| Before drawing optimization | 436 | 324 or 74.31 percent | 73 ms |
| After drawing optimization | 970 | 416 or 42.89 percent | 46 ms |

Raw observations are in `release-frame-stats.txt` and `release-frame-stats-optimized.txt`. Sample lengths and concurrent tooling load differ, so these are diagnostic observations, not a controlled benchmark. Animation performance remains below the proposed 60 fps target on this emulator. Reduced motion is available in Settings. Physical-device profiling and Layout Inspector review remain required before claiming the performance target is met.

The original keyboard screenshot revealed that the emulator was suppressing its software keyboard while a hardware keyboard was attached. The focused follow-up test explicitly checks IME visibility, and separately checks that a very long pending prompt does not push the progress indicator out of view. All three focused checks passed; results are in `keyboard-and-scroll-test-results.txt`. The long-message test exposed an offscreen progress indicator, which is now pinned above the composer. The emulator shows a floating IME toolbar, so this does not establish full docked-keyboard behavior on a physical phone. The original hardware-keyboard preference was restored after testing.

## Secrets

The focused scanner found zero API-key patterns in source/history, zero tracked `local.properties` files and no history entries for that file. `git check-ignore local.properties` confirms it is ignored. The scanner does not print matched secret values and is not an exhaustive guarantee for every secret format.

## Remaining checks before submission

- Live invalid-key/quota/provider failure coverage beyond the successful local credential smoke test.
- Real microphone transcription on a physical speech-enabled device; callback handling is automated, but that does not prove recognition quality.
- Android API 26 device compatibility beyond compilation/minSdk configuration.
- Android Studio Layout Inspector recomposition-count review and physical-device performance measurement.
- Full TalkBack and large-font accessibility review.
- Exact original avatar/font/vector assets and a final designer-approved visual comparison if literal pixel fidelity is required.

The assignment's optional dynamic colour/system light theme and pre-commit scanner hook are not enabled. A manual secret scanner is included. The default remains the reference's dark palette. Reference iPhone hardware/system bars and the Android speech-provider activity are excluded from app-owned visual matching.

## Live credential smoke test

The user-supplied credential returned HTTP 200 from Gemini 3.6 Flash with the requested text, "Local chatbot test successful". A temporary on-device instrumented probe through ApiKeyVault and the existing GenerativeModel SDK also passed (one test, 5.46 seconds); the probe was removed to avoid automatic billable requests in the regular suite. The rebuilt debug app persisted live replies, including "Hello! How can I help you today?", visible in conversation history. Some interactive attempts showed the generic error, so this smoke test does not establish reliability or quota behavior. No credential value is included in this record. At the time of that test, release APKs had not been rebuilt with the credential. The subsequent second UI pass rebuilds both variants with the local configuration; keep those APKs private.

The second UI pass is documented separately in [PASS_2.md](PASS_2.md), including updated model-selector tests and animation evidence.
