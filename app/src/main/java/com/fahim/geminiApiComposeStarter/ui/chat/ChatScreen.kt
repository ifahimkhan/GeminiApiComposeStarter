package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageDeliveryStatus
import com.fahim.geminiApiComposeStarter.data.local.room.SenderRole
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceInputResult = viewModel::onVoiceInputResult,
        onRetry = viewModel::onRetry,
        onDismissError = viewModel::onDismissError,
        onClearChat = viewModel::onClearChat
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInputResult: (String) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onClearChat: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Adaptive bubble width based on screen width
    val isTabletOrLandscape = configuration.screenWidthDp > 600
    val maxBubbleWidthFactor = if (isTabletOrLandscape) 0.65f else 0.85f

    // Voice recognition launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onVoiceInputResult(spokenText)
            }
        }
    }

    // Auto-scroll to the bottom when new messages arrive or loading begins
    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Show error in Snackbar if needed
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Vivaan Shrivastava • N157",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Project Info"
                        )
                    }
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_chat),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main conversation area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    EmptyChatState(
                        onSelectPrompt = onPromptChange,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                maxBubbleWidthFactor = maxBubbleWidthFactor
                            )
                        }

                        if (state.isLoading) {
                            item(key = "loading_indicator_item") {
                                LoadingBubble(maxBubbleWidthFactor = maxBubbleWidthFactor)
                            }
                        }
                    }
                }
            }

            // Inline error notification card with retry button
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut()
            ) {
                state.errorMessage?.let { error ->
                    ErrorCard(
                        errorMessage = error,
                        canRetry = state.lastFailedPrompt != null,
                        onRetry = onRetry,
                        onDismiss = onDismissError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Chat input bar
            ChatInputBar(
                prompt = state.prompt,
                promptError = state.promptError,
                isLoading = state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
                        }
                        speechRecognizerLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.speech_not_available),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }

    // Confirmation dialog for clearing chat
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = stringResource(R.string.clear_chat_title)) },
            text = { Text(text = stringResource(R.string.clear_chat_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearChat()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    // Project Info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About Assignment 1") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Student: Vivaan Shrivastava", fontWeight = FontWeight.Bold)
                    Text("• Roll Number: N157", fontWeight = FontWeight.Bold)
                    Text("• Subject: Mobile Application Development")
                    Text("• University: SVKM's NMIMS MPSTME")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Technical Architecture:", fontWeight = FontWeight.SemiBold)
                    Text("• Jetpack Compose & Material 3")
                    Text("• Google Generative AI (Gemini)")
                    Text("• Android Keystore AES-256-GCM")
                    Text("• Persistent Room Chat Database")
                    Text("• Preferences DataStore for Settings")
                    Text("• Speech-to-Text Voice Input")
                    Text("• Responsive Window Size Layouts")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    maxBubbleWidthFactor: Float,
    modifier: Modifier = Modifier
) {
    val isUser = message.senderRole == SenderRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 18.dp
        )
    }

    val containerColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        message.status == MessageDeliveryStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        message.status == MessageDeliveryStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(maxBubbleWidthFactor)
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(bottom = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Gemini",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = bubbleShape,
                color = containerColor,
                tonalElevation = 2.dp,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = message.content.toBoldAnnotatedString(),
                            color = contentColor,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        if (!isUser && message.status != MessageDeliveryStatus.ERROR) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    copied = true
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(bottom = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "User",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBubble(maxBubbleWidthFactor: Float, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth(maxBubbleWidthFactor)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.padding(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.gemini_thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatState(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePrompts = listOf(
        "Explain Jetpack Compose state hoisting",
        "How does Android Keystore protect API keys?",
        "Write a Kotlin Coroutine with Flow example",
        "Compare Room database vs Preferences DataStore"
    )

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome, Vivaan!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ask any question to get started with Gemini AI.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.try_asking),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            samplePrompts.forEach { prompt ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectPrompt(prompt) },
                    label = {
                        Text(
                            text = prompt,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (canRetry) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.retry),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dismiss),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    prompt: String,
    promptError: PromptError?,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onVoiceClick,
                enabled = !isLoading,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.voice_input)
                )
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.enter_your_prompt_here),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                maxLines = 4,
                isError = promptError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent
                )
            )

            FilledIconButton(
                onClick = onSend,
                enabled = !isLoading && prompt.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessageEntity(
                        id = 1,
                        senderRole = SenderRole.USER,
                        content = "Explain Android Keystore with AES-256-GCM"
                    ),
                    ChatMessageEntity(
                        id = 2,
                        senderRole = SenderRole.ASSISTANT,
                        content = "**Android Keystore** provides hardware-backed cryptographic key isolation. With **AES-256-GCM**, data is encrypted and authenticated."
                    )
                )
            ),
            onPromptChange = {},
            onSend = {},
            onVoiceInputResult = {},
            onRetry = {},
            onDismissError = {},
            onClearChat = {}
        )
    }
}
