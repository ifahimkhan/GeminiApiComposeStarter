package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.fahim.geminiApiComposeStarter.audio.TextToSpeechManager
import com.fahim.geminiApiComposeStarter.core.tools.DocumentProcessorTool
import com.fahim.geminiApiComposeStarter.ui.dashboard.DashboardScreen
import com.fahim.geminiApiComposeStarter.ui.library.KnowledgeLibraryScreen
import com.fahim.geminiApiComposeStarter.ui.memory.MemoryManagementScreen
import com.fahim.geminiApiComposeStarter.ui.navigation.NavigationDestination
import com.fahim.geminiApiComposeStarter.ui.settings.SettingsScreen
import com.fahim.geminiApiComposeStarter.ui.voice.LiveVoiceDialog
import com.fahim.geminiApiComposeStarter.ui.voice.VoiceState
import com.fahim.geminiApiComposeStarter.ui.workflows.StudyWorkflowsScreen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.core.utils.ImageUtils
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.model.StudyMode
import com.fahim.geminiApiComposeStarter.ui.chat.components.ConversationDrawerContent
import com.fahim.geminiApiComposeStarter.ui.chat.components.MarkdownViewer
import com.fahim.geminiApiComposeStarter.ui.chat.components.SavedNotesDialog
import com.fahim.geminiApiComposeStarter.ui.chat.components.SessionInsightsDialog
import com.fahim.geminiApiComposeStarter.ui.chat.components.StudyModeSelectorBottomSheet
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Test tags ────────────────────────────────────────────────────────────────
object ChatTestTags {
    const val MESSAGE_LIST = "message_list"
    const val INPUT_FIELD = "input_field"
    const val SEND_BUTTON = "send_button"
    const val VOICE_BUTTON = "voice_button"
    const val IMAGE_BUTTON = "image_button"
    const val MODE_BUTTON = "mode_button"
    const val LOADING_INDICATOR = "loading_indicator"
    const val EMPTY_STATE = "empty_state"
    const val ERROR_SNACKBAR = "error_snackbar"
    const val SAVED_NOTES_BUTTON = "saved_notes_button"
    const val INSIGHTS_BUTTON = "insights_button"
}

