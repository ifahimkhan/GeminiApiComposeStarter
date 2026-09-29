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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onVoiceResult(spokenText)
            }
        }
    }

    ChatScreen(
        uiState = uiState,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                viewModel.onVoiceError("Speech recognition isn't available on this device.")
            } catch (e: Exception) {
                viewModel.onVoiceError("Speech recognition isn't available on this device.")
            }
        },
        onRetry = viewModel::retry,
        onDismissError = viewModel::clearErrorMessage,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onPromptChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRetry()
            }
            onDismissError()
        }
    }

    val messageCount = uiState.messages.size
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    // Adaptive sizing parameters for compact, medium, and expanded windows
    val (maxContainerWidth, horizontalPadding, bubbleMaxWidth) = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> Triple(Dp.Unspecified, 16.dp, 300.dp)
        WindowWidthSizeClass.Medium -> Triple(640.dp, 24.dp, 440.dp)
        else -> Triple(800.dp, 32.dp, 540.dp)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (maxContainerWidth != Dp.Unspecified) Modifier.widthIn(max = maxContainerWidth)
                                else Modifier
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_assistant),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini AI",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val containerModifier = if (maxContainerWidth != Dp.Unspecified) {
                Modifier
                    .widthIn(max = maxContainerWidth)
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            }

            Column(
                modifier = containerModifier.fillMaxSize(),
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    EmptyChatGreeting(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id },
                        ) { message ->
                            ChatMessageBubble(
                                message = message,
                                maxBubbleWidth = bubbleMaxWidth,
                            )
                        }

                        if (uiState.isLoading) {
                            item(key = "loading_indicator") {
                                AssistantLoadingBubble()
                            }
                        }
                    }
                }

                ChatInputBar(
                    prompt = uiState.prompt,
                    promptError = uiState.promptError,
                    isLoading = uiState.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceClick = onVoiceClick,
                    horizontalPadding = horizontalPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to Gemini",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ask a question, generate ideas, or explore concepts with AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    maxBubbleWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val isUser = message.participant == Participant.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 2.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant),
                        contentDescription = "Assistant",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }

        val bubbleColor = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

        val textColor = if (isUser) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isUser) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = message.text.toBoldAnnotatedString(),
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun AssistantLoadingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini is thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp)) {
            AnimatedVisibility(
                visible = promptError == PromptError.EMPTY,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "Prompt cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(text = "Ask Gemini...")
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    maxLines = 4,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (prompt.isNotBlank() && !isLoading) {
                                onSend()
                            }
                        },
                    ),
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onVoiceClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "Voice input",
                        modifier = Modifier.size(24.dp),
                        tint = if (!isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val isSendEnabled = prompt.isNotBlank() && !isLoading
                IconButton(
                    onClick = onSend,
                    enabled = isSendEnabled,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSendEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    )
                }
            }
        }
    }
}

@Preview(name = "Compact Phone Preview", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun ChatScreenCompactPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        text = "Can you explain quantum computing in simple terms?",
                        participant = Participant.USER,
                    ),
                    ChatMessage(
                        id = "2",
                        text = "Quantum computing uses **qubits** instead of classical bits to process complex calculations exponentially faster.",
                        participant = Participant.ASSISTANT,
                    ),
                ),
                prompt = "Tell me more",
            ),
            widthSizeClass = WindowWidthSizeClass.Compact,
        )
    }
}

@Preview(name = "Medium Tablet / Landscape Preview", widthDp = 700, heightDp = 500, showBackground = true)
@Composable
private fun ChatScreenMediumPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        text = "Show me an example in Kotlin.",
                        participant = Participant.USER,
                    ),
                    ChatMessage(
                        id = "2",
                        text = "Here is an example using **StateFlow** in Kotlin coroutines.",
                        participant = Participant.ASSISTANT,
                    ),
                ),
            ),
            widthSizeClass = WindowWidthSizeClass.Medium,
        )
    }
}

@Preview(name = "Expanded Tablet / Foldable Preview", widthDp = 1000, heightDp = 800, showBackground = true)
@Composable
private fun ChatScreenExpandedPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        text = "What are the benefits of adaptive layouts?",
                        participant = Participant.USER,
                    ),
                    ChatMessage(
                        id = "2",
                        text = "Adaptive layouts ensure that apps look stunning and remain ergonomically usable across phones, foldables, tablets, and desktop displays.",
                        participant = Participant.ASSISTANT,
                    ),
                ),
            ),
            widthSizeClass = WindowWidthSizeClass.Expanded,
        )
    }
}
