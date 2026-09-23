package com.fahim.geminiApiComposeStarter.core.ai

enum class PlanStepType {
    LOCAL_MEMORY_OPERATION,
    LOCAL_TOOL_CALCULATION,
    DOCUMENT_CONTEXT_ASSEMBLY,
    GEMINI_REASONING,
    STRUCTURED_OUTPUT_PARSING,
}

data class PlanStep(
    val type: PlanStepType,
    val description: String,
)

data class ExecutionPlan(
    val steps: List<PlanStep>,
    val primaryIntent: IntentType,
    val directLocalResponse: String? = null,
)

class TaskPlanner {

    fun plan(intent: ClassifiedIntent, prompt: String, hasDocument: Boolean): ExecutionPlan {
        return when (intent.type) {
            IntentType.MEMORY_COMMAND -> {
                ExecutionPlan(
                    steps = listOf(
                        PlanStep(PlanStepType.LOCAL_MEMORY_OPERATION, "Process natural memory command locally"),
                    ),
                    primaryIntent = intent.type,
                )
            }

            IntentType.CALCULATION -> {
                ExecutionPlan(
                    steps = listOf(
                        PlanStep(PlanStepType.LOCAL_TOOL_CALCULATION, "Evaluate math expression via MathEngineTool"),
                        PlanStep(PlanStepType.GEMINI_REASONING, "Provide pedagogical explanation and derivation"),
                    ),
                    primaryIntent = intent.type,
                )
            }

            IntentType.DOCUMENT_QUERY -> {
                ExecutionPlan(
                    steps = listOf(
                        PlanStep(PlanStepType.DOCUMENT_CONTEXT_ASSEMBLY, "Extract text from attached document"),
                        PlanStep(PlanStepType.GEMINI_REASONING, "Analyze document and generate structured study answer"),
                    ),
                    primaryIntent = intent.type,
                )
            }

            IntentType.STUDY_WORKFLOW -> {
                ExecutionPlan(
                    steps = listOf(
                        PlanStep(PlanStepType.GEMINI_REASONING, "Generate structured pedagogical study material"),
                        PlanStep(PlanStepType.STRUCTURED_OUTPUT_PARSING, "Parse into interactive Quiz, Flashcards, or Plan"),
                    ),
                    primaryIntent = intent.type,
                )
            }

            IntentType.CODE_ANALYSIS,
            IntentType.GENERAL_CONVERSATION -> {
                ExecutionPlan(
                    steps = listOf(
                        PlanStep(PlanStepType.GEMINI_REASONING, "Query Gemini with conversational and memory context"),
                    ),
                    primaryIntent = intent.type,
                )
            }
        }
    }
}
