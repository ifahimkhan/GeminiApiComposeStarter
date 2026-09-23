package com.fahim.geminiApiComposeStarter.core.ai

import com.fahim.geminiApiComposeStarter.model.StudyMode

/**
 * Represents a code snippet extracted from Gemini output.
 */
data class CodeBlock(
    val language: String,
    val code: String,
)

/**
 * Represents an individual multiple-choice question extracted from a Quiz response.
 */
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
)

/**
 * Structured application-level response produced by the ResponseEngine.
 */
data class AiResponse(
    val rawText: String,
    val mode: StudyMode,
    val codeBlocks: List<CodeBlock> = emptyList(),
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val suggestedActions: List<String> = emptyList(),
)

object AiResponseParser {

    private val CODE_BLOCK_REGEX = Regex("```([a-zA-Z0-9_-]*)\n([\\s\\S]*?)```")

    /**
     * Parses raw Gemini response text into a structured AiResponse.
     * Guaranteed to never throw — falls back gracefully to rawText.
     */
    fun parse(rawText: String, mode: StudyMode): AiResponse {
        val trimmed = rawText.trim()
        val codeBlocks = extractCodeBlocks(trimmed)
        val suggestedActions = deriveSuggestedActions(mode, trimmed)
        val quizQuestions = if (mode == StudyMode.QUIZ) parseQuizQuestions(trimmed) else emptyList()

        return AiResponse(
            rawText = trimmed,
            mode = mode,
            codeBlocks = codeBlocks,
            quizQuestions = quizQuestions,
            suggestedActions = suggestedActions,
        )
    }

    private fun extractCodeBlocks(text: String): List<CodeBlock> {
        val matches = CODE_BLOCK_REGEX.findAll(text)
        return matches.map { match ->
            val lang = match.groupValues[1].ifBlank { "text" }
            val code = match.groupValues[2].trimEnd()
            CodeBlock(language = lang, code = code)
        }.toList()
    }

    private fun deriveSuggestedActions(mode: StudyMode, text: String): List<String> {
        return when (mode) {
            StudyMode.CHAT -> listOf("Explain simpler", "Summarize this", "Quiz me on this")
            StudyMode.EXPLAIN -> listOf("Give another example", "Explain for a beginner", "Test my understanding")
            StudyMode.STUDY -> listOf("Continue", "Give me a hint", "Explain simpler", "End Session")
            StudyMode.QUIZ -> listOf("Next Question", "Explain correct answer", "Restart Quiz")
            StudyMode.CHALLENGE -> listOf("Give a hint", "I give up, show solution", "Harder challenge")
            StudyMode.SUMMARIZE -> listOf("Make it shorter", "Key formulas only", "Create quiz from this")
            StudyMode.CODE_REVIEW -> listOf("How to test this?", "Optimize time complexity", "Make it cleaner")
            StudyMode.IMAGE_ANALYSIS -> listOf("Explain step-by-step", "Extract text/formulas", "Solve problem")
            StudyMode.FLASHCARDS -> listOf("Add card", "Flip all", "Practice deck", "Shuffle")
            StudyMode.EXAM_SIMULATOR -> listOf("Submit Exam", "Time left?", "Review flagged questions")
            StudyMode.STUDY_PLANNER -> listOf("Adjust timetable", "Add more days", "Save Plan")
            StudyMode.REVISION -> listOf("Next weak topic", "Explain where I failed", "More drills")
            StudyMode.WEAK_TOPIC_DETECTOR -> listOf("Start targeted quiz", "View weak topics", "Deep dive")
            StudyMode.ASSIGNMENT_ASSISTANT -> listOf("Check rubric", "Verify edge cases", "Structure report")
        }
    }

    /**
     * Attempts to parse questions formatted with Q: / Options: / Answer: or 1. / A) B) C) D).
     * If the format is unconventional, returns emptyList and the rawText is rendered safely.
     */
    private fun parseQuizQuestions(text: String): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        try {
            val blocks = text.split(Regex("\n(?=\\d+\\.|Question\\s*\\d*:)"))
            for ((index, block) in blocks.withIndex()) {
                val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.size < 4) continue

                val questionLine = lines.firstOrNull { it.matches(Regex("^(?:\\d+\\.|Question.*?:).*")) } ?: lines[0]
                val questionText = questionLine.replace(Regex("^(?:\\d+\\.|Question\\s*\\d*:)\\s*"), "")

                val optionLines = lines.filter { it.matches(Regex("^[A-Da-d][\\.\\)]\\s*.*")) }
                if (optionLines.size >= 2) {
                    val options = optionLines.map { it.replace(Regex("^[A-Da-d][\\.\\)]\\s*"), "") }
                    val answerLine = lines.firstOrNull { it.contains("Answer", ignoreCase = true) || it.contains("Correct", ignoreCase = true) }
                    var correctIdx = 0
                    if (answerLine != null) {
                        val letter = Regex("([A-Da-d])").find(answerLine)?.value?.uppercase()
                        correctIdx = when (letter) {
                            "A" -> 0
                            "B" -> 1
                            "C" -> 2
                            "D" -> 3
                            else -> 0
                        }
                    }
                    val explanation = lines.firstOrNull { it.contains("Explanation", ignoreCase = true) }
                        ?.replace(Regex("(?i)Explanation:\\s*"), "") ?: ""

                    questions.add(
                        QuizQuestion(
                            id = "q_$index",
                            question = questionText,
                            options = options,
                            correctOptionIndex = correctIdx.coerceIn(0, options.size - 1),
                            explanation = explanation,
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Never fail parsing — fallback to empty questions list
        }
        return questions
    }
}
