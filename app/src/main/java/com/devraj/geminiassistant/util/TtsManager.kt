package com.devraj.geminiassistant.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                isInitialized = true
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                onSpeechDoneCallback?.invoke()
            }

            override fun onError(utteranceId: String?) {
                onSpeechDoneCallback?.invoke()
            }
        })
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!isInitialized) return
        stop()
        onSpeechDoneCallback = onDone
        val utteranceId = System.currentTimeMillis().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
        onSpeechDoneCallback?.invoke()
        onSpeechDoneCallback = null
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
