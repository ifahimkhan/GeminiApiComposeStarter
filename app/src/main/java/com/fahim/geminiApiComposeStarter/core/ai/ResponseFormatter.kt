package com.fahim.geminiApiComposeStarter.core.ai

object ResponseFormatter {

    fun formatWithToolBadge(rawResponse: String, toolBadge: String? = null): String {
        val trimmed = rawResponse.trim()
        return if (toolBadge.isNullOrBlank()) {
            trimmed
        } else {
            "$trimmed\n\n> ⚡ *$toolBadge*"
        }
    }

    fun formatMemoryResponse(content: String): String {
        return "🧠 **Memory Updated**\n\n$content"
    }

    fun cleanMarkdown(text: String): String {
        return text.trim()
    }
}
