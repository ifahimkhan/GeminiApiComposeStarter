package com.fahim.geminiApiComposeStarter

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Register in AndroidManifest.xml:  <application android:name=".ChatApplication" ...>
 */
class ChatApplication : Application() {

    lateinit var container: ServiceLocator
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = ServiceLocator(this)

        // First launch: encrypt the build-time key with an Android Keystore key and
        // store only the ciphertext. No-op on later launches unless the key changed.
        appScope.launch {
            try {
                container.secureKeyStore.ensureEncrypted()
            } catch (e: Exception) {
                // Never log the key itself – only that encryption failed.
                Log.w("ChatApplication", "Could not encrypt API key: ${e.javaClass.simpleName}")
            }
        }
    }
}
