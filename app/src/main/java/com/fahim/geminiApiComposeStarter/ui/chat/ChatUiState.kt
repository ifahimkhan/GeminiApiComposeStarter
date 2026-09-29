package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatAttachment
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSession

data class ModelOption(
    val id: String,
    val displayName: String,
    val description: String,
)

val PRESET_MODELS = listOf(
    ModelOption(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Default choice for most apps (Recommended)",
    ),
    ModelOption(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Default choice for most apps",
    ),
    ModelOption(
        id = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "High-volume / Budget-critical tasks",
    ),
    ModelOption(
        id = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash-Lite",
        description = "Ultra-fast & cost-efficient lightweight model",
    ),
    ModelOption(
        id = "gemini-3.1-pro",
        displayName = "Gemini 3.1 Pro",
        description = "Complex logic & reasoning",
    ),
    ModelOption(
        id = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        description = "Complex logic & reasoning",
    ),
)

/** Immutable UI state for the multi-session chat flow with attachments. */
data class ChatUiState(
    val prompt: String = "",
    val pendingAttachments: List<ChatAttachment> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedModel: String = "gemini-3.8-flash",
    val showModelDialog: Boolean = false,
    val showAttachmentPicker: Boolean = false,
)

enum class PromptError { EMPTY }
