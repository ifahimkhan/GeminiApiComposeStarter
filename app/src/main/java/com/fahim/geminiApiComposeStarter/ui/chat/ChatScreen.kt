package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.mascot.MascotAvatar
import com.fahim.geminiApiComposeStarter.ui.mascot.MascotState
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode

/** Test tags, so the Compose UI tests target structure rather than user-visible strings. */
object ChatTestTags {
    const val CONVERSATION = "conversation"
    const val PROMPT_FIELD = "prompt-field"
    const val SEND_BUTTON = "send-button"
    const val TYPING_INDICATOR = "typing-indicator"
    const val MASCOT = "mascot"
}

/** Key for the typing row so it keeps its slot in the LazyColumn. */
private const val TYPING_KEY = "typing-indicator"

/**
 * Connects the screen to the ViewModel. This is the only stateful composable in the file --
 * everything below it takes plain values, which is what makes the previews and UI tests possible.
 */
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onErrorShown = viewModel::onErrorShown,
        onClearHistory = viewModel::onClearHistory,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        modifier = modifier,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onErrorShown: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // Phones get the full width; tablets and landscape get a centred, readable column instead of
    // one very long line of text.
    val contentMaxWidth: Dp = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> Dp.Unspecified
        WindowWidthSizeClass.Medium -> 620.dp
        else -> 840.dp
    }
    val bubbleMaxWidthFraction = if (widthSizeClass == WindowWidthSizeClass.Compact) 0.88f else 0.72f
    val horizontalPadding = if (widthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp
    val mascotSize = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 68.dp
        WindowWidthSizeClass.Medium -> 84.dp
        else -> 96.dp
    }

    // The list state is hoisted here because the mascot header reacts to it: the avatar yields
    // its vertical space once the transcript is scrolled, so a long conversation is not squeezed.
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOfScrolled(listState) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showMascot = !scrolled && !imeVisible

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            ChatTopBar(
                themeMode = state.themeMode,
                dynamicColor = state.dynamicColor,
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onClearHistory = onClearHistory,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
                    .padding(horizontal = horizontalPadding),
            ) {
                // The header collapses rather than jumping away. The avatar is unmounted while
                // hidden -- safe to do repeatedly, because its animation is a pure function of an
                // infinite phase with no state to lose, so it simply resumes its loop on return.
                AnimatedVisibility(
                    visible = showMascot,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    MascotHeader(state = state.mascotState, size = mascotSize)
                }

                ConversationList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    listState = listState,
                    bubbleMaxWidthFraction = bubbleMaxWidthFraction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    isLoading = state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                )
            }
        }
    }
}

/** `derivedStateOf` wrapper kept out of the composable body so it is created exactly once. */
private fun derivedStateOfScrolled(listState: LazyListState) =
    androidx.compose.runtime.derivedStateOf {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
    }

@Composable
private fun MascotHeader(state: MascotState, size: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        MascotAvatar(
            state = state,
            size = size,
            modifier = Modifier.testTag(ChatTestTags.MASCOT),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.mascot_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    when (state) {
                        MascotState.THINKING -> R.string.mascot_status_thinking
                        MascotState.SLEEPY -> R.string.mascot_status_sleepy
                        MascotState.CONFUSED -> R.string.mascot_status_confused
                        else -> R.string.mascot_status_ready
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.chat_title)) },
        actions = {
            IconButton(onClick = onClearHistory) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.clear_history),
                )
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.settings),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label()) },
                        leadingIcon = { RadioButton(selected = mode == themeMode, onClick = null) },
                        onClick = {
                            onThemeModeChange(mode)
                            menuOpen = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dynamic_colour)) },
                    trailingIcon = { Switch(checked = dynamicColor, onCheckedChange = null) },
                    onClick = { onDynamicColorChange(!dynamicColor) },
                )
            }
        },
    )
}

