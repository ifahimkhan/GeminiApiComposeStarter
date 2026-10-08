package com.n149.geminichat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class – entry point for Hilt dependency injection.
 * Hilt generates the component graph here; no manual DI setup required.
 */
@HiltAndroidApp
class GeminiChatApplication : Application()
