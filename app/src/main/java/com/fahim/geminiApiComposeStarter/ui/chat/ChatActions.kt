package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode

/** All UI events, hoisted to the ViewModel. Defaults are no-ops so previews and tests stay short. */
@Immutable
data class ChatActions(
    val onPromptChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onErrorShown: () -> Unit = {},
    val onMicClick: () -> Unit = {},
    val onClearHistory: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onDismissSettings: () -> Unit = {},
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onDynamicColorChange: (Boolean) -> Unit = {},
    val onResponseStyleChange: (ResponseStyle) -> Unit = {},
)

/** Layout buckets derived from WindowSizeClass (phone / small tablet or foldable / large tablet). */
enum class ChatLayout { COMPACT, MEDIUM, EXPANDED }
