package com.fahim.geminiApiComposeStarter.ui.chat

/** Recognizes explicit image requests; ordinary chat continues through the text model. */
internal fun requestsImageCreation(prompt: String): Boolean {
    val request = prompt.trim().lowercase()
        .replace(Regex("^(please\\s+|(?:can|could|would) you\\s+)+"), "")
    if (Regex("^(draw|paint|illustrate|sketch)\\b").containsMatchIn(request)) return true
    return Regex(
        "^(create|generate|make|design)\\s+(?:(?:me|us)\\s+)?(?:(?:an?|the|some)\\s+)?" +
            "(?:[\\w-]+\\s+){0,4}(image|picture|photo|illustration|poster|logo|wallpaper|artwork|icon)\\b",
    ).containsMatchIn(request)
}
