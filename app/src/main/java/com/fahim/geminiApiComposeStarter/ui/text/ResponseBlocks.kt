package com.fahim.geminiApiComposeStarter.ui.text

data class ResponseBlock(val text: String, val language: String? = null)

/** Fenced code/text stays literal; ordinary prose remains on the screen background. */
fun parseResponseBlocks(text: String): List<ResponseBlock> {
    val blocks = mutableListOf<ResponseBlock>()
    val buffer = StringBuilder()
    var language: String? = null
    fun flush() {
        if (buffer.isNotEmpty()) blocks += ResponseBlock(buffer.toString().trimEnd('\n'), language)
        buffer.clear()
    }
    text.lineSequence().forEach { line ->
        if (line.trimStart().startsWith("```")) {
            flush()
            language = if (language == null) line.trim().removePrefix("```").trim() else null
        } else buffer.append(line).append('\n')
    }
    flush()
    return blocks
}
