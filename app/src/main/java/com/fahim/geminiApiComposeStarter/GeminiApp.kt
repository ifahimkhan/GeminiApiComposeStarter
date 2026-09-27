package com.fahim.geminiApiComposeStarter

import android.app.Application
import com.fahim.geminiApiComposeStarter.di.AppContainer
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
        // First launch: encrypt the API key with the Keystore and persist only the ciphertext.
        appScope.launch {
            runCatching { container.secureApiKeyStore.ensureEncrypted() }
        }
    }
}
