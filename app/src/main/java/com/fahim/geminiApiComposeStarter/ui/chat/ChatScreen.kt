package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.theme.ModelBubbleDark
import com.fahim.geminiApiComposeStarter.ui.theme.ModelBubbleLight
import com.fahim.geminiApiComposeStarter.ui.theme.UserBubbleDark
import com.fahim.geminiApiComposeStarter.ui.theme.UserBubbleLight
import kotlinx.coroutines.launch
import java.util.Locale

private val AVAILABLE_MODELS = listOf(
    "gemini-1.5-flash",
    "gemini-2.5-flash",
    "gemini-3.6-flash",
    "gemini-1.5-pro"
)

@Composable
private fun ambientGradientBrush(isDark: Boolean): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B132B),
                Color(0xFF0F2038),
                Color(0xFF0A2E3D),
                Color(0xFF050B14)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE0F2FE),
                Color(0xFFBAE6FD),
                Color(0xFFE0F2FE),
                Color(0xFFF0F9FF)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScopeCompat()
    val context = LocalContext.current

    // Resolve dark mode based on user's ThemeMode preference
    val resolvedIsDark = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onVoiceResult(spoken)
            } else {
                viewModel.onListeningCancelled()
            }
        } else {
            viewModel.onListeningCancelled()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.dismissError()
            }
        }
    }

    // Clear Chat Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Chat History") },
            text = { Text("Are you sure you want to delete all messages?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // API Key Notice Popup Dialog
    uiState.modelNoticeDialogMessage?.let { noticeMessage ->
        AlertDialog(
            onDismissRequest = viewModel::dismissModelNotice,
            title = { Text("API Key Notice") },
            text = { Text(noticeMessage) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissModelNotice) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Gemini",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // Model Selector Chip & Menu
                    Box {
                        AssistChip(
                            onClick = { showModelMenu = true },
                            label = { Text(uiState.selectedModel) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Model",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            AVAILABLE_MODELS.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        showModelMenu = false
                                        viewModel.selectModel(model)
                                    }
                                )
                            }
                        }
                    }

                    // Clear Chat Button
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat"
                        )
                    }

                    // Theme Mode Selector Menu
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Default") },
                                onClick = {
                                    showThemeMenu = false
                                    viewModel.selectThemeMode(ThemeMode.SYSTEM)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Light Mode") },
                                onClick = {
                                    showThemeMenu = false
                                    viewModel.selectThemeMode(ThemeMode.LIGHT)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark Mode") },
                                onClick = {
                                    showThemeMenu = false
                                    viewModel.selectThemeMode(ThemeMode.DARK)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    action = {
                        TextButton(onClick = { data.dismiss() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                ) { Text(data.visuals.message) }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientGradientBrush(resolvedIsDark))
                .padding(paddingValues)
        ) {
            ChatScreenContent(
                uiState = uiState,
                isDark = resolvedIsDark,
                isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact,
                onInputChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onMicClick = {
                    viewModel.onListeningStarted()
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.hint_message))
                    }
                    runCatching { voiceLauncher.launch(intent) }
                        .onFailure { viewModel.onListeningCancelled() }
                }
            )
        }
    }
}

/**
 * Fully stateless chat content composable.
 */
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    isDark: Boolean = false,
    isCompactWidth: Boolean = true,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onSelectModel: (String) -> Unit = {},
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (uiState.messages.isEmpty()) {
        EmptyChatStartView(
            uiState = uiState,
            isDark = isDark,
            isCompactWidth = isCompactWidth,
            onInputChanged = onInputChanged,
            onSend = onSend,
            onMicClick = onMicClick,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            MessageList(
                messages = uiState.messages,
                isDark = isDark,
                isSending = uiState.isSending,
                modifier = Modifier.weight(1f),
                horizontalPadding = if (isCompactWidth) 12.dp else 64.dp
            )
            MessageInputBar(
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                isListening = uiState.isListening,
                onInputChanged = onInputChanged,
                onSend = onSend,
                onMicClick = onMicClick,
                horizontalPadding = if (isCompactWidth) 8.dp else 48.dp
            )
        }
    }
}

/**
 * Centered empty state UI ("What do you want to ask today, [Name]?").
 */
@Composable
private fun EmptyChatStartView(
    uiState: ChatUiState,
    isDark: Boolean,
    isCompactWidth: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nameGreeting = if (uiState.userName.isNotBlank()) ", ${uiState.userName}" else ""
    val horizontalPadding = if (isCompactWidth) 20.dp else 80.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Gemini",
                tint = Color.Unspecified,
                modifier = Modifier.size(if (isCompactWidth) 30.dp else 36.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "What do you want to ask today$nameGreeting?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = if (isCompactWidth) 26.sp else 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Start,
                color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1A1C2A)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Floating Pill Search/Input Bar
        Surface(
            shape = CircleShape,
            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    else Color.White.copy(alpha = 0.9f),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add attachment",
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black.copy(alpha = 0.6f)
                    )
                }

                TextField(
                    value = uiState.inputText,
                    onValueChange = onInputChanged,
                    placeholder = {
                        Text(
                            "Ask Gemini...",
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant
                                    else Color.Black.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(visible = uiState.isListening) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }

                IconButton(onClick = onMicClick, enabled = !uiState.isListening) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.content_desc_mic),
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black.copy(alpha = 0.6f)
                    )
                }

                IconButton(onClick = onSend, enabled = uiState.inputText.isNotBlank() && !uiState.isSending) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.content_desc_send),
                        tint = if (uiState.inputText.isNotBlank() && !uiState.isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isDark: Boolean,
    isSending: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = messages, key = { it.id }) { message ->
            ChatBubble(message, isDark)
        }
        if (isSending) {
            item(key = "typing-indicator") { TypingIndicator() }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isDark: Boolean) {
    val isUser = message.isFromUser
    val bubbleColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        isUser -> if (isDark) UserBubbleDark.copy(alpha = 0.9f) else UserBubbleLight.copy(alpha = 0.9f)
        else -> if (isDark) ModelBubbleDark.copy(alpha = 0.85f) else ModelBubbleLight.copy(alpha = 0.85f)
    }
    val contentColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> if (isDark) Color.White else Color.Black
        else -> if (isDark) Color.White else Color.Black
    }
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = bubbleColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Gemini is thinking…", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    inputText: String,
    isSending: Boolean,
    isListening: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.hint_message)) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))

            AnimatedVisibility(visible = isListening) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                IconButton(onClick = onMicClick, enabled = !isListening) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.content_desc_mic),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (inputText.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                IconButton(onClick = onSend, enabled = inputText.isNotBlank() && !isSending) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.content_desc_send),
                        tint = if (inputText.isNotBlank() && !isSending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// isSystemDark() removed — isDark is now passed explicitly to respect ThemeMode

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
