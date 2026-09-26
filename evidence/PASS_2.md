# Second UI and animation pass

25 September 2026 · local branch `C016-assignment-1-chatbot`.

## Delivered

- Chat-header model pill and a scrollable, dark blue model sheet with distinct cards, selected radio semantics, preview labeling and an assistant-category link. Also available in Settings.
- Model choice is saved in Preferences DataStore. The next send/retry/regeneration uses it, while an already pending request retains its captured model. Draft and conversation remain intact.
- Dark launch background on Android 12+ to remove the white startup flash.
- Short route fades with small vertical movement; message entry/movement; transcript transitions; spring press feedback for cards and microphone; animated navigation tint and loading visibility.
- Denser blue-to-purple particle geometry with a seamless nine-second cycle and a decorative waveform. Geometry is calculated off the UI thread and cached (~11.8 MB); the rendering loop draws 16 color groups without recalculating particle positions.
- Reduce Motion suppresses the app-owned ambient and transition effects and uses a static progress indicator. Native sheet animation follows platform settings.

The reference is the supplied f6c video. The model sheet is an extension designed to fit its palette and controls, not a screen that appears in that video. Original avatar, fonts and vector assets are still unavailable; pixel identity is not claimed.

## Verification

- 15 unit tests passed, including model snapshot isolation, model switching before a queued request executes, retry routing, draft preservation and unknown-model fallback.
- 14 instrumented UI/persistence tests passed (`pass2-instrumented-tests.txt`). Model selection, next-message explanation, long-message loading and persistence across repository recreation are covered.
- Two focused screenshot tests passed after the final particle refinement (`pass2-final-capture-tests.txt`). The picker uses a full-window screenshot because it is a separate dialog window.
- Debug and R8 release builds succeeded; debug lint reported no errors.
- Secret scanner and `git diff --check` passed. Credential stays in ignored local properties; local APKs include the bootstrap credential and remain private test artifacts.
- Model IDs were checked against the authenticated Google models endpoint. The prior default-model live smoke test remains recorded in `VERIFICATION.md`; this pass did not send billable test prompts to every model or prove their quota/access.

## Visual evidence

- `screenshots/model-picker.png`: reviewed picker with its selected card and all four choices.
- `screenshots/chat-reference.png`: updated header pill and reference-content layout.
- `screenshots/voice-reference.png`: denser, static reference render with transcript.
- `pass2-launch.png`: final cold-launch capture after the dark splash theme fix.
- `pass2-release-voice.png`: actual release build with ambient motion enabled.
- `pass2-voice-motion.mp4`: ten-second release-mode emulator recording (screen capture itself adds load).

The reference screenshots contain deterministic fixture text, not generated live answers.

## Performance

The 15-second release-mode voice sample is recorded in `pass2-release-frame-stats.txt`: 868 rendered frames, 39 deadline-based janky frames (4.49%), median 22 ms and 90th percentile 36 ms. Android also reported a legacy jank value of 96.54%; both metrics are preserved in the raw output. These timings do not establish a sustained 60 fps target. A first capture that moved to the chat screen was discarded; this sample and the recording were checked against the Voice screen. It is an emulator diagnostic, not a controlled comparison with the earlier samples or a guarantee for physical devices. Layout Inspector, physical-device profiling, TalkBack and large-font review remain pending before submission.
