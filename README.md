# Gemini API Compose Starter

A fully functional, modern Android chatbot application built with **Jetpack Compose** and the **Google Generative AI SDK**. This app demonstrates how to integrate Gemini into an Android app with a rich user interface, local persistence, and security best practices for API keys.

## Features
* 💬 **Interactive Chat Interface**: Built with Material 3, featuring adaptive layouts and Markdown support.
* 🎙️ **Voice Input**: Integrated speech-to-text allowing users to dictate their prompts.
* 💾 **Local Chat History**: Uses Room Database to persist multiple conversation sessions locally.
* 🌓 **Dark Mode**: Toggleable dark/light theme using DataStore preferences.
* 🔒 **Encrypted Key Storage**: Uses Android KeyStore and AES-256-GCM to encrypt the API key when saving it locally.

## Setup Instructions
1. Open the project in Android Studio.
2. Open the `local.properties` file in the root directory.
3. Add your Gemini API Key:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
4. Sync the project with Gradle and run the app.

---

## ⚠️ Security: Know the Limits

This project includes a `KeySecurityManager` that encrypts the API key before persisting it to disk, protecting it from basic local extraction. However, **client-side encryption raises the bar but cannot fully hide a key from a determined attacker.** If an API key is used directly in a client application, it can theoretically be reverse-engineered or intercepted during runtime.

### Production Recommendations
For a production application, you should **not** embed or distribute the raw Gemini API key within the client app. Instead, implement one of the following architectures:

1. **Backend Proxy (Recommended)**
   Move the Gemini API calls behind a secure backend server that you control. 
   - Your Android app authenticates with your backend.
   - Your backend securely holds the API key and makes requests to the Gemini API.
   - This ensures the key never touches the user's device.

2. **Firebase App Check & Restricted API Keys**
   If you must call the Gemini API directly from the client (for example, using the Vertex AI in Firebase SDK), you must secure your setup:
   - **Restrict API Keys:** In the Google Cloud Console, restrict your API keys to only allow requests from your specific Android app's package name and SHA-1 certificate.
   - **Firebase App Check:** Implement Firebase App Check (using Play Integrity on Android). This attestation mechanism ensures that only genuine, untampered instances of your app running on legitimate devices can make requests to your API.