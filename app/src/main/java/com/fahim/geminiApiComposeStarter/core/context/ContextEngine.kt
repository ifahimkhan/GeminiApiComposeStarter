package com.fahim.geminiApiComposeStarter.core.context

import com.fahim.geminiApiComposeStarter.core.ai.StudyPromptBuilder
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.StudyMode

/**
 * Encapsulates the bounded, curated context passed to the Gemini Gateway.
 */
data class ConversationContext(
    val recentMessages: List<ChatMessage>,
    val systemInstruction: String,
    val effectivePrompt: String,
    val mode: StudyMode,
    val memoryContext: String = "",
)

/**
 * Prepares bounded conversational context for the Gemini model.
 * Injects persistent user memory and prevents token overflow while preserving conversational continuity.
 */
class ContextEngine(
    private val maxHistoryWindow: Int = 10,
) {

    fun buildContext(
        history: List<ChatMessage>,
        newPrompt: String,
        mode: StudyMode,
        relevantSavedNotes: List<ChatMessage> = emptyList(),
        userMemories: List<UserMemoryEntity> = emptyList(),
    ): ConversationContext {
        // Rolling window of the most recent messages (excluding the new prompt which is sent separately)
        val boundedHistory = if (history.size > maxHistoryWindow) {
            history.takeLast(maxHistoryWindow)
        } else {
            history
        }

        val baseInstruction = StudyPromptBuilder.getSystemInstruction(mode)
        val instructions = mutableListOf<String>()
        instructions.add(baseInstruction)

        if (relevantSavedNotes.isNotEmpty()) {
            val notesSummary = relevantSavedNotes.take(3).joinToString("\n---\n") { it.content.take(150) }
            instructions.add("Student's saved reference notes:\n$notesSummary")
        }

        val memoryBlock = formatUserMemories(userMemories)
        if (memoryBlock.isNotBlank()) {
            instructions.add(
                "Persistent User Profile & Long-Term Preferences (from local memory):\n" +
                    "Information in LOCAL USER MEMORY represents facts explicitly stored by the user. " +
                    "Use these facts when relevant. Do not claim not to know information that is explicitly present in LOCAL USER MEMORY.\n\n" +
                    memoryBlock
            )

            // Log sanitized representation for debugging (never log API keys or secrets)
            val sanitized = memoryBlock.removePrefix("USER MEMORY:\n").trim()
            try {
                android.util.Log.d("MemoryContext", "MEMORY CONTEXT:\n$sanitized")
            } catch (_: Throwable) {
                println("MEMORY CONTEXT:\n$sanitized")
            }
        }

        val fullInstruction = instructions.joinToString("\n\n")
        val effectivePrompt = StudyPromptBuilder.wrapPrompt(newPrompt.trim(), mode)

        return ConversationContext(
            recentMessages = boundedHistory,
            systemInstruction = fullInstruction,
            effectivePrompt = effectivePrompt,
            mode = mode,
            memoryContext = memoryBlock,
        )
    }

    fun formatUserMemories(memories: List<UserMemoryEntity>): String {
        if (memories.isEmpty()) return ""
        val lines = memories.map { mem ->
            val c = mem.content.trim()
            when {
                c.startsWith("my name is ", ignoreCase = true) -> {
                    val name = c.substring("my name is ".length).trim().trimEnd('.', '!', ',').replaceFirstChar { it.uppercase() }
                    "Name: $name"
                }
                c.startsWith("name:", ignoreCase = true) -> {
                    "Name: " + c.substring("name:".length).trim().replaceFirstChar { it.uppercase() }
                }
                else -> c
            }
        }
        return "USER MEMORY:\n" + lines.joinToString("\n")
    }
}
