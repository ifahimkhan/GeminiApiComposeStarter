package com.fahim.geminiApiComposeStarter.core.ai

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.core.context.ContextEngine
import com.fahim.geminiApiComposeStarter.core.memory.MemoryCommandResult
import com.fahim.geminiApiComposeStarter.core.memory.MemoryEngine
import com.fahim.geminiApiComposeStarter.core.tools.ToolRegistry
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.StudyMode

sealed class AICoreResponse {
    data class Direct(val message: String, val badge: String? = null) : AICoreResponse()
    data class Success(val message: String, val badge: String? = null) : AICoreResponse()
    data class Error(val errorMessage: String) : AICoreResponse()
}

class AICore(
    val intentRouter: IntentRouter = IntentRouter(),
    val memoryEngine: MemoryEngine,
    val taskPlanner: TaskPlanner = TaskPlanner(),
    val toolRegistry: ToolRegistry = ToolRegistry(),
    val contextEngine: ContextEngine = ContextEngine(),
) {

    suspend fun execute(
        prompt: String,
        history: List<ChatMessage>,
        mode: StudyMode,
        attachedBitmap: Bitmap? = null,
        attachedDocText: String? = null,
        savedNotes: List<ChatMessage> = emptyList(),
        geminiRepository: GeminiRepository,
    ): AICoreResponse {
        val trimmedPrompt = prompt.trim()

        // Step 1: Check natural memory commands first
        val memoryResult = memoryEngine.processNaturalCommand(trimmedPrompt)
        when (memoryResult) {
            is MemoryCommandResult.Saved -> {
                val formatted = "Got it! I will remember: \"${memoryResult.fact}\" (Category: ${memoryResult.category.name})."
                return AICoreResponse.Direct(ResponseFormatter.formatMemoryResponse(formatted))
            }
            is MemoryCommandResult.Forgotten -> {
                val formatted = if (memoryResult.count > 0) {
                    "I have forgotten ${memoryResult.count} memory item(s) matching \"${memoryResult.keyword}\"."
                } else {
                    "No memories found matching \"${memoryResult.keyword}\"."
                }
                return AICoreResponse.Direct(ResponseFormatter.formatMemoryResponse(formatted))
            }
            is MemoryCommandResult.ListAll -> {
                val formatted = if (memoryResult.memories.isEmpty()) {
                    "I don't have any saved memories about you yet. You can say: \"Remember that [fact]\"."
                } else {
                    val sb = StringBuilder("Here is what I remember about you:\n\n")
                    memoryResult.memories.forEachIndexed { index, mem ->
                        sb.append("${index + 1}. **[${mem.category}]** ${mem.content}\n")
                    }
                    sb.toString()
                }
                return AICoreResponse.Direct(ResponseFormatter.formatMemoryResponse(formatted))
            }
            is MemoryCommandResult.ClearedAll -> {
                return AICoreResponse.Direct(ResponseFormatter.formatMemoryResponse("All persistent memories have been deleted."))
            }
            MemoryCommandResult.NotACommand -> {
                // Continue to intent classification
            }
        }

        // Step 2: Classify Intent & Plan
        val classifiedIntent = intentRouter.classify(
            prompt = trimmedPrompt,
            hasAttachedDocument = !attachedDocText.isNullOrBlank(),
        )
        val plan = taskPlanner.plan(
            intent = classifiedIntent,
            prompt = trimmedPrompt,
            hasDocument = !attachedDocText.isNullOrBlank(),
        )

        // Step 3: Tool Execution if planned
        var toolBadge: String? = null
        var enrichedPrompt = trimmedPrompt

        if (classifiedIntent.type == IntentType.CALCULATION) {
            val mathResult = toolRegistry.mathTool.execute(trimmedPrompt)
            if (mathResult.isSuccess) {
                toolBadge = "Evaluated via Math Engine"
                enrichedPrompt = "$trimmedPrompt\n\n[Exact Tool Calculation Result]: ${mathResult.explanation}"
            }
        }

        if (!attachedDocText.isNullOrBlank()) {
            toolBadge = "Analyzed Attached Document"
            enrichedPrompt = "$enrichedPrompt\n\n[Attached Study Document Content]:\n$attachedDocText"
        }

        // Step 4: Context Assembly with Persistent Memory Injection
        val currentMemories = memoryEngine.getMemoriesSnapshot()
        val baseContext = contextEngine.buildContext(
            history = history,
            newPrompt = enrichedPrompt,
            mode = mode,
            relevantSavedNotes = savedNotes,
            userMemories = currentMemories,
        )

        val fullSystemInstruction = baseContext.systemInstruction

        // Step 5: Execute with Gemini
        val apiResult = if (attachedBitmap != null) {
            geminiRepository.generateMultimodal(
                prompt = baseContext.effectivePrompt,
                imageBitmap = attachedBitmap,
                systemInstruction = fullSystemInstruction,
            )
        } else {
            geminiRepository.generateChat(
                prompt = baseContext.effectivePrompt,
                history = baseContext.recentMessages,
                systemInstruction = fullSystemInstruction,
            )
        }

        return apiResult.fold(
            onSuccess = { rawText ->
                val formatted = ResponseFormatter.formatWithToolBadge(rawText, toolBadge)
                AICoreResponse.Success(message = formatted, badge = toolBadge)
            },
            onFailure = { error ->
                AICoreResponse.Error(error.message ?: "Failed to generate AI response.")
            },
        )
    }
}
