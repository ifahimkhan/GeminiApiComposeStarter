package com.fahim.geminiApiComposeStarter.core.ai

enum class IntentType {
    MEMORY_COMMAND,
    CALCULATION,
    DOCUMENT_QUERY,
    CODE_ANALYSIS,
    STUDY_WORKFLOW,
    GENERAL_CONVERSATION,
}

data class ClassifiedIntent(
    val type: IntentType,
    val confidence: Float,
    val details: String? = null,
)

class IntentRouter {

    fun classify(prompt: String, hasAttachedDocument: Boolean = false): ClassifiedIntent {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // 1. Natural Memory Commands
        if (isMemoryCommand(lower)) {
            return ClassifiedIntent(IntentType.MEMORY_COMMAND, 0.95f)
        }

        // 2. Document Query
        if (hasAttachedDocument || isDocumentCommand(lower)) {
            return ClassifiedIntent(IntentType.DOCUMENT_QUERY, 0.90f)
        }

        // 3. Calculation / Math
        if (isCalculationCommand(trimmed, lower)) {
            return ClassifiedIntent(IntentType.CALCULATION, 0.85f)
        }

        // 4. Code Review / Execution
        if (isCodeCommand(trimmed, lower)) {
            return ClassifiedIntent(IntentType.CODE_ANALYSIS, 0.85f)
        }

        // 5. Explicit Study Workflow Commands
        if (isStudyWorkflowCommand(lower)) {
            return ClassifiedIntent(IntentType.STUDY_WORKFLOW, 0.90f)
        }

        return ClassifiedIntent(IntentType.GENERAL_CONVERSATION, 0.50f)
    }

    private fun isMemoryCommand(lower: String): Boolean {
        return lower.startsWith("remember that") ||
            lower.startsWith("remember:") ||
            lower.startsWith("forget that") ||
            lower.startsWith("forget everything") ||
            lower.contains("what do you remember about me") ||
            lower.contains("what do you know about me") ||
            lower.contains("show my memories")
    }

    private fun isDocumentCommand(lower: String): Boolean {
        return lower.contains("this pdf") ||
            lower.contains("this document") ||
            lower.contains("summarize the document") ||
            lower.contains("from this file")
    }

    private fun isCalculationCommand(raw: String, lower: String): Boolean {
        // Explicit calculation request
        if (lower.startsWith("calculate") ||
            lower.startsWith("solve") ||
            lower.startsWith("compute") ||
            lower.contains("standard deviation") ||
            lower.contains("mean of") ||
            lower.contains("average of") ||
            lower.contains("variance of") ||
            lower.contains("convert ") && (lower.contains("celsius") || lower.contains("fahrenheit") || lower.contains("km") || lower.contains("miles") || lower.contains("kg") || lower.contains("lbs"))
        ) {
            return true
        }

        // Mathematical expression syntax detection e.g. "25 * 4 + 10" or "sqrt(144)"
        val mathPattern = Regex("""^[\d\s\+\-\*\/\^\(\)\.\,\%\=]+$""")
        if (raw.length in 3..60 && mathPattern.matches(raw) && raw.any { it in "+-*/^%" }) {
            return true
        }

        return false
    }

    private fun isCodeCommand(raw: String, lower: String): Boolean {
        if (raw.contains("```") ||
            lower.contains("review this code") ||
            lower.contains("debug this") ||
            lower.contains("run this code") ||
            lower.contains("execute this python") ||
            lower.contains("time complexity") ||
            lower.contains("refactor this function")
        ) {
            return true
        }
        return false
    }

    private fun isStudyWorkflowCommand(lower: String): Boolean {
        return lower.startsWith("quiz me") ||
            lower.startsWith("create flashcards") ||
            lower.startsWith("make flashcards") ||
            lower.startsWith("generate flashcards") ||
            lower.startsWith("study plan") ||
            lower.startsWith("create a study plan") ||
            lower.startsWith("exam simulator") ||
            lower.startsWith("revision mode")
    }
}
