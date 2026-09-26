package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.BuildConfig

data class GeminiModelOption(val id: String, val title: String, val detail: String, val badge: String)

/** Curated text models checked against Google's models endpoint on 2026-09-25.
 * Listing a model does not guarantee a project's quota or access. No silent fallback. */
object GeminiModels {
    private val curated = listOf(
        GeminiModelOption("gemini-3.6-flash", "Gemini 3.6 Flash", "Everyday questions, writing and ideas", "EVERYDAY"),
        GeminiModelOption("gemini-3.8-flash", "Gemini 3.8 Flash", "Coding and more involved tasks", "EXPLORE"),
        GeminiModelOption("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", "Short questions and lightweight tasks", "LIGHT"),
        GeminiModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Complex questions and reasoning", "PREVIEW"),
    )
    val options: List<GeminiModelOption> = run {
        val configured = curated.firstOrNull { it.id == BuildConfig.GEMINI_MODEL }
            ?: GeminiModelOption(BuildConfig.GEMINI_MODEL, BuildConfig.GEMINI_MODEL, "Your configured model", "DEFAULT")
        listOf(configured) + curated.filterNot { it.id == configured.id }
    }
    fun selected(id: String): GeminiModelOption = options.firstOrNull { it.id == id } ?: options.first()
    fun normalize(id: String): String = if (id.isBlank()) "" else selected(id).id
}
