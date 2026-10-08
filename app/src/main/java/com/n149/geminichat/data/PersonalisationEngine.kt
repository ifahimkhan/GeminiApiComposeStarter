package com.n149.geminichat.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the user's own typed feedback about past Gemini responses and
 * converts it into a concrete system-prompt that adjusts future answers.
 *
 * HOW IT WORKS
 * ─────────────
 * 1. TOPIC detection — scans recent user questions for domain keywords
 *    (code / math / science / health / history / creative / business).
 *
 * 2. FEEDBACK SIGNAL — the user's typed sentences are scanned for
 *    adjustment keywords:
 *      • "too long / lengthy / verbose"  → shorten replies
 *      • "too short / more detail / explain more / expand"  → lengthen
 *      • "simple / easier / basic / layman"  → simplify language
 *      • "technical / detailed / advanced / expert"  → go deeper
 *      • "example / show me / code / snippet"  → add examples
 *      • "wrong / incorrect / mistake / error"  → be more careful & accurate
 *      • "great / perfect / good / helpful / love it"  → keep current style
 *
 * 3. The signals are combined into plain English instructions that Gemini
 *    can follow directly as a system instruction.
 */
@Singleton
class PersonalisationEngine @Inject constructor() {

    // ── Topic keyword banks ──────────────────────────────────────────────────

    private val topicMap = mapOf(
        "code"     to setOf("code","program","function","class","bug","error","kotlin","java",
                            "python","android","api","compile","debug","script","algorithm","syntax"),
        "science"  to setOf("science","physics","chemistry","biology","space","universe",
                            "experiment","atom","molecule","gravity","quantum","evolution"),
        "math"     to setOf("math","calculus","algebra","equation","integral","derivative",
                            "probability","statistics","geometry","matrix","proof"),
        "history"  to setOf("history","war","empire","ancient","century","revolution",
                            "civilization","historical","king","queen","dynasty","era"),
        "health"   to setOf("health","diet","exercise","nutrition","medicine","disease",
                            "symptom","doctor","fitness","mental","wellness","yoga","calories"),
        "creative" to setOf("story","poem","write","creative","fiction","novel","song",
                            "lyric","script","character","plot","narrative","essay"),
        "business" to setOf("business","startup","finance","invest","market","economy",
                            "product","strategy","revenue","profit","entrepreneur","salary")
    )

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Builds a fully personalised system instruction from context.
     *
     * @param recentUserMessages  Last ≤20 user message strings.
     * @param recentFeedbackTexts Last ≤10 feedback strings the user typed.
     */
    fun buildSystemPrompt(
        recentUserMessages: List<String>,
        recentFeedbackTexts: List<String>
    ): String {
        val topic        = detectTopic(recentUserMessages)
        val feedbackHints = parseFeedback(recentFeedbackTexts)

        return buildString {
            append("You are Gemini, a helpful, honest and harmless AI assistant embedded in the ")
            append("N149 Gemini Chat Android app.\n\n")

            // ── Topic specialisation ────────────────────────────────────
            when (topic) {
                "code"     -> append("The user frequently asks about programming. " +
                                     "Prefer fenced code blocks with the language label. " +
                                     "Explain what the code does after showing it.\n")
                "science"  -> append("The user enjoys science discussions. Use precise terminology " +
                                     "and real-world analogies. Cite concepts accurately.\n")
                "math"     -> append("The user asks math questions. Always show step-by-step working. " +
                                     "Use clear plain-text notation.\n")
                "history"  -> append("The user is interested in history. Provide dates, context, " +
                                     "and cause-and-effect explanations.\n")
                "health"   -> append("The user asks health questions. Always recommend consulting " +
                                     "a professional for personal medical decisions. Be empathetic.\n")
                "creative" -> append("The user enjoys creative writing. Match the tone they request. " +
                                     "Be imaginative and vivid.\n")
                "business" -> append("The user is interested in business and finance. Be data-driven " +
                                     "and practical.\n")
                else       -> append("Answer helpfully across any topic.\n")
            }

            // ── Feedback-driven adjustments ─────────────────────────────
            if (feedbackHints.shortenReplies)
                append("IMPORTANT: The user has asked for shorter answers. Be concise — " +
                       "aim for 2–4 sentences unless the question truly requires more.\n")

            if (feedbackHints.lengthenReplies)
                append("IMPORTANT: The user wants more detail. Give thorough, well-structured " +
                       "answers with sub-points where helpful.\n")

            if (feedbackHints.simplifyLanguage)
                append("IMPORTANT: The user wants simpler language. Avoid jargon; " +
                       "explain as if speaking to a curious beginner.\n")

            if (feedbackHints.goDeeper)
                append("IMPORTANT: The user wants technical depth. Use precise terminology, " +
                       "go into underlying mechanisms, and don't over-simplify.\n")

            if (feedbackHints.addExamples)
                append("IMPORTANT: The user explicitly asked for examples. " +
                       "Always include at least one concrete example or code snippet.\n")

            if (feedbackHints.beMoreAccurate)
                append("IMPORTANT: The user flagged inaccuracies in past answers. " +
                       "Double-check every factual claim before stating it. " +
                       "If uncertain, say so explicitly.\n")

            if (feedbackHints.keepCurrentStyle)
                append("The user is happy with the current response style. Maintain it.\n")

            // ── Universal rules ─────────────────────────────────────────
            append("\nIMPORTANT LANGUAGE RULE: Always respond in English unless the user explicitly requests another language. ")
            append("Common greetings like 'hi', 'hello', 'hey' must always be greeted back in English. ")
            append("Be direct, concise, and helpful. ")
            append("Skip filler phrases like \"Certainly!\", \"Of course!\", \"Great question!\".")
        }
    }

