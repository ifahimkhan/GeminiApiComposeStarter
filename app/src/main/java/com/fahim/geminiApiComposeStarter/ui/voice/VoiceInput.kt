package com.fahim.geminiApiComposeStarter.ui.voice

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Remembers a launcher that opens the system speech-recognition UI and returns the recognized
 * text via [onResult]. If no recognizer is available on the device (some emulators, some devices
 * without Google apps) [onUnavailable] is called instead of crashing.
 */
@Composable
fun rememberVoiceInputLauncher(
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit,
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spokenText != null) {
            onResult(spokenText)
        }
        // A null/cancelled result (user dismissed the dialog, no speech detected) is not an
        // error — we simply leave the text field untouched.
    }

    return remember(context) {
        {
            val intent = createSpeechRecognizerIntent()
            try {
                launcher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                onUnavailable()
            }
        }
    }
}

private fun createSpeechRecognizerIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
    }

/** True if a speech-recognition activity is registered on the device. */
fun isSpeechRecognitionAvailable(context: Context): Boolean =
    createSpeechRecognizerIntent().resolveActivity(context.packageManager) != null
