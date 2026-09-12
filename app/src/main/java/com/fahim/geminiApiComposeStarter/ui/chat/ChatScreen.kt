package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

private const val EMPTY_CHAT_MESSAGE = "What should we explore?"
private const val PROMPT_PLACEHOLDER = "Ask anything"
private const val EMPTY_FIELD_ERROR = "Field cannot be empty"
private const val SEND_DESCRIPTION = "Send"
private const val MENU_DESCRIPTION = "Open menu"
private const val INCOGNITO_DESCRIPTION = "Private mode"
private const val LIGHT_MODE_DESCRIPTION = "Switch to light mode"
private const val DARK_MODE_DESCRIPTION = "Switch to dark mode"

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = calculateWindowSizeClass(LocalContext.current as Activity)
    ChatScreen(
        state = state,
        windowWidthSizeClass = windowSizeClass.widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        darkTheme = darkTheme,
        onToggleTheme = onToggleTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ChatTopBar(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ChatContent(
            state = state,
            windowWidthSizeClass = windowWidthSizeClass,
            onPromptChange = onPromptChange,
            onSend = onSend,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        )
    }
}

@Composable
private fun ChatTopBar(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
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
        IconButton(onClick = {}) {
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
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = INCOGNITO_DESCRIPTION,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatContent(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val horizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 32.dp
        else -> 48.dp
    }
    val maxContentWidth = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 560.dp
        WindowWidthSizeClass.Medium -> 720.dp
        else -> 840.dp
    }
    val showLoadingBubble = state.isLoading && state.messages.lastOrNull()?.author != ChatAuthor.GEMINI
    val latestMessageTextLength = state.messages.lastOrNull()?.text?.length ?: 0
    val extraItemCount = when {
        showLoadingBubble && state.errorMessage != null -> 2
        showLoadingBubble || state.errorMessage != null -> 1
        state.messages.isEmpty() -> 1
        else -> 0
    }

    LaunchedEffect(state.messages.size, latestMessageTextLength, showLoadingBubble, state.errorMessage) {
        val itemCount = state.messages.size + extraItemCount
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxContentWidth),
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
                        horizontal = horizontalPadding,
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
                            windowWidthSizeClass = windowWidthSizeClass,
                        )
                    }
                    if (showLoadingBubble) {
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
                horizontalPadding = horizontalPadding,
                onPromptChange = onPromptChange,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val isUser = message.author == ChatAuthor.USER
    val maxUserBubbleWidth = when (windowWidthSizeClass) {
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
                modifier = Modifier.widthIn(max = maxUserBubbleWidth),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
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
    Markdown(
        content = content,
        modifier = modifier.fillMaxWidth(),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
            h2 = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
            h3 = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, lineHeight = 23.sp),
            h4 = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
            h5 = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
            h6 = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
            text = MaterialTheme.typography.bodyLarge,
            paragraph = MaterialTheme.typography.bodyLarge,
            ordered = MaterialTheme.typography.bodyLarge,
            bullet = MaterialTheme.typography.bodyLarge,
            list = MaterialTheme.typography.bodyLarge,
            code = MaterialTheme.typography.bodyMedium,
            inlineCode = MaterialTheme.typography.bodyMedium,
            table = MaterialTheme.typography.bodyMedium,
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
    horizontalPadding: Dp,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = horizontalPadding, end = horizontalPadding, top = 12.dp, bottom = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = PROMPT_PLACEHOLDER,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 1,
                    maxLines = 4,
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
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onSend,
                        enabled = enabled,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = SEND_DESCRIPTION,
                            tint = MaterialTheme.colorScheme.primary,
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
            windowWidthSizeClass = WindowSizeClass
                .calculateFromSize(DpSize(width = 390.dp, height = 844.dp))
                .widthSizeClass,
            onPromptChange = {},
            onSend = {},
            darkTheme = false,
            onToggleTheme = {},
        )
    }
}
