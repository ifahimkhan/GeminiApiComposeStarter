package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import java.io.File

object ChatTestTags {
    const val MESSAGE_LIST = "message_list"
    const val PROMPT_FIELD = "prompt_field"
    const val SEND_BUTTON = "send_button"
    const val STOP_BUTTON = "stop_button"
    const val LOADING_INDICATOR = "loading_indicator"
    const val MENU_BUTTON = "menu_button"
    const val VOICE_BUTTON = "voice_button"
    const val ATTACH_BUTTON = "attach_button"
    const val GALLERY_ITEM = "gallery_item"
    const val CAMERA_ITEM = "camera_item"
    const val ATTACHMENT_PREVIEW = "attachment_preview"
    const val REMOVE_IMAGE_BUTTON = "remove_image_button"
    const val SPEAK_BUTTON = "speak_button"
}

private const val LOADING_ITEM_KEY = "loading"
private val BubbleCorner = 20.dp
private val BubbleTail = 4.dp
private val BubbleGutter = 48.dp
private val BubbleMaxWidth = 560.dp
private val BubbleImageWidth = 220.dp
private val BubbleImageHeight = 160.dp

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    widthSizeClass: WindowWidthSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The system speech recogniser returns its transcripts as an activity result.
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(viewModel::onVoiceResult)
        }
    }
    val speakPrompt = stringResource(R.string.speak_your_prompt)

    // The photo picker needs no storage permission.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onImageSelected(it.toString()) }
    }

    // The camera app writes into a temporary file that we share with it through a FileProvider.
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = cameraUri
        if (taken && uri != null) viewModel.onImageSelected(uri.toString())
        cameraUri = null
    }

    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onStopGenerating = viewModel::onStopGenerating,
        onRetry = viewModel::onRetry,
        onErrorShown = viewModel::onErrorShown,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onClearConversation = viewModel::onClearConversation,
        onVoiceInput = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, speakPrompt)
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                viewModel.onVoiceUnavailable()
            }
        },
        onPickFromGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onTakePhoto = {
            val photoDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val photo = File.createTempFile("photo_", ".jpg", photoDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
            cameraUri = uri
            try {
                cameraLauncher.launch(uri)
            } catch (e: ActivityNotFoundException) {
                cameraUri = null
                viewModel.onCameraUnavailable()
            }
        },
        onRemoveImage = viewModel::onImageRemoved,
        onSpeakMessage = viewModel::onSpeakMessage,
    )
}

/** Stateless: everything it shows comes from [state], every user action goes out through a callback. */
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onClearConversation: () -> Unit,
    onVoiceInput: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    onSpeakMessage: (Long) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = retryLabel.takeIf { state.errorIsRetryable && state.canRetry },
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        onErrorShown()
        if (result == SnackbarResult.ActionPerformed) onRetry()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            ChatTopBar(
                themeMode = state.themeMode,
                hasMessages = state.messages.isNotEmpty(),
                onThemeModeSelected = onThemeModeSelected,
                onClearConversation = onClearConversation,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            // On tablets and landscape the chat stays a readable column instead of stretching edge to edge.
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = contentMaxWidth(widthSizeClass))
                    .fillMaxWidth(),
            ) {
                MessageList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    speakingMessageId = state.speakingMessageId,
                    onSpeakMessage = onSpeakMessage,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    attachedImagePath = state.attachedImagePath,
                    isLoading = state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onStopGenerating = onStopGenerating,
                    onVoiceInput = onVoiceInput,
                    onPickFromGallery = onPickFromGallery,
                    onTakePhoto = onTakePhoto,
                    onRemoveImage = onRemoveImage,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    themeMode: ThemeMode,
    hasMessages: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onClearConversation: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.chat_title)) },
        actions = {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag(ChatTestTags.MENU_BUTTON),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_conversation)) },
                    enabled = hasMessages,
                    onClick = {
                        menuOpen = false
                        onClearConversation()
                    },
                )
                HorizontalDivider()
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(stringResource(mode.labelRes())) },
                        leadingIcon = if (mode == themeMode) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            menuOpen = false
                            onThemeModeSelected(mode)
                        },
                    )
                }
            }
        },
    )
}

