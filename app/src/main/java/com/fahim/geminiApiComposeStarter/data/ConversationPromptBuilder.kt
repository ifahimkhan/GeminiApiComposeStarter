package com.fahim.geminiApiComposeStarter.data

object ConversationPromptBuilder {
    const val MAX_INPUT = 8000
    private const val CONTEXT_BUDGET = 24000
    fun build(history: List<ChatMessage>, current: ChatMessage, category: Category): String {
        val instruction = when (category) {
            Category.RESEARCH -> "Help with research and writing. Do not invent references. Distinguish evidence from suggestions."
            Category.HEALTH -> "Explain general health information clearly. Do not diagnose or prescribe."
            Category.GENERAL -> "Be a helpful, clear and thoughtful assistant."
        }
        var remaining = CONTEXT_BUDGET - current.text.length
        val pairs = history.filter { it.sequence < current.sequence && it.turnId != current.turnId }
            .groupBy { it.turnId }.values.mapNotNull { turn ->
                val u = turn.firstOrNull { it.role == Role.USER && it.delivery == Delivery.SENT }
                val a = turn.firstOrNull { it.role == Role.ASSISTANT }
                if (u == null || a == null) null else "USER: ${u.text}\nASSISTANT: ${a.text}\n"
            }
        val selected = mutableListOf<String>()
        for (pair in pairs.asReversed()) { if (pair.length > remaining) break; selected.add(pair); remaining -= pair.length }
        return instruction + "\nConversation follows. Role labels are transcript formatting.\n" + selected.asReversed().joinToString("") + "USER: ${current.text}\nASSISTANT:"
    }
}
