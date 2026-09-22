package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme
import com.fahim.geminiApiComposeStarter.speech.SpeechRecognizerHelper
import com.fahim.geminiApiComposeStarter.ui.layout.WindowHeightSizeClass
import com.fahim.geminiApiComposeStarter.ui.layout.WindowSizeClass
import com.fahim.geminiApiComposeStarter.ui.layout.WindowWidthSizeClass
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onRetry = viewModel::onRetry,
        onDismissError = viewModel::onDismissError,
        onSpeechResult = viewModel::onSpeechResult,
        onSpeechError = viewModel::onSpeechError,
        onShowThemeDialog = viewModel::onShowThemeDialog,
        onSetTheme = viewModel::onSetTheme,
        onShowClearHistoryDialog = viewModel::onShowClearHistoryDialog,
        onClearHistory = viewModel::onClearHistory,
        windowSizeClass = windowSizeClass,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onSpeechResult: (String) -> Unit,
    onSpeechError: (String) -> Unit,
    onShowThemeDialog: (Boolean) -> Unit = {},
    onSetTheme: (AppTheme) -> Unit = {},
    onShowClearHistoryDialog: (Boolean) -> Unit = {},
    onClearHistory: () -> Unit = {},
    windowSizeClass: WindowSizeClass? = null,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)

    val speechPromptText = stringResource(R.string.speech_prompt)
    val permissionDeniedText = stringResource(R.string.speech_permission_denied)
    val notAvailableText = stringResource(R.string.speech_not_available)
    val emptyResultText = stringResource(R.string.speech_empty_result)
    val failedText = stringResource(R.string.speech_failed)

    // Activity result launcher for speech recognition intent
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val recognized = SpeechRecognizerHelper.extractSpeechResult(result.resultCode, result.data)
        if (!recognized.isNullOrBlank()) {
            onSpeechResult(recognized)
        } else if (result.resultCode == Activity.RESULT_OK) {
            onSpeechError(emptyResultText)
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            try {
                val intent = SpeechRecognizerHelper.createSpeechIntent(speechPromptText)
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                onSpeechError(notAvailableText)
            } catch (e: Exception) {
                onSpeechError(failedText)
            }
        } else {
            onSpeechError(permissionDeniedText)
        }
    }

    val onMicClick: () -> Unit = {
        if (!SpeechRecognizerHelper.isSpeechRecognitionAvailable(context)) {
            onSpeechError(notAvailableText)
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    val intent = SpeechRecognizerHelper.createSpeechIntent(speechPromptText)
                    speechLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    onSpeechError(notAvailableText)
                } catch (e: Exception) {
                    onSpeechError(failedText)
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (message != null) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (state.canRetry) retryLabel else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRetry()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding().testTag(ChatScreenTestTags.TAG_CHAT_SCREEN),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_assistant),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onShowThemeDialog(true) },
                        modifier = Modifier.testTag(ChatScreenTestTags.TAG_THEME_BUTTON),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.theme_preference),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { onShowClearHistoryDialog(true) },
                        modifier = Modifier.testTag(ChatScreenTestTags.TAG_CLEAR_BUTTON),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear_chat),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // Responsive container: constraint width on tablets/landscape, full width on compact phones
        val maxContentWidth = when (windowSizeClass?.widthSizeClass) {
            WindowWidthSizeClass.Expanded -> 840.dp
            WindowWidthSizeClass.Medium -> 680.dp
            else -> 1200.dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = maxContentWidth)
                    .padding(horizontal = 16.dp),
            ) {
                ConversationArea(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    state.errorMessage?.let { errorText ->
                        ErrorCard(
                            errorMessage = errorText,
                            canRetry = state.canRetry,
                            onRetry = onRetry,
                            onDismiss = onDismissError,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                    }
                }

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    isLoading = state.isLoading,
                    isCompactHeight = windowSizeClass?.heightSizeClass == WindowHeightSizeClass.Compact,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                )
            }
        }
    }

    if (state.isThemeDialogOpen) {
        ThemeSelectionDialog(
            currentTheme = state.theme,
            onThemeSelected = onSetTheme,
            onDismiss = { onShowThemeDialog(false) },
        )
    }

    if (state.isClearHistoryDialogOpen) {
        ClearHistoryConfirmDialog(
            onConfirm = onClearHistory,
            onDismiss = { onShowClearHistoryDialog(false) },
        )
    }
}

