# N103 submission steps

1. Complete the manual checks listed in VALIDATION.md. Capture a successful live reply, voice input, dark mode, and phone/tablet layouts as available. Never include your API key or local.properties in screenshots.
2. Fork https://github.com/ifahimkhan/GeminiApiComposeStarter in your GitHub account.
3. This project already has the upstream history and the `N103-assignment-1` branch. Add your fork as a separate remote, replacing YOUR_GITHUB_USERNAME:

   ```bash
   git remote add fork https://github.com/YOUR_GITHUB_USERNAME/GeminiApiComposeStarter.git
   ```

4. Review the changes and secret scan before committing. `local.properties` must remain ignored. Never use force-push to the instructor's repository.
5. Push the assignment branch:

   ```bash
   git push -u fork N103-assignment-1
   ```

6. In GitHub choose **Compare & pull request**. Base repository: `ifahimkhan/GeminiApiComposeStarter`, base branch: `master`. Head repository: your fork, compare branch: `N103-assignment-1`.
7. Use title **N103 Assignment 1 Gemini chat enhancements** and copy the verified features/results from PR_DESCRIPTION.md. Attach screenshots or link the committed screenshots.
8. Submit the resulting PR URL through the instructor's requested submission channel.

If changes have not yet been committed, select only the assignment source, tests, documentation and build configuration in Android Studio's Commit view, review the diff for secrets, and commit before step 5.
