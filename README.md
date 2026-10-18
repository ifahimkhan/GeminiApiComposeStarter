# Gemini Jetpack Compose Starter — Assignment 1

**Student Roll Number:** N123  
**Course:** Mobile Application Development  
**Institution:** SVKM's NMIMS University, School of Technology Management & Engineering, Mumbai  

---

## 1. Setup & API Key Configuration

The project securely reads the Google Gemini API key during build time and never checks secrets into Version Control Systems (VCS).

### Configuration Steps

1. Copy the example configuration template:
   ```bash
   cp local.properties.example local.properties
   ```
2. Open `local.properties` in the project root and provide your Gemini API key:
   ```properties
   GEMINI_API_KEY=YOUR_ACTUAL_GEMINI_API_KEY
   ```
3. `local.properties` is registered in `.gitignore` and must **never** be committed.

### CI/CD Environment Fallback

In automated CI environments where `local.properties` is absent, `app/build.gradle.kts` falls back to the `GEMINI_API_KEY` system environment variable:
```kotlin
val geminiApiKey: String = (localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY"))
    ?.trim().orEmpty()
```

---

## 2. API Key Security & Encryption Architecture

To satisfy strict security guidelines, the application implements defense-in-depth API key management:

```
+-----------------------------------+
|  BuildConfig.GEMINI_API_KEY       | (Obtained at compile-time)
+-----------------+-----------------+
                  | (Passed during first launch initialization)
                  v
+-----------------------------------+
|  ApiKeyManager                    |
+-----------------+-----------------+
                  |
        +---------+---------+
        |                   |
        v                   v
+------------------+  +--------------------------------+
| KeyStoreCrypto   |  | ApiKeyStorage                  |
| (AES-256-GCM)    |  | (EncryptedSharedPreferences)   |
| Android Keystore |  | Persists only:                 |
| KeyGenParamSpec  |  |  - Ciphertext (Base64)         |
| Hardware Backed  |  |  - GCM Nonce/IV (Base64)       |
+------------------+  +--------------------------------+
        |                   |
        +---------+---------+
                  |
                  | (Decryption on-demand)
                  v
+-----------------------------------+
| GeminiRepositoryImpl (in-memory)  |
| GenerativeModel created lazily    |
+-----------------------------------+
```

### Key Security Features

1. **Hardware-Backed Keystore Generation (`KeyStoreCryptoEngine`)**:
   - Uses `KeyGenParameterSpec` to generate an AES-256 key inside `AndroidKeyStore`.
   - The key is configured with `KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, and `ENCRYPTION_PADDING_NONE`.
   - Private key material remains strictly inside the secure hardware environment (TEE/SE) and cannot be exported.

2. **Encryption at Rest**:
   - The API key is encrypted using AES-256-GCM with a fresh random 12-byte initialization vector (IV/nonce).
   - Only the ciphertext and IV are persisted; the plaintext key is never written to disk.
   - Persistence is managed by `EncryptedSharedPreferences`, utilizing an Android Keystore `MasterKey` for additional defense-in-depth.

3. **Strict In-Memory Lifecycle**:
   - `GeminiRepositoryImpl` initializes `GenerativeModel` lazily via an `apiKeyProvider` lambda.
   - Decryption occurs in-memory strictly when the Gemini client requires it to make a network call.
   - The decrypted key is never logged, toasted, displayed in UI, or included in exception messages.

4. **Code Obfuscation & Shrinking (R8)**:
   - Release builds have `isMinifyEnabled = true` to obfuscate symbol names, strip unused metadata, and deter static analysis.
   - Targeted ProGuard rules are configured in `proguard-rules.pro` to suppress benign compile-time annotation warnings from Google Tink (`com.google.errorprone.annotations.**`).

---

## 3. Limitations of Client-Side Key Protection

While hardware-backed keystore encryption, encrypted preferences, and R8 minification significantly elevate the security bar against casual inspection, **no client-side encryption can completely protect a secret from a determined attacker on an untrusted device**.

### Vulnerabilities on Compromised / Rooted Devices:
- **Memory Inspection & Dynamic Hooking**: Tools like Frida or Xposed can hook Java/Kotlin methods (`Cipher.doFinal`, `GenerativeModel.<init>`) to intercept decrypted keys directly from memory.
- **Root Keystore Extraction**: On rooted devices or emulators, an attacker with root privileges can access application private directories and attach debuggers (gdb/lldb/jdwp).
- **Network Traffic Interception**: With custom CA certificates installed on a rooted device, TLS interception (MITM) can inspect HTTPS traffic containing API keys unless strict certificate pinning and App Attestation are applied.

### Recommended Production Architecture:
1. **Backend-For-Frontend (BFF) / Backend Proxy**:
   - Rather than embedding the Gemini API key in mobile clients, the Android app authenticates users (e.g., Firebase Authentication, OAuth2/OIDC) and forwards prompts to a secure, private backend service.
   - The backend holds the Gemini API key securely in a secret manager (e.g., Google Cloud Secret Manager) and forwards requests to Google AI Studio or Vertex AI.
2. **Firebase App Check**:
   - Enforce device integrity using Play Integrity API via Firebase App Check to ensure only unmodified, authentic instances of the app can call backend endpoints.
3. **API Key Restrictions & Quotas**:
   - Restrict API keys in Google Cloud Console to specific Android application package names and SHA-256 certificate fingerprints.
   - Configure billing alerts and rate-limiting quotas to mitigate abuse.

---

## 4. Building and Testing

### Run Unit Tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK (with R8 Minification)
```bash
./gradlew assembleRelease
```