// ── Route (ViewModel-aware entry point) ──────────────────────────────────────

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    textToSpeechManager: TextToSpeechManager? = null,
    onSpeakMessage: (String, String) -> Unit = { messageId, text ->
        textToSpeechManager?.speak(messageId, text)
    },
    onShareContent: (String) -> Unit = {},
    onUpdateApiKey: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSpeaking by (textToSpeechManager?.isSpeaking?.collectAsStateWithLifecycle()
        ?: remember { androidx.compose.runtime.mutableStateOf(false) })

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = state.selectedNavigation == destination,
                        onClick = { viewModel.setNavigation(destination) },
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (state.selectedNavigation) {
                NavigationDestination.DASHBOARD -> {
                    DashboardScreen(
                        conversations = state.conversations,
                        savedNotesCount = state.savedNotes.size,
                        quizAttempts = state.quizAttempts,
                        flashcardCount = state.flashcards.size,
                        weakTopics = state.weakTopics,
                        onNavigate = { viewModel.setNavigation(it) },
                        onStartMode = { mode ->
                            viewModel.createNewSession(mode)
                        },
                        onOpenVoice = {
                            viewModel.setShowLiveVoiceDialog(true)
                        },
                        onSelectConversation = { convId ->
                            viewModel.loadConversationMessages(convId)
                            viewModel.setNavigation(NavigationDestination.CHAT)
                        },
                        onStudyWeakTopic = { topic ->
                            viewModel.onPromptChange("I want to revise and practice: $topic. Please explain core principles and test me.")
                            viewModel.setNavigation(NavigationDestination.CHAT)
                        },
                    )
                }
                NavigationDestination.CHAT -> {
                    ChatScreen(
                        state = state,
                        onPromptChange = viewModel::onPromptChange,
                        onSend = viewModel::onSend,
                        onDismissError = viewModel::onDismissError,
                        onClearHistory = viewModel::onClearHistory,
                        onModeSelected = viewModel::setMode,
                        onQuickAction = viewModel::onQuickAction,
                        onToggleSaveNote = viewModel::onToggleSaveNote,
                        onSelectConversation = viewModel::loadConversationMessages,
                        onNewConversation = viewModel::createNewSession,
                        onDeleteConversation = viewModel::deleteSession,
                        onImageSelected = viewModel::onImageSelected,
                        onClearImage = viewModel::onClearImage,
                        onShowModeDialog = viewModel::setShowModeDialog,
                        onShowSavedNotes = viewModel::setShowSavedNotesDialog,
                        onShowInsights = viewModel::setShowInsightsDialog,
                        onSpeakMessage = onSpeakMessage,
                        onOpenVoiceDialog = { viewModel.setShowLiveVoiceDialog(true) },
                        onDocumentAttached = viewModel::onDocumentAttached,
                        onClearDocument = viewModel::onClearDocument,
                    )
                }
                NavigationDestination.STUDY_WORKFLOWS -> {
                    StudyWorkflowsScreen(
                        onGeneratePrompt = { prompt, onResult ->
                            viewModel.generateOneOffPrompt(prompt, onResult)
                        },
                        onSaveQuizScore = { topic, score, total, strong, rev ->
                            viewModel.onRecordQuizScore(topic, score, total, strong, rev)
                        },
                        onSaveFlashcards = { cards ->
                            viewModel.saveFlashcards(cards)
                        },
                        onSaveStudyPlan = { plan ->
                            viewModel.saveStudyPlan(plan)
                        },
                        onAddWeakTopic = { topic, subject ->
                            viewModel.addWeakTopic(topic, subject)
                        },
                        savedFlashcards = state.flashcards,
                    )
                }
                NavigationDestination.KNOWLEDGE_LIBRARY -> {
                    KnowledgeLibraryScreen(
                        savedNotes = state.savedNotes,
                        flashcards = state.flashcards,
                        studyPlans = state.studyPlans,
                        weakTopics = state.weakTopics,
                        onDeleteNote = { viewModel.onToggleSaveNote(it) },
                        onDeleteFlashcard = { viewModel.deleteFlashcard(it) },
                        onDeleteStudyPlan = { viewModel.deleteStudyPlan(it) },
                        onResolveWeakTopic = { viewModel.resolveWeakTopic(it) },
                        onShareContent = onShareContent,
                    )
                }
                NavigationDestination.MEMORY -> {
                    MemoryManagementScreen(
                        memories = state.userMemories,
                        onAddMemory = { content, category ->
                            viewModel.addMemory(content, category)
                        },
                        onDeleteMemory = { viewModel.deleteMemory(it) },
                        onClearAllMemories = { viewModel.clearAllMemories() },
                    )
                }
                NavigationDestination.SETTINGS -> {
                    SettingsScreen(
                        hasApiKey = state.hasApiKey,
                        isStreamingEnabled = state.isStreamingEnabled,
                        onToggleStreaming = { viewModel.setStreamingEnabled(it) },
                        isHandsFreeVoice = state.isHandsFreeVoice,
                        onToggleHandsFreeVoice = { viewModel.setHandsFreeVoice(it) },
                        onUpdateApiKey = onUpdateApiKey,
                        onClearAllData = { viewModel.clearAllWorkspaceData() },
                        totalMessagesCount = state.messages.size,
                        savedNotesCount = state.savedNotes.size,
                        flashcardsCount = state.flashcards.size,
                        studyPlansCount = state.studyPlans.size,
                        memoriesCount = state.userMemories.size,
                    )
                }
            }
        }
    }

    if (state.showLiveVoiceDialog) {
        LiveVoiceDialog(
            voiceState = state.voiceState,
            isSpeaking = isSpeaking,
            recentMessages = state.messages,
            handsFreeEnabled = state.isHandsFreeVoice,
            onToggleHandsFree = { viewModel.toggleHandsFreeVoice() },
            onVoiceInputReceived = { spokenText ->
                viewModel.sendVoiceInput(spokenText) { response ->
                    textToSpeechManager?.speak("live_voice", response)
                }
            },
            onInterrupt = {
                textToSpeechManager?.stop()
                viewModel.setVoiceState(VoiceState.IDLE)
            },
            onDismiss = {
                textToSpeechManager?.stop()
                viewModel.setShowLiveVoiceDialog(false)
                viewModel.setVoiceState(VoiceState.IDLE)
            },
        )
    }
}

