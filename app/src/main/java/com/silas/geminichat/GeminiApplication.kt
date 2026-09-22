package com.silas.geminichat

import android.app.Application
import com.silas.geminichat.di.AppContainer

class GeminiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