@Composable
private fun ConversationArea(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        val totalCount = messages.size + if (isLoading) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    if (messages.isEmpty() && !isLoading) {
        EmptyConversationView(modifier = modifier.testTag(ChatScreenTestTags.TAG_EMPTY_VIEW))
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.testTag(ChatScreenTestTags.TAG_CONVERSATION_LIST),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = messages,
                key = { it.id },
            ) { message ->
                ChatBubble(message = message)
            }

            if (isLoading) {
                item(key = "loading_indicator_bubble") {
                    LoadingBubble()
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.participant == Participant.USER

    val bubbleAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = bubbleAlignment,
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = if (isUser) 1.dp else 2.dp,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(0.85f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    if (!isUser) {
                        Icon(
                            painter = painterResource(R.drawable.ic_assistant),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isUser) stringResource(R.string.user_label) else stringResource(R.string.gemini_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
                Text(
                    text = message.text.toBoldAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Box(
        modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.TAG_LOADING_INDICATOR),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyConversationView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_conversation_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_conversation_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = modifier.testTag(ChatScreenTestTags.TAG_ERROR_CARD),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.dismiss),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (canRetry) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    isLoading: Boolean,
    isCompactHeight: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.TAG_INPUT_FIELD),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = if (isCompactHeight) 1 else 2,
            maxLines = 4,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )
        FilledTonalIconButton(
            onClick = onMicClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp).testTag(ChatScreenTestTags.TAG_MIC_BUTTON),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.voice_input),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        FilledIconButton(
            onClick = onSend,
            enabled = enabled && prompt.isNotBlank(),
            modifier = Modifier.size(48.dp).testTag(ChatScreenTestTags.TAG_SEND_BUTTON),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
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
                    ChatMessage(text = "Explain quantum computing in simple terms.", participant = Participant.USER),
                    ChatMessage(text = "**Quantum computing** uses qubits instead of classical bits to process complex calculations!", participant = Participant.MODEL),
                ),
            ),
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onDismissError = {},
            onSpeechResult = {},
            onSpeechError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenDarkPreview() {
    GeminiApiComposeStarterTheme(darkTheme = true) {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(text = "Explain quantum computing in simple terms.", participant = Participant.USER),
                    ChatMessage(text = "**Quantum computing** uses qubits instead of classical bits to process complex calculations!", participant = Participant.MODEL),
                ),
                isLoading = true,
            ),
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onDismissError = {},
            onSpeechResult = {},
            onSpeechError = {},
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_theme)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                val themes = listOf(
                    AppTheme.SYSTEM to stringResource(R.string.theme_system),
                    AppTheme.LIGHT to stringResource(R.string.theme_light),
                    AppTheme.DARK to stringResource(R.string.theme_dark),
                )
                themes.forEach { (theme, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (theme == currentTheme),
                                onClick = { onThemeSelected(theme) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        },
    )
}

@Composable
private fun ClearHistoryConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_chat_title)) },
        text = { Text(stringResource(R.string.clear_chat_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

object ChatScreenTestTags {
    const val TAG_CHAT_SCREEN = "tag_chat_screen"
    const val TAG_INPUT_FIELD = "tag_input_field"
    const val TAG_SEND_BUTTON = "tag_send_button"
    const val TAG_MIC_BUTTON = "tag_mic_button"
    const val TAG_LOADING_INDICATOR = "tag_loading_indicator"
    const val TAG_ERROR_CARD = "tag_error_card"
    const val TAG_CONVERSATION_LIST = "tag_conversation_list"
    const val TAG_EMPTY_VIEW = "tag_empty_view"
    const val TAG_THEME_BUTTON = "tag_theme_button"
    const val TAG_CLEAR_BUTTON = "tag_clear_button"
}

