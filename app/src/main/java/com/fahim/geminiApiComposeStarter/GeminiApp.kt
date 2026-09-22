package com.fahim.geminiApiComposeStarter

import android.app.Application
import com.fahim.geminiApiComposeStarter.di.AppContainer

class GeminiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
