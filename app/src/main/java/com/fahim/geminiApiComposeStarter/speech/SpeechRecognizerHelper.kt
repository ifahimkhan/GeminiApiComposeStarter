package com.fahim.geminiApiComposeStarter.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import java.util.Locale

/**
 * Utility helper encapsulating RecognizerIntent construction, intent result parsing,
 * and device speech availability checks to keep the UI clean and decoupled from platform details.
 */
object SpeechRecognizerHelper {

    /**
     * Creates an Intent to launch Android's speech recognition dialog.
     */
    fun createSpeechIntent(prompt: String? = null): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            if (!prompt.isNullOrBlank()) {
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
        }
    }

    /**
     * Extracts the primary recognized speech text from the activity result Intent.
     * Returns null if canceled, empty, or failed.
     */
    fun extractSpeechResult(resultCode: Int, data: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null
        }
        val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return results?.firstOrNull { it.isNotBlank() }?.trim()
    }

    /**
     * Checks whether the device has an activity capable of handling ACTION_RECOGNIZE_SPEECH.
     */
    fun isSpeechRecognitionAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return activities.isNotEmpty()
    }
}