@Composable
private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun ConversationList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: LazyListState,
    bubbleMaxWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty() && !isLoading) {
        EmptyConversation(modifier = modifier)
        return
    }

    // Auto-scroll so the newest bubble (or the typing row) is always visible.
    LaunchedEffect(messages.size, isLoading) {
        val lastIndex = messages.size - 1 + if (isLoading) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag(ChatTestTags.CONVERSATION),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            EnterFromBelow {
                MessageBubble(message = message, maxWidthFraction = bubbleMaxWidthFraction)
            }
        }
        if (isLoading) {
            item(key = TYPING_KEY) {
                EnterFromBelow { TypingIndicator() }
            }
        }
    }
}

/**
 * Slides a row up from the input bar and fades it in the first time it is composed, so new
 * messages read as the thread scrolling up rather than as content popping into place.
 */
@Composable
private fun EnterFromBelow(content: @Composable () -> Unit) {
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = transitionState,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = fadeOut(),
    ) {
        content()
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, maxWidthFraction: Float) {
    val fromUser = message.author == Author.USER
    val bubbleColor = if (fromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (fromUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            contentColor = textColor,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(maxWidthFraction),
        ) {
            Text(
                text = message.text.toBoldAnnotatedString(),
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** Three dots bouncing in sequence -- the conventional "assistant is composing" affordance. */
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.testTag(ChatTestTags.TYPING_INDICATOR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val offset by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 900
                                0f at 0 using LinearEasing
                                -5f at 150 using LinearEasing
                                0f at 300 using LinearEasing
                                0f at 900
                            },
                            repeatMode = RepeatMode.Restart,
                            // Stagger each dot so they bounce in sequence, not together.
                            initialStartOffset = androidx.compose.animation.core.StartOffset(
                                index * 150,
                            ),
                        ),
                        label = "dot-$index",
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = offset * density }
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.response_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    var speechUnavailable by remember { mutableStateOf(false) }

    // Speech-to-text: the system recogniser runs in its own Activity and hands the transcript
    // back through the Activity Result API.
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(onPromptChange)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                // Grows smoothly as the text wraps instead of snapping to the next line height.
                .animateContentSize()
                .testTag(ChatTestTags.PROMPT_FIELD),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = 1,
            maxLines = 5,
            shape = MaterialTheme.shapes.large,
            enabled = !isLoading,
            isError = promptError != null || speechUnavailable,
            supportingText = when {
                promptError != null -> {
                    { Text(stringResource(R.string.field_cannot_be_empty)) }
                }

                speechUnavailable -> {
                    { Text(stringResource(R.string.speech_unavailable)) }
                }

                else -> null
            },
        )
        IconButton(
            onClick = {
                speechUnavailable = false
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_PROMPT,
                        context.getString(R.string.speak_your_prompt),
                    )
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    speechUnavailable = true
                }
            },
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.voice_input),
            )
        }
        FilledIconButton(
            onClick = onSend,
            enabled = !isLoading,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .testTag(ChatTestTags.SEND_BUTTON),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
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

private val previewState = ChatUiState(
    messages = listOf(
        ChatMessage(id = 1, text = "What is Jetpack Compose?", author = Author.USER),
        ChatMessage(
            id = 2,
            text = "**Jetpack Compose** is Android's declarative UI toolkit for building native " +
                "interfaces in Kotlin.",
            author = Author.GEMINI,
        ),
    ),
)

@Preview(showBackground = true, name = "Chat - phone")
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = previewState,
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true, name = "Chat - thinking, dark", backgroundColor = 0xFF101415)
@Composable
private fun ChatScreenDarkPreview() {
    GeminiApiComposeStarterTheme(themeMode = ThemeMode.DARK) {
        ChatScreen(
            state = previewState.copy(isLoading = true, mascotState = MascotState.THINKING),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true, name = "Chat - tablet", widthDp = 840, heightDp = 640)
@Composable
private fun ChatScreenExpandedPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = previewState.copy(mascotState = MascotState.HAPPY),
            widthSizeClass = WindowWidthSizeClass.Expanded,
            onPromptChange = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true, name = "Chat - empty")
@Composable
private fun ChatScreenEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {},
            onSend = {},
        )
    }
}
