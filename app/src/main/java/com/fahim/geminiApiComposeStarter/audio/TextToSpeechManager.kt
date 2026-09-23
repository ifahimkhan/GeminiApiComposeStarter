package com.fahim.geminiApiComposeStarter.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Lifecycle-safe manager for Android Text-To-Speech (TTS).
 * Provides speech playback for Gemini educational responses.
 */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _speakingMessageId.value = null
            }

            @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _speakingMessageId.value = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                _speakingMessageId.value = null
                Log.w(TAG, "TTS playback error code: $errorCode")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS Language not supported or missing data")
            } else {
                isInitialized = true
            }
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    fun speak(messageId: String, text: String) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not ready")
            return
        }

        // If currently speaking this message, toggle stop
        if (_speakingMessageId.value == messageId && _isSpeaking.value) {
            stop()
            return
        }

        stop()
        _speakingMessageId.value = messageId
        val cleanText = text
            .replace(Regex("[#*`_~]"), "") // Strip markdown symbols for natural speech
            .trim()

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
