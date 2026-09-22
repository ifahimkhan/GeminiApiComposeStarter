# Task List - Secure the API Key

- [x] Update `app/build.gradle.kts` with CI environment fallback and release minification (`isMinifyEnabled = true`, `isShrinkResources = true`)
- [x] Create `local.properties.example` template
- [x] Implement `ApiKeyManager.kt` with Android Keystore AES-256-GCM encryption at rest
- [x] Update `MainActivity.kt` to retrieve decrypted API key securely at runtime
- [x] Update `README.md` with production security considerations (backend proxy, App Check, Keystore encryption)
- [x] Build project and verify compilation & unit tests (`5 passed`)
- [x] Create walkthrough artifact
