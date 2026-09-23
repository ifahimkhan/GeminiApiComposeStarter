package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.local.MessageRole
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
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
        onSpeechResult = viewModel::onSpeechResult,
        onClearChat = viewModel::onClearChat,
        onSelectModel = viewModel::onSelectModel,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDismissError = viewModel::onDismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onSpeechResult: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
    onSelectModel: (String) -> Unit = {},
    onThemeModeChange: (String) -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    // Auto-scroll to the bottom whenever a new message is added or loading begins
    val messageCount = state.messages.size
    LaunchedEffect(messageCount, state.isLoading) {
        if (messageCount > 0) {
            val targetIndex = if (state.isLoading) messageCount else messageCount - 1
            listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_chat)) },
            text = { Text(stringResource(R.string.clear_chat_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearChat()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            ChatTopBar(
                selectedModel = state.selectedModel,
                availableModels = state.availableModels,
                themeMode = state.themeMode,
                onSelectModel = onSelectModel,
                onThemeModeChange = onThemeModeChange,
                onClearClick = { showClearDialog = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // Responsive container: max width 840dp centered horizontally on tablets and large screens
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
            ) {
                if (state.messages.isEmpty()) {
                    EmptyChatState(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        onSuggestionClick = { suggestion ->
                            onPromptChange(suggestion)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.messages,
                            key = { it.id }
                        ) { message ->
                            ChatBubbleItem(message = message)
                        }

                        if (state.isLoading) {
                            item(key = "loading_indicator") {
                                LoadingBubbleItem()
                            }
                        }
                    }
                }

                PromptInputBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    isLoading = state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onSpeechResult = onSpeechResult,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    selectedModel: String,
    availableModels: List<String>,
    themeMode: String,
    onSelectModel: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onClearClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.select_model),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        onSelectModel(model)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            // Theme Mode Selector
            Box {
                IconButton(onClick = { themeMenuExpanded = true }) {
                    val (icon, description) = when (themeMode) {
                        "DARK" -> Icons.Default.DarkMode to "Dark mode"
                        "LIGHT" -> Icons.Default.LightMode to "Light mode"
                        else -> Icons.Default.BrightnessAuto to "System theme"
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System default") },
                        leadingIcon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null)
                        },
                        onClick = {
                            onThemeModeChange("SYSTEM")
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light mode") },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null)
                        },
                        onClick = {
                            onThemeModeChange("LIGHT")
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark mode") },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                        },
                        onClick = {
                            onThemeModeChange("DARK")
                            themeMenuExpanded = false
                        }
                    )
                }
            }

            IconButton(onClick = onClearClick) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.clear_chat),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    )
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.response_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val suggestions = listOf(
            "Explain quantum computing in simple terms",
            "Write a Kotlin function for binary search",
            "What are the benefits of Jetpack Compose?",
        )
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion, maxLines = 1) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant),
                        contentDescription = stringResource(R.string.gemini),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(
                    containerColor = bubbleColor,
                    contentColor = contentColor,
                ),
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (message.isError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = if (isUser) {
                            androidx.compose.ui.text.AnnotatedString(message.content)
                        } else {
                            message.content.toBoldAnnotatedString()
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    )
                }
            }
        }

        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(
                start = if (isUser) 0.dp else 40.dp,
                end = if (isUser) 4.dp else 0.dp,
                top = 2.dp,
            ),
        )
    }
}

@Composable
private fun LoadingBubbleItem() {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = stringResource(R.string.gemini),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini is thinking…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PromptInputBar(
    prompt: String,
    promptError: PromptError?,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onSpeechResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onSpeechResult(spokenText)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
            shape = RoundedCornerShape(24.dp),
            enabled = !isLoading,
            isError = promptError != null,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Speech-to-text Voice Input button
        IconButton(
            onClick = {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
                    }
                    speechLauncher.launch(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.speech_not_available),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            enabled = !isLoading,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.voice_input),
            )
        }

        // Send button
        FilledIconButton(
            onClick = onSend,
            enabled = !isLoading && prompt.isNotBlank(),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
