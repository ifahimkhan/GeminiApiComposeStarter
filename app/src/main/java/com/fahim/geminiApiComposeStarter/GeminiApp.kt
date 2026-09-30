package com.fahim.geminiApiComposeStarter

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Register in AndroidManifest.xml:  <application android:name=".GeminiApp" ...>
 */
class GeminiApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // First launch: encrypt the build-time key with an Android Keystore key and
        // store only the ciphertext. No-op on later launches unless the key changed.
        appScope.launch {
            try {
                container.apiKeyManager.ensureEncrypted()
            } catch (e: Exception) {
                // Never log the key itself – only that encryption failed.
                Log.w("GeminiApp", "Could not encrypt API key: ${e.javaClass.simpleName}")
            }
        }
    }
}
