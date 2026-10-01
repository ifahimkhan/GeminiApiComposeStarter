package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceResult = viewModel::onVoiceInputResult,
        onModelSelected = viewModel::onModelSelected,
        onClearHistory = viewModel::onClearHistory,
        onDismissError = viewModel::onDismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Speech-to-text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onVoiceResult(spokenText)
            }
        }
    }

    // Launch speech recognizer safely
    val startVoiceInput: () -> Unit = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    context.getString(R.string.voice_input),
                )
            }
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.voice_not_supported))
            }
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Voice input error: ${e.message}")
            }
        }
    }

    // Trigger Snackbar on error state
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onDismissError()
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.selectedModel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("menu_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.select_model),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        state.availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    val isSelected = model == state.selectedModel
                                    Text(
                                        text = if (isSelected) "✓ $model" else "   $model",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onModelSelected(model)
                                    menuExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_history)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onClearHistory()
                                menuExpanded = false
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // Responsive container: centers and constrains chat content on wider displays/tablets
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val maxWidthConstraint = if (maxWidth > 720.dp) 680.dp else maxWidth

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = maxWidthConstraint)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    // Chat messages list
                    MessageList(
                        messages = state.messages,
                        isLoading = state.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )

                    // Bottom prompt input bar
                    PromptBar(
                        prompt = state.prompt,
                        promptError = state.promptError,
                        enabled = !state.isLoading,
                        onPromptChange = onPromptChange,
                        onSend = onSend,
                        onVoiceClick = startVoiceInput,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Automatically scroll to the newest message whenever message count or loading state changes
    LaunchedEffect(messages.size, isLoading) {
        val targetIndex = if (isLoading) messages.size else (messages.size - 1)
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    if (messages.isEmpty() && !isLoading) {
        EmptyChatPlaceholder(modifier = modifier)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.testTag("messages_list"),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = messages,
                key = { message -> message.id },
            ) { message ->
                MessageBubble(message = message)
            }

            if (isLoading) {
                item(key = "loading_indicator_item") {
                    GeminiGeneratingBubble()
                }
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.welcome_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.isUser

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = stringResource(R.string.gemini_label),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = if (isUser) {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 2.dp,
                )
            } else {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 2.dp,
                    bottomEnd = 16.dp,
                )
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .testTag(if (isUser) "user_bubble" else "gemini_bubble"),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = if (isUser) stringResource(R.string.user_label) else stringResource(R.string.gemini_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = if (isUser) {
                        message.text.toBoldAnnotatedString()
                    } else {
                        message.text.toBoldAnnotatedString()
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                )
            }
        }
    }
}

@Composable
private fun GeminiGeneratingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.testTag("loading_indicator"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini is thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_input"),
                placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
                maxLines = 4,
                enabled = enabled,
                isError = promptError != null,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Speech-to-text button
            IconButton(
                onClick = onVoiceClick,
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .testTag("voice_button"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.voice_input),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send button
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && prompt.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = promptError != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.field_cannot_be_empty),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
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
                    ChatMessage(id = 1, text = "Hello Gemini! How are you?", isUser = true),
                    ChatMessage(id = 2, text = "Hello! I am **Gemini**, ready to help you.", isUser = false),
                ),
            ),
            onPromptChange = {},
            onSend = {},
            onVoiceResult = {},
            onModelSelected = {},
            onClearHistory = {},
            onDismissError = {},
        )
    }
}