private fun ThemeMode.labelRes() = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun contentMaxWidth(widthSizeClass: WindowWidthSizeClass): Dp = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> 600.dp
    WindowWidthSizeClass.Medium -> 680.dp
    else -> 840.dp
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    speakingMessageId: Long?,
    onSpeakMessage: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0

    // Follow the conversation: jump to the newest bubble (or the loading bubble) whenever the list grows.
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    if (itemCount == 0) {
        Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.empty_chat_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.testTag(ChatTestTags.MESSAGE_LIST),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isSpeaking = message.id == speakingMessageId,
                    onSpeak = { onSpeakMessage(message.id) },
                )
            }
            if (isLoading) {
                item(key = LOADING_ITEM_KEY) { LoadingBubble() }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
) {
    val isUser = message.author == Author.USER
    Row(
        // The gutter on the opposite side keeps bubbles from spanning the full width.
        modifier = Modifier.fillMaxWidth().padding(
            start = if (isUser) BubbleGutter else 0.dp,
            end = if (isUser) 0.dp else BubbleGutter,
        ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            AssistantAvatar()
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f, fill = false).widthIn(max = BubbleMaxWidth),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                shape = bubbleShape(isUser),
            ) {
                Column {
                    message.imagePath?.let { path ->
                        StoredImage(
                            path = path,
                            contentDescription = stringResource(R.string.attached_image),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(width = BubbleImageWidth, height = BubbleImageHeight)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = if (isUser) AnnotatedString(message.text) else message.text.toBoldAnnotatedString(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            if (!isUser) {
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.size(36.dp).testTag(ChatTestTags.SPEAK_BUTTON),
                ) {
                    Icon(
                        painter = painterResource(if (isSpeaking) R.drawable.ic_stop else R.drawable.ic_volume_up),
                        contentDescription = stringResource(if (isSpeaking) R.string.stop_reading else R.string.read_aloud),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    val description = stringResource(R.string.gemini_is_thinking)
    Row(modifier = Modifier.fillMaxWidth()) {
        AssistantAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = bubbleShape(isUser = false),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .size(20.dp)
                    .testTag(ChatTestTags.LOADING_INDICATOR)
                    .semantics { contentDescription = description },
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun AssistantAvatar() {
    Icon(
        painter = painterResource(R.drawable.ic_assistant),
        contentDescription = null,
        modifier = Modifier.size(32.dp).padding(top = 4.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

private fun bubbleShape(isUser: Boolean) = RoundedCornerShape(
    topStart = BubbleCorner,
    topEnd = BubbleCorner,
    bottomEnd = if (isUser) BubbleTail else BubbleCorner,
    bottomStart = if (isUser) BubbleCorner else BubbleTail,
)

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    attachedImagePath: String?,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    onVoiceInput: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (attachedImagePath != null) {
            AttachmentPreview(path = attachedImagePath, onRemove = onRemoveImage)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachButton(
                enabled = !isLoading,
                onPickFromGallery = onPickFromGallery,
                onTakePhoto = onTakePhoto,
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f).testTag(ChatTestTags.PROMPT_FIELD),
                label = { Text(stringResource(R.string.enter_your_prompt_here)) },
                minLines = 1,
                maxLines = 4,
                isError = promptError != null,
                supportingText = promptError?.let {
                    { Text(stringResource(R.string.field_cannot_be_empty)) }
                },
            )
            IconButton(
                onClick = onVoiceInput,
                enabled = !isLoading,
                modifier = Modifier.testTag(ChatTestTags.VOICE_BUTTON),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.voice_input),
                )
            }
            if (isLoading) {
                // While Gemini is working the send button turns into a stop button.
                FilledIconButton(
                    onClick = onStopGenerating,
                    modifier = Modifier.testTag(ChatTestTags.STOP_BUTTON),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_stop),
                        contentDescription = stringResource(R.string.stop_generating),
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    modifier = Modifier.testTag(ChatTestTags.SEND_BUTTON),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachButton(
    enabled: Boolean,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { menuOpen = true },
            enabled = enabled,
            modifier = Modifier.testTag(ChatTestTags.ATTACH_BUTTON),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_attach),
                contentDescription = stringResource(R.string.attach_image),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.choose_from_gallery)) },
                modifier = Modifier.testTag(ChatTestTags.GALLERY_ITEM),
                onClick = {
                    menuOpen = false
                    onPickFromGallery()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.take_photo)) },
                modifier = Modifier.testTag(ChatTestTags.CAMERA_ITEM),
                onClick = {
                    menuOpen = false
                    onTakePhoto()
                },
            )
        }
    }
}

@Composable
private fun AttachmentPreview(path: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp).testTag(ChatTestTags.ATTACHMENT_PREVIEW),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoredImage(
            path = path,
            contentDescription = stringResource(R.string.attached_image),
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
            maxSidePx = 240,
        )
        IconButton(onClick = onRemove, modifier = Modifier.testTag(ChatTestTags.REMOVE_IMAGE_BUTTON)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_image))
        }
    }
}

private val previewMessages = listOf(
    ChatMessage(0, Author.USER, "What is the capital of France?"),
    ChatMessage(1, Author.GEMINI, "The capital of France is **Paris**."),
    ChatMessage(2, Author.USER, "And roughly how many people live there?"),
)

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        PreviewScreen(ChatUiState(messages = previewMessages, isLoading = true), WindowWidthSizeClass.Compact)
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun ChatScreenTabletPreview() {
    GeminiApiComposeStarterTheme {
        PreviewScreen(ChatUiState(messages = previewMessages), WindowWidthSizeClass.Expanded)
    }
}

@Composable
private fun PreviewScreen(state: ChatUiState, widthSizeClass: WindowWidthSizeClass) {
    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = {},
        onSend = {},
        onStopGenerating = {},
        onRetry = {},
        onErrorShown = {},
        onThemeModeSelected = {},
        onClearConversation = {},
        onVoiceInput = {},
        onPickFromGallery = {},
        onTakePhoto = {},
        onRemoveImage = {},
        onSpeakMessage = {},
    )
}
