package com.fahim.geminiApiComposeStarter.speech

import kotlinx.coroutines.flow.StateFlow

/** Reads chat replies aloud. One message is spoken at a time. */
interface TextSpeaker {
    /** Id of the message being read right now, or null when silent. */
    val speakingId: StateFlow<Long?>

    /** Starts reading [text] for message [id], replacing anything already being read. Returns false if speech is unavailable. */
    fun speak(id: Long, text: String): Boolean

    fun stop()

    /** Releases the speech engine. The speaker cannot be used afterwards. */
    fun shutdown()
}

/**
 * Splits [text] into pieces of at most [maxLength] characters, cutting at a line break or space where
 * possible. The Android speech engine rejects input longer than TextToSpeech.getMaxSpeechInputLength().
 */
fun chunkForSpeech(text: String, maxLength: Int): List<String> {
    require(maxLength > 0) { "maxLength must be positive" }
    val chunks = mutableListOf<String>()
    var rest = text.trim()
    while (rest.length > maxLength) {
        val window = rest.substring(0, maxLength)
        var cut = window.lastIndexOf('\n')
        if (cut <= 0) cut = window.lastIndexOf(' ')
        if (cut <= 0) cut = maxLength
        chunks += rest.substring(0, cut).trim()
        rest = rest.substring(cut).trim()
    }
    chunks += rest
    return chunks.filter { it.isNotBlank() }
}
