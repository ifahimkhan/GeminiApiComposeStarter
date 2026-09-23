package com.fahim.geminiApiComposeStarter.data

import android.net.Uri


interface GeminiRepository {

    suspend fun initialize()

    suspend fun sendMessage(
        prompt: String,
        imageUri: Uri? = null
    ): String
}