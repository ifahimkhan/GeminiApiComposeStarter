# C033 Assignment 1 — Start-to-Pull-Request Guide (Android Studio Flamingo)

## A. Get the starter repository

### Recommended when you do NOT have write access to the instructor repository
1. Open `https://github.com/ifahimkhan/GeminiApiComposeStarter` in GitHub.
2. Click **Fork** and create the fork under your own GitHub account.
3. In Windows Terminal / Git Bash:

```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/GeminiApiComposeStarter.git
cd GeminiApiComposeStarter
git remote add upstream https://github.com/ifahimkhan/GeminiApiComposeStarter.git
git fetch upstream
```

### If your faculty has explicitly given you push access
Clone the instructor repository directly instead and skip the fork/upstream step.

## B. Create the compulsory roll-number branch

```bash
git checkout -b C033-assignment1-gemini
```

Confirm:

```bash
git branch --show-current
```

The output must start with `C033`.

## C. Apply the supplied code overlay

1. Extract `Assignment1_Flamingo_Overlay_C033.zip`.
2. Copy **the contents inside the extracted folder** into the cloned `GeminiApiComposeStarter` repository root.
3. Choose **Replace files** when Windows asks.
4. Do not delete the repository's `.git` folder.
5. Keep the repository's existing `gradlew` and `gradlew.bat` files.

## D. Open in Android Studio Flamingo

1. Start **Android Studio Flamingo | 2022.2.1**.
2. **File > Open** and choose the cloned `GeminiApiComposeStarter` folder.
3. Trust the project if prompted.
4. In **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**, set **Gradle JDK = Embedded JDK / jbr-17**.
5. In **Tools > SDK Manager**, ensure **Android 13 / API 33** SDK Platform is installed.
6. Click **Sync Project with Gradle Files**.

If `org.gradle.wrapper.GradleWrapperMain` is missing, follow the wrapper-JAR recovery steps in `README.md`.

## E. Add the Gemini API key safely

1. Obtain a key from Google AI Studio.
2. Open the root `local.properties` file. Android Studio may already have created it with `sdk.dir=...`.
3. Add a NEW line without deleting `sdk.dir`:

```properties
GEMINI_API_KEY=PASTE_YOUR_REAL_KEY_HERE
```

4. Never put the real key in Kotlin, XML, README, Gradle files, screenshots, commit messages or the PR description.
5. `local.properties` is ignored by Git; `local.properties.example` contains only a placeholder.

## F. Run the app

1. **Tools > Device Manager**.
2. Create a Pixel emulator using API 33, or connect a physical Android device.
3. Run the `app` configuration.
4. Verify:
   - text prompt -> Gemini response;
   - user/Gemini Material 3 bubbles;
   - loading indicator;
   - microphone -> speech-to-text;
   - Preferences dialog saves a display name;
   - chat history remains after app restart;
   - Clear History works;
   - dark mode works;
   - rotate the emulator or use a tablet emulator to verify responsive width.

## G. Run tests

From Android Studio you can right-click the two test classes and run them.

If the Gradle wrapper JAR exists, Windows terminal commands are:

```bat
gradlew.bat testDebugUnitTest
gradlew.bat connectedDebugAndroidTest
```

`connectedDebugAndroidTest` needs an emulator/device running.

## H. Check that no secret will be committed

```bash
git status
git check-ignore local.properties
git diff
git grep -n "PASTE_A_UNIQUE_10_CHAR_FRAGMENT_FROM_YOUR_REAL_KEY"
```

Expected:
- `local.properties` is NOT listed under files to be committed.
- `git check-ignore local.properties` prints `local.properties`.
- replace the placeholder in the grep command with a unique 10-character fragment from your actual key; the command should return no matches.

After staging, check once more:

```bash
git add .
git status
git diff --cached
```

After committing, you can also verify the new commit with:

```bash
git show --stat --oneline HEAD
git show HEAD
```

Again, search the output for a unique fragment of the real key.

If `local.properties` somehow appears staged, STOP and run:

```bash
git restore --staged local.properties
```

## I. Commit

A clean two-commit history is easy for the faculty to review:

```bash
git add .
git commit -m "C033: add secure Gemini chat persistence and responsive UI"
```

After screenshots / README updates (if any):

```bash
git add .
git commit -m "C033: add tests and assignment documentation"
```

One good commit is also acceptable if you prefer.

## J. Push the branch

If working from your fork:

```bash
git push -u origin C033-assignment1-gemini
```

## K. Create the pull request

1. Open your fork on GitHub after pushing.
2. Click **Compare & pull request**.
3. Base repository: `ifahimkhan/GeminiApiComposeStarter`.
4. Base branch: `master` (unless faculty tells you otherwise).
5. Compare branch: your `C033-assignment1-gemini` branch.
6. Suggested title:

`C033 - MAD Assignment 1 - Gemini Compose Enhancement`

7. Suggested PR description:

```text
## C033 - Lab Assignment 1

### Implemented
- Material 3 LazyColumn chat bubbles with stable keys and auto-scroll
- StateFlow/ChatUiState state hoisting and lifecycle-aware collection
- WindowSizeClass responsive layout
- Loading indicator and Snackbar error handling
- Speech-to-text using RecognizerIntent
- Preferences DataStore personalization
- Room-persisted chat history
- AES-256-GCM key-at-rest protection using Android Keystore
- local.properties + CI environment fallback for GEMINI_API_KEY
- Release R8 minification
- ChatViewModel unit tests and Compose UI test

### Security
- No real Gemini API key is included in Git history or this PR.
- local.properties remains git-ignored.

### Testing
- [ ] App run on API 33 emulator/device
- [ ] Text Gemini request tested
- [ ] Voice input tested
- [ ] Persistence after restart tested
- [ ] Unit tests passed
- [ ] Compose UI test passed

### Screenshots
(Add your screenshots here.)
```

8. Add the required screenshots to the PR description by drag-and-drop.
9. Re-check the **Files changed** tab. Search visually for your API key and verify `local.properties` is absent.
10. Click **Create pull request**.

## L. Do not merge your own PR

Leave it open for the faculty/repository owner unless your faculty explicitly tells you to merge it yourself.
