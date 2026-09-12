# Gemini

## API key setup

Do not commit a real Gemini API key.

For local builds, copy `local.properties.example` to `local.properties` and set:

```properties
GEMINI_API_KEY=replace-with-your-gemini-api-key
```

`local.properties` is ignored by Git. Before committing, run `git status` and confirm `local.properties` is not staged.

For CI builds, set a repository secret or environment variable named `GEMINI_API_KEY`. The Gradle build uses `local.properties` first, then falls back to `System.getenv("GEMINI_API_KEY")`.

## Security limits

This sample stores the key encrypted at rest with an AES-256-GCM key generated in Android Keystore. The decrypted value is only used in memory when creating the Gemini client, and it must never be logged, toasted, or displayed.

Client-side encryption only raises the bar. A determined attacker can still extract secrets from an app running on their device. A production app should move Gemini calls behind a backend proxy, or combine restricted API keys with protections such as Firebase App Check.
