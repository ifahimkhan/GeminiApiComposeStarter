package com.fahim.geminiApiComposeStarter.core.tools

enum class ToolSafetyLevel {
    SAFE_READONLY,
    REQUIRES_CONFIRMATION,
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val safetyLevel: ToolSafetyLevel = ToolSafetyLevel.SAFE_READONLY,
)

sealed class ToolExecutionResult {
    data class Success(val toolName: String, val output: String, val badgeLabel: String) : ToolExecutionResult()
    data class Failure(val toolName: String, val errorMessage: String) : ToolExecutionResult()
}

class ToolRegistry(
    val mathTool: MathEngineTool = MathEngineTool(),
    val documentTool: DocumentProcessorTool = DocumentProcessorTool(),
    val smartActionTool: SmartActionTool = SmartActionTool(),
) {
    val registeredTools = listOf(
        ToolDefinition(
            name = "math_calculator",
            description = "Evaluates arithmetic expressions, statistics (mean, variance, std dev), percentages, and unit conversions.",
            safetyLevel = ToolSafetyLevel.SAFE_READONLY,
        ),
        ToolDefinition(
            name = "document_intelligence",
            description = "Extracts structure and text from user-attached PDF, text, and data files.",
            safetyLevel = ToolSafetyLevel.SAFE_READONLY,
        ),
        ToolDefinition(
            name = "smart_actions",
            description = "Creates study reminders, copies text, and shares notes via Android intents.",
            safetyLevel = ToolSafetyLevel.SAFE_READONLY,
        ),
        ToolDefinition(
            name = "code_execution",
            description = "Executes computational Python and algorithmic code blocks.",
            safetyLevel = ToolSafetyLevel.SAFE_READONLY,
        ),
        ToolDefinition(
            name = "web_grounding",
            description = "Verifies recent real-time facts and technological developments with web citations.",
            safetyLevel = ToolSafetyLevel.SAFE_READONLY,
        ),
    )

    fun executeMath(expression: String): ToolExecutionResult {
        val result = mathTool.execute(expression)
        return if (result.isSuccess) {
            ToolExecutionResult.Success(
                toolName = "math_calculator",
                output = "${result.explanation}\n\nResult: `${result.result}`",
                badgeLabel = "Calculated via Math Engine",
            )
        } else {
            ToolExecutionResult.Failure(
                toolName = "math_calculator",
                errorMessage = result.explanation,
            )
        }
    }
}
