package com.fahim.geminiApiComposeStarter

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeminiApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Encrypt the build-time key into the Keystore-backed store on first launch.
        // Failure is non-fatal: decrypt() retries provisioning when the model is created.
        appScope.launch { runCatching { container.apiKeyStore.provision() } }
    }
}
