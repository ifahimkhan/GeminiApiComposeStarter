package com.fahim.geminiApiComposeStarter.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [TextSpeaker] backed by Android's TextToSpeech engine. */
class AndroidTextSpeaker(context: Context) : TextSpeaker, TextToSpeech.OnInitListener {

    private enum class EngineState { STARTING, READY, FAILED }

    private val _speakingId = MutableStateFlow<Long?>(null)
    override val speakingId: StateFlow<Long?> = _speakingId.asStateFlow()

    private val lock = Any()
    private var engineState = EngineState.STARTING
    private var pending: Pair<Long, String>? = null
    private var lastChunkIndex = 0

    private val tts = TextToSpeech(context.applicationContext, this)

    // The engine starts asynchronously, so a request made before it is ready is held here and played on init.
    override fun onInit(status: Int) = synchronized(lock) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceProgressListener(progressListener)
            engineState = EngineState.READY
            pending?.let { (id, text) -> start(id, text) }
        } else {
            engineState = EngineState.FAILED
            _speakingId.value = null
        }
        pending = null
    }

    override fun speak(id: Long, text: String): Boolean = synchronized(lock) {
        when (engineState) {
            EngineState.FAILED -> false
            EngineState.STARTING -> {
                pending = id to text
                _speakingId.value = id
                true
            }
            EngineState.READY -> {
                start(id, text)
                true
            }
        }
    }

    override fun stop() = synchronized(lock) {
        pending = null
        if (engineState == EngineState.READY) tts.stop()
        _speakingId.value = null
    }

    override fun shutdown() = synchronized(lock) {
        pending = null
        tts.stop()
        tts.shutdown()
        engineState = EngineState.FAILED
        _speakingId.value = null
    }

    private fun start(id: Long, text: String) {
        val chunks = chunkForSpeech(text, TextToSpeech.getMaxSpeechInputLength())
        if (chunks.isEmpty()) {
            _speakingId.value = null
            return
        }
        // Set before speaking so the "stopped" callbacks of the message we are replacing are ignored.
        _speakingId.value = id
        lastChunkIndex = chunks.lastIndex
        chunks.forEachIndexed { index, chunk ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (tts.speak(chunk, mode, null, "$id:$index") == TextToSpeech.ERROR) {
                _speakingId.value = null
                return
            }
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finish(utteranceId, onlyAfterLastChunk = true)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = finish(utteranceId, onlyAfterLastChunk = false)

        override fun onStop(utteranceId: String?, interrupted: Boolean) =
            finish(utteranceId, onlyAfterLastChunk = false)
    }

    private fun finish(utteranceId: String?, onlyAfterLastChunk: Boolean) = synchronized(lock) {
        val parts = utteranceId?.split(":") ?: return@synchronized
        val id = parts.getOrNull(0)?.toLongOrNull() ?: return@synchronized
        val index = parts.getOrNull(1)?.toIntOrNull() ?: return@synchronized
        if (_speakingId.value != id) return@synchronized
        if (onlyAfterLastChunk && index < lastChunkIndex) return@synchronized
        _speakingId.value = null
    }
}
