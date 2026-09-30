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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Voice recognition launcher via RecognizerIntent
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            spokenText?.let { viewModel.onVoiceInput(it) }
        }
    }

    val onLaunchVoice = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    context.getString(R.string.enter_your_prompt_here)
                )
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onClearHistory = viewModel::onClearHistory,
        onVoiceClick = onLaunchVoice,
        onSelectSuggestion = viewModel::onSelectPromptSuggestion,
        onToggleSettings = viewModel::onToggleSettings,
        onUpdatePreferences = viewModel::onUpdatePreferences,
        onSaveApiKey = viewModel::onSaveCustomApiKey,
        onDismissError = viewModel::onDismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearHistory: () -> Unit,
    onVoiceClick: () -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onToggleSettings: (Boolean) -> Unit,
    onUpdatePreferences: (userName: String, model: String, tone: String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onDismissError: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onDismissError()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = state.selectedModel.removePrefix("gemini-"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = "Hello, ${state.userName} • ${state.tone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_history),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { onToggleSettings(true) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Adaptive max-width container for tablet / landscape responsiveness
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 16.dp),
            ) {
                // Conversation Area (LazyColumn with auto-scroll)
                ConversationList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    onSelectSuggestion = onSelectSuggestion,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                // Input Bar (Text + Voice + Send)
                ChatInputBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    isLoading = state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceClick = onVoiceClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (state.isSettingsOpen) {
        SettingsDialog(
            currentName = state.userName,
            currentModel = state.selectedModel,
            currentTone = state.tone,
            onDismiss = { onToggleSettings(false) },
            onSave = { name, model, tone, customKey ->
                onUpdatePreferences(name, model, tone)
                if (customKey.isNotBlank()) {
                    onSaveApiKey(customKey)
                }
                onToggleSettings(false)
            }
        )
    }
}

@Composable
private fun ConversationList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to latest message on new messages or loading trigger
    LaunchedEffect(messages.size, isLoading) {
        val targetIndex = if (isLoading) messages.size else (messages.size - 1)
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    if (messages.isEmpty() && !isLoading) {
        WelcomeSuggestionsCard(
            onSelectSuggestion = onSelectSuggestion,
            modifier = modifier,
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                MessageBubbleItem(message = message)
            }

            if (isLoading) {
                item(key = "loading_indicator") {
                    LoadingBubbleItem()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeSuggestionsCard(
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp),
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
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val suggestions = listOf(
                stringResource(R.string.chip_1),
                stringResource(R.string.chip_2),
                stringResource(R.string.chip_3),
                stringResource(R.string.chip_4),
            )
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSelectSuggestion(suggestion) },
                    label = { Text(suggestion) },
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun MessageBubbleItem(message: ChatMessage) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(if (message.isUser) 1f else 0.95f),
        ) {
            if (!message.isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 4.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = bubbleShape,
                color = containerColor,
                tonalElevation = 2.dp,
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text.toBoldAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                        lineHeight = 22.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = message.formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f),
                        )
                        if (!message.isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy),
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(message.text))
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.copied_to_clipboard),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            )
                        }
                    }
                }
            }

            if (message.isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 4.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBubbleItem() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Voice Input Button
            IconButton(
                onClick = onVoiceClick,
                enabled = !isLoading,
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.voice_input),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // Prompt text field
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
                maxLines = 4,
                enabled = !isLoading,
                isError = promptError != null,
                supportingText = promptError?.let {
                    { Text(stringResource(R.string.field_cannot_be_empty)) }
                },
                shape = RoundedCornerShape(20.dp),
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            FilledIconButton(
                onClick = onSend,
                enabled = !isLoading && prompt.trim().isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    currentName: String,
    currentModel: String,
    currentTone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, model: String, tone: String, customKey: String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var selectedTone by remember { mutableStateOf(currentTone) }
    var customKey by remember { mutableStateOf("") }

    val models = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash")
    val tones = listOf("Balanced", "Concise", "Creative")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.user_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.model_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    models.forEach { model ->
                        FilterChip(
                            selected = (selectedModel == model),
                            onClick = { selectedModel = model },
                            label = { Text(model.removePrefix("gemini-"), fontSize = 11.sp) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.tone_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tones.forEach { tone ->
                        FilterChip(
                            selected = (selectedTone == tone),
                            onClick = { selectedTone = tone },
                            label = { Text(tone, fontSize = 12.sp) },
                        )
                    }
                }

                OutlinedTextField(
                    value = customKey,
                    onValueChange = { customKey = it },
                    label = { Text(stringResource(R.string.custom_api_key_label)) },
                    placeholder = { Text(stringResource(R.string.api_key_placeholder), fontSize = 12.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, selectedModel, selectedTone, customKey) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
