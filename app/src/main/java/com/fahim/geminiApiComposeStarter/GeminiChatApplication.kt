package com.fahim.geminiApiComposeStarter

import android.app.Application
import com.fahim.geminiApiComposeStarter.data.db.AppDatabase

/** Application-scoped singletons: the Room database lives as long as the process. */
class GeminiChatApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
