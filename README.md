# Gemini API Compose Starter

A modern Android Jetpack Compose application demonstrating integration with the Google Gemini API (`gemini-3.6-flash`).

## API Key Setup

1. Copy the example properties file or create a `local.properties` file in the root project directory:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
2. Alternatively, provide the key as an environment variable in your CI or build environment:
   ```bash
   export GEMINI_API_KEY="your_api_key_here"
   ```
3. The build configuration in `app/build.gradle.kts` extracts the key at build time and makes it accessible via `BuildConfig.GEMINI_API_KEY`.
   - `local.properties` is listed in `.gitignore` and is never committed to version control.
   - Never commit a real API key to Git.

## Key Encryption Architecture and Flow

To guard the API key at rest on the client device:
1. **Android KeyStore AES-256-GCM (`SecureKeyStore`)**:
   - Master AES-256 keys are created and stored inside the hardware-backed Android KeyStore.
   - Encryption uses `AES/GCM/NoPadding` with an initialization vector (IV) generated uniquely for each encryption operation.
2. **Persistence in DataStore (`EncryptedKeyDataStore`)**:
   - Plaintext keys are never stored on disk.
   - Only the random IV and ciphertext (encoded in Base64) are persisted to Jetpack DataStore Preferences.
3. **Lazy In-Memory Decryption**:
   - Keys are decrypted in memory strictly when creating the `GenerativeModel` during runtime requests.
   - Plaintext keys are never logged, displayed in UI, toasted, or cached unencrypted.

## Production Limitations and Best Practices

While Android KeyStore and AES-256-GCM protect data at rest, on-device key management has architectural limits:
- **Client-Side Extraction Risk**: Compromised or rooted devices, memory dump tools, or advanced reverse-engineering techniques can intercept keys held in process memory.
- **Production Architecture Recommendations**:
  1. **Backend Proxy / BFF (Backend-for-Frontend)**:
     - For production releases, client apps should never store or invoke Gemini API keys directly.
     - Direct client requests through a secure server proxy that authenticates the user (e.g., via OAuth/Firebase Auth) and forwards requests to Gemini server-to-server.
  2. **Firebase App Check & API Key Restrictions**:
     - When direct client calls are required, pair them with **Firebase App Check** (using Play Integrity) to ensure calls originate only from genuine, unmodified instances of your app.
     - Enforce Google Cloud Console API key restrictions: restrict keys strictly to your Android app's package name and SHA-256 signing certificate fingerprint, and apply strict quota rate limits.
