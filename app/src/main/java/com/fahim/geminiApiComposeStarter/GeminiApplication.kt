package com.fahim.geminiApiComposeStarter

import android.app.Application

class GeminiApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