// ── Main Workspace Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissError: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onModeSelected: (StudyMode) -> Unit = {},
    onQuickAction: (String) -> Unit = {},
    onToggleSaveNote: (ChatMessage) -> Unit = {},
    onSelectConversation: (String) -> Unit = {},
    onNewConversation: (StudyMode) -> Unit = {},
    onDeleteConversation: (String) -> Unit = {},
    onImageSelected: (Bitmap?, String?) -> Unit = { _, _ -> },
    onClearImage: () -> Unit = {},
    onShowModeDialog: (Boolean) -> Unit = {},
    onShowSavedNotes: (Boolean) -> Unit = {},
    onShowInsights: (Boolean) -> Unit = {},
    onSpeakMessage: (String, String) -> Unit = { _, _ -> },
    onOpenVoiceDialog: () -> Unit = {},
    onDocumentAttached: (String, String) -> Unit = { _, _ -> },
    onClearDocument: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Show error in snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    // Auto-scroll to latest message on new content
    LaunchedEffect(state.messages.size, state.isLoading) {
        val targetIndex = state.messages.size - 1
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Modals
    if (state.showModeDialog) {
        StudyModeSelectorBottomSheet(
            currentMode = state.activeMode,
            onModeSelected = onModeSelected,
            onDismiss = { onShowModeDialog(false) },
        )
    }

    if (state.showSavedNotesDialog) {
        SavedNotesDialog(
            savedNotes = state.savedNotes,
            onRemoveNote = onToggleSaveNote,
            onDismiss = { onShowSavedNotes(false) },
        )
    }

    if (state.showInsightsDialog) {
        SessionInsightsDialog(
            attempts = state.quizAttempts,
            onDismiss = { onShowInsights(false) },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawerContent(
                conversations = state.conversations,
                currentConversationId = state.currentConversationId,
                onSelectConversation = onSelectConversation,
                onNewConversation = { onNewConversation(state.activeMode) },
                onDeleteConversation = onDeleteConversation,
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                WorkspaceTopBar(
                    sessionTitle = state.currentConversationTitle,
                    activeMode = state.activeMode,
                    hasMessages = state.messages.isNotEmpty(),
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onShowSavedNotes = { onShowSavedNotes(true) },
                    onShowInsights = { onShowInsights(true) },
                    onOpenVoiceDialog = onOpenVoiceDialog,
                    onClearHistory = onClearHistory,
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.testTag(ChatTestTags.ERROR_SNACKBAR),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Adaptive container centered up to 840dp for tablets and landscape
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                ) {
                    // Mode Selector Bar & Quick Action Chips
                    ModeAndActionHeader(
                        activeMode = state.activeMode,
                        onOpenModeSelector = { onShowModeDialog(true) },
                        onQuickAction = onQuickAction,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Message list or Empty State
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.messages.isEmpty() && !state.isLoading) {
                            WorkspaceEmptyState(
                                activeMode = state.activeMode,
                                onQuickAction = onQuickAction,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(ChatTestTags.EMPTY_STATE),
                            )
                        } else {
                            ChatMessageList(
                                messages = state.messages,
                                isLoading = state.isLoading,
                                speakingMessageId = state.speakingMessageId,
                                listState = listState,
                                onToggleSaveNote = onToggleSaveNote,
                                onSpeakMessage = onSpeakMessage,
                                onQuickAction = onQuickAction,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    // Input Composer
                    WorkspaceInputBar(
                        prompt = state.prompt,
                        promptError = state.promptError,
                        enabled = !state.isLoading,
                        activeMode = state.activeMode,
                        selectedBitmap = state.selectedImageBitmap,
                        attachedDocName = state.attachedDocName,
                        onPromptChange = onPromptChange,
                        onSend = onSend,
                        onImageSelected = onImageSelected,
                        onClearImage = onClearImage,
                        onDocumentAttached = onDocumentAttached,
                        onClearDocument = onClearDocument,
                        onOpenModeSelector = { onShowModeDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ── Workspace Top Bar ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceTopBar(
    sessionTitle: String,
    activeMode: StudyMode,
    hasMessages: Boolean,
    onOpenDrawer: () -> Unit,
    onShowSavedNotes: () -> Unit,
    onShowInsights: () -> Unit,
    onOpenVoiceDialog: () -> Unit = {},
    onClearHistory: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.open_menu),
                )
            }
        },
        title = {
            Column {
                Text(
                    text = sessionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = "Mode: ${activeMode.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        actions = {
            IconButton(onClick = onOpenVoiceDialog) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = "Live Voice Mode",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(
                onClick = onShowInsights,
                modifier = Modifier.testTag(ChatTestTags.INSIGHTS_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = stringResource(R.string.study_insights),
                )
            }
            IconButton(
                onClick = onShowSavedNotes,
                modifier = Modifier.testTag(ChatTestTags.SAVED_NOTES_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = stringResource(R.string.saved_notes),
                )
            }
            if (hasMessages) {
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.clear_history),
                    )
                }
            }
        },
    )
}

// ── Mode & Quick Action Chips Header ─────────────────────────────────────────

@Composable
private fun ModeAndActionHeader(
    activeMode: StudyMode,
    onOpenModeSelector: () -> Unit,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = true,
            onClick = onOpenModeSelector,
            label = {
                Text(
                    text = "⚡ ${activeMode.badgeLabel}",
                    fontWeight = FontWeight.Bold,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = Modifier.testTag(ChatTestTags.MODE_BUTTON),
        )

        // Socratic / Academic Quick Prompt Chips for current mode
        for (prompt in activeMode.quickPrompts.take(3)) {
            AssistChip(
                onClick = { onQuickAction(prompt) },
                label = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

// ── Workspace Empty State ────────────────────────────────────────────────────

@Composable
private fun WorkspaceEmptyState(
    activeMode: StudyMode,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = activeMode.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = activeMode.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Suggested Study Prompts:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            for (prompt in activeMode.quickPrompts) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickAction(prompt) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "💡 $prompt",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// ── Message List ─────────────────────────────────────────────────────────────

@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    speakingMessageId: String?,
    listState: LazyListState,
    onToggleSaveNote: (ChatMessage) -> Unit,
    onSpeakMessage: (String, String) -> Unit,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag(ChatTestTags.MESSAGE_LIST),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            WorkspaceMessageBubble(
                message = message,
                isSpeaking = speakingMessageId == message.id,
                onToggleSaveNote = { onToggleSaveNote(message) },
                onSpeak = { onSpeakMessage(message.id, message.content) },
                onExplainSimpler = { onQuickAction("Explain simpler for a beginner:\n${message.content.take(150)}") },
            )
        }
        if (isLoading) {
            item(key = "loading_indicator") {
                WorkspaceLoadingBubble()
            }
        }
    }
}

// ── Workspace Message Bubble ─────────────────────────────────────────────────

@Composable
private fun WorkspaceMessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onToggleSaveNote: () -> Unit,
    onSpeak: () -> Unit,
    onExplainSimpler: () -> Unit,
) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        // Label with sender name and time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = if (isUser) stringResource(R.string.role_you) else stringResource(R.string.role_gemini),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = dateFormatter.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        // Bubble card
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.widthIn(min = 60.dp, max = 640.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Attached image thumbnail if present (multimodal)
                if (!message.imageUri.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "📷 Attached Study Image",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }

                // Message text / rich markdown
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    MarkdownViewer(
                        content = message.content,
                        textColor = MaterialTheme.colorScheme.onSurface,
                    )

                    // Gemini Action Row: Copy, Read Aloud, Save/Star, Simplify
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Copy Text
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy_text),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        // Read Aloud (TTS)
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Hearing else Icons.Default.VolumeUp,
                                contentDescription = stringResource(R.string.read_aloud),
                                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        // Save Note / Star
                        IconButton(
                            onClick = onToggleSaveNote,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (message.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = stringResource(R.string.save_note),
                                tint = if (message.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        // Explain Simpler
                        IconButton(
                            onClick = onExplainSimpler,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = stringResource(R.string.explain_simpler),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Workspace Loading Bubble ─────────────────────────────────────────────────

@Composable
private fun WorkspaceLoadingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ChatTestTags.LOADING_INDICATOR),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Workspace Input Bar ──────────────────────────────────────────────────────

@Composable
private fun WorkspaceInputBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    activeMode: StudyMode,
    selectedBitmap: Bitmap?,
    attachedDocName: String? = null,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageSelected: (Bitmap?, String?) -> Unit,
    onClearImage: () -> Unit,
    onDocumentAttached: (String, String) -> Unit = { _, _ -> },
    onClearDocument: () -> Unit = {},
    onOpenModeSelector: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onPromptChange(spoken)
            }
        }
    }

    // Photo / Image attachment launcher (multimodal)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = ImageUtils.loadAndResizeBitmap(context, uri, maxDimension = 1024)
            onImageSelected(bitmap, uri.toString())
        }
    }

    // Document picker launcher (notes, code, csv)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val processed = DocumentProcessorTool().processDocument(context, uri)
            if (processed != null) {
                onDocumentAttached(processed.fileName, processed.extractedText)
            }
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Document attachment preview banner
        if (attachedDocName != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = attachedDocName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = onClearDocument,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove document",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Image attachment preview banner
        if (selectedBitmap != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        bitmap = selectedBitmap.asImageBitmap(),
                        contentDescription = "Selected diagram",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diagram Attached",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Ready for Gemini image analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onClearImage) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove image")
                    }
                }
            }
        }

        // Input row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mode selector button
            IconButton(
                onClick = onOpenModeSelector,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = stringResource(R.string.select_mode),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // Image attachment button (multimodal)
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = enabled,
                modifier = Modifier.testTag(ChatTestTags.IMAGE_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.attach_image),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Document attachment button
            IconButton(
                onClick = {
                    docPickerLauncher.launch(
                        arrayOf(
                            "text/*",
                            "application/json",
                            "text/plain",
                            "text/csv",
                        )
                    )
                },
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach Document",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Voice input button
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Gemini...")
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (_: Exception) {
                        // Voice recognition gracefully unavailable
                    }
                },
                enabled = enabled,
                modifier = Modifier.testTag(ChatTestTags.VOICE_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.voice_input),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Text field
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .testTag(ChatTestTags.INPUT_FIELD),
                placeholder = {
                    Text(
                        text = if (selectedBitmap != null)
                            "Ask about this image…"
                        else
                            stringResource(R.string.enter_your_prompt_here),
                        maxLines = 1,
                    )
                },
                singleLine = false,
                maxLines = 4,
                enabled = enabled,
                isError = promptError != null,
                supportingText = promptError?.let {
                    { Text(stringResource(R.string.field_cannot_be_empty)) }
                },
                shape = RoundedCornerShape(24.dp),
            )

            // Send button
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && (prompt.isNotBlank() || selectedBitmap != null),
                modifier = Modifier.testTag(ChatTestTags.SEND_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun WorkspaceEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(),
            onPromptChange = {},
            onSend = {},
        )
    }
}