    // ── Topic detection ──────────────────────────────────────────────────────

    fun detectTopicLabel(recentUserMessages: List<String>): String {
        return when (detectTopic(recentUserMessages)) {
            "code"     -> "💻 Code"
            "science"  -> "🔬 Science"
            "math"     -> "📐 Math"
            "history"  -> "📜 History"
            "health"   -> "🏥 Health"
            "creative" -> "✍️ Creative"
            "business" -> "📈 Business"
            else       -> "✦ General"
        }
    }

    private fun detectTopic(messages: List<String>): String {
        if (messages.isEmpty()) return "general"
        val scores = topicMap.mapValues { (_, kws) ->
            messages.sumOf { msg ->
                val lower = msg.lowercase()
                kws.count { kw -> lower.contains(kw) }
            }
        }
        val best = scores.maxByOrNull { it.value }
        return if ((best?.value ?: 0) == 0) "general" else best!!.key
    }

    // ── Feedback text parser ─────────────────────────────────────────────────

    private fun parseFeedback(texts: List<String>): FeedbackSignals {
        if (texts.isEmpty()) return FeedbackSignals()
        val combined = texts.joinToString(" ").lowercase()

        return FeedbackSignals(
            shortenReplies  = combined.containsAny("too long","too lengthy","too verbose",
                                                    "shorter","brief","concise","less text"),
            lengthenReplies = combined.containsAny("too short","more detail","explain more",
                                                    "expand","elaborate","not enough","deeper"),
            simplifyLanguage= combined.containsAny("too complex","simpler","simple","easier",
                                                    "basic","layman","don't understand","confusing"),
            goDeeper        = combined.containsAny("technical","advanced","expert","more depth",
                                                    "detailed explanation","in depth","thorough"),
            addExamples     = combined.containsAny("example","show me","code example","snippet",
                                                    "demonstrate","give me an example"),
            beMoreAccurate  = combined.containsAny("wrong","incorrect","mistake","inaccurate",
                                                    "error","not right","that's wrong","factually"),
            keepCurrentStyle= combined.containsAny("great","perfect","love it","helpful","good answer",
                                                    "well explained","excellent","keep it up")
        )
    }

    private data class FeedbackSignals(
        val shortenReplies  : Boolean = false,
        val lengthenReplies : Boolean = false,
        val simplifyLanguage: Boolean = false,
        val goDeeper        : Boolean = false,
        val addExamples     : Boolean = false,
        val beMoreAccurate  : Boolean = false,
        val keepCurrentStyle: Boolean = false
    )

    private fun String.containsAny(vararg phrases: String) = phrases.any { contains(it) }
}
