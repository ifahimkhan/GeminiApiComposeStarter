package com.fahim.geminiApiComposeStarter

import android.app.Application

class GeminiChatApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
