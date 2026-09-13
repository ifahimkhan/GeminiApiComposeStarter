package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState
import kotlinx.coroutines.launch
import java.util.Locale

private const val EMPTY_CHAT_MESSAGE = "What should we explore?"
private const val PROMPT_PLACEHOLDER = "Ask anything"
private const val EMPTY_FIELD_ERROR = "Field cannot be empty"
private const val SEND_DESCRIPTION = "Send"
private const val MENU_DESCRIPTION = "Open menu"
private const val VOICE_INPUT_DESCRIPTION = "Use voice input"
private const val LIGHT_MODE_DESCRIPTION = "Switch to light mode"
private const val DARK_MODE_DESCRIPTION = "Switch to dark mode"
private const val NEW_CHAT_LABEL = "New chat"
private val SendBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val window = calculateWindowSizeClass(context as Activity)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(viewModel::onPromptChange)
        }
    }
    ChatScreen(
        state = state,
        widthClass = window.widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceInput = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                .putExtra(RecognizerIntent.EXTRA_PROMPT, PROMPT_PLACEHOLDER)
            try {
                speechLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                viewModel.showError("Speech recognition is not available on this device.")
            }
        },
        onNewChat = viewModel::onNewChat,
        onSelectChat = viewModel::onSelectChat,
        darkTheme = darkTheme,
        onToggleTheme = onToggleTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                chats = state.chatSummaries,
                activeChatId = state.activeChatId,
                onNewChat = {
                    onNewChat()
                    scope.launch { drawerState.close() }
                },
                onSelectChat = { chatId ->
                    onSelectChat(chatId)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ChatTopBar(
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { innerPadding ->
            ChatContent(
                state = state,
                widthClass = widthClass,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceInput = onVoiceInput,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun ChatDrawer(
    chats: List<ChatSummary>,
    activeChatId: String,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(
                onClick = onNewChat,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(NEW_CHAT_LABEL)
            }
            Spacer(modifier = Modifier.height(12.dp))
            chats.forEach { chat ->
                NavigationDrawerItem(
                    label = { Text(chat.title) },
                    selected = chat.id == activeChatId,
                    onClick = { onSelectChat(chat.id) },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = MENU_DESCRIPTION,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (darkTheme) LIGHT_MODE_DESCRIPTION else DARK_MODE_DESCRIPTION,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatContent(
    state: ChatUiState,
    widthClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val sidePad = when (widthClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 32.dp
        else -> 48.dp
    }
    val maxWidth = when (widthClass) {
        WindowWidthSizeClass.Compact -> 560.dp
        WindowWidthSizeClass.Medium -> 720.dp
        else -> 840.dp
    }
    val showLoader = state.isLoading && state.messages.lastOrNull()?.author != ChatAuthor.GEMINI
    val extraItems = when {
        showLoader && state.errorMessage != null -> 2
        showLoader || state.errorMessage != null -> 1
        state.messages.isEmpty() -> 1
        else -> 0
    }

    LaunchedEffect(state.messages.size, showLoader, state.errorMessage) {
        val count = state.messages.size + extraItems
        if (count > 0) listState.scrollToItem(count - 1)
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth),
        ) {
            if (state.messages.isEmpty() && !state.isLoading && state.errorMessage == null) {
                EmptyChatMessage(
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
                    contentPadding = PaddingValues(
                        horizontal = sidePad,
                        vertical = 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(
                        items = state.messages,
                        key = { message -> message.id },
                    ) { message ->
                        ChatBubble(
                            message = message,
                            widthClass = widthClass,
                        )
                    }
                    if (showLoader) {
                        item(key = "loading") { LoadingBubble() }
                    }
                    state.errorMessage?.let { message ->
                        item(key = "error") { ErrorCard(message = message) }
                    }
                }
            }

            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                sidePad = sidePad,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceInput = onVoiceInput,
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    widthClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val isUser = message.author == ChatAuthor.USER
    val maxUserWidth = when (widthClass) {
        WindowWidthSizeClass.Compact -> 280.dp
        WindowWidthSizeClass.Medium -> 420.dp
        else -> 520.dp
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = maxUserWidth),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            GeminiMarkdown(content = message.text)
        }
    }
}

@Composable
private fun GeminiMarkdown(content: String, modifier: Modifier = Modifier) {
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = 0.1.sp,
    )
    val markdownState = rememberMarkdownState(content = content, retainState = true)

    Markdown(
        markdownState = markdownState,
        modifier = modifier.fillMaxWidth(),
        padding = markdownPadding(
            block = 8.dp,
            list = 6.dp,
            listItemTop = 3.dp,
            listItemBottom = 3.dp,
            listIndent = 14.dp,
            codeBlock = PaddingValues(12.dp),
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            h2 = MaterialTheme.typography.titleMedium.copy(
                fontSize = 19.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            h3 = MaterialTheme.typography.titleSmall.copy(
                fontSize = 17.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            h4 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            h5 = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            h6 = MaterialTheme.typography.bodySmall.copy(
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            text = bodyStyle,
            paragraph = bodyStyle,
            ordered = bodyStyle,
            bullet = bodyStyle,
            list = bodyStyle,
            code = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
            ),
            inlineCode = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            table = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
            ),
        ),
    )
}

@Composable
private fun EmptyChatMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = EMPTY_CHAT_MESSAGE,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun LoadingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
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
private fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Could not get a response",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    sidePad: Dp,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = sidePad, end = sidePad, top = 12.dp, bottom = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = PROMPT_PLACEHOLDER,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 1,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    enabled = enabled,
                    isError = promptError != null,
                    supportingText = promptError?.let { { Text(EMPTY_FIELD_ERROR) } },
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent,
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onVoiceInput,
                        enabled = enabled,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = VOICE_INPUT_DESCRIPTION,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        )
                    }
                    IconButton(
                        onClick = onSend,
                        enabled = enabled,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = SendBlue,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = SEND_DESCRIPTION,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(1, "Can you explain Compose chat UIs?", ChatAuthor.USER),
                    ChatMessage(2, "Use **LazyColumn** with stable keys and hoisted state.", ChatAuthor.GEMINI),
                ),
            ),
            widthClass = WindowSizeClass
                .calculateFromSize(DpSize(width = 390.dp, height = 844.dp))
                .widthSizeClass,
            onPromptChange = {},
            onSend = {},
            onVoiceInput = {},
            onNewChat = {},
            onSelectChat = {},
            darkTheme = false,
            onToggleTheme = {},
        )
    }
}
