package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.content.Intent
import android.graphics.Matrix
import android.net.Uri
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.os.Bundle
import android.graphics.BitmapFactory
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── Colour palette (matches main dark theme) ─────────────────────────────────
private val SidebarBg        = Color(0xFF171717)   // ChatGPT sidebar colour
private val SidebarItemHover = Color(0xFF212121)
private val SidebarDivider   = Color(0xFF2D2D2D)
private val SidebarText      = Color(0xFFECECEC)
private val SidebarSubText   = Color(0xFF8E8E8E)
private val BtnGrey          = Color(0xFF2F2F2F)

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(state, viewModel::onPromptChange, viewModel::onSend,
        autoFocus = state.openKeyboard, onNewChat = viewModel::newChat,
        onSelectChat = viewModel::selectChat, onDeleteChat = viewModel::deleteChat,
        onSaveSettings = viewModel::saveSettings,
        onAttachmentsAdded = viewModel::addAttachments,
        onAttachmentRemoved = viewModel::removeAttachment)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    autoFocus: Boolean = true,
    onNewChat: () -> Unit = {},
    onSelectChat: (String) -> Unit = {},
    onDeleteChat: (String) -> Unit = {},
    onSaveSettings: (String?, Boolean, String) -> Unit = { _, _, _ -> },
    onAttachmentsAdded: (List<PendingAttachment>) -> Unit = {},
    onAttachmentRemoved: (String) -> Unit = {},
) {
    var settingsOpen by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val windowClass = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        WindowSizeClass.calculateFromSize(DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp))
    }
    val contentWidth = remember(windowClass.widthSizeClass) {
        if (windowClass.widthSizeClass == WindowWidthSizeClass.Expanded) 840.dp else 760.dp
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val preview = LocalInspectionMode.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isRestoring) {
        if (!preview && autoFocus && !state.isRestoring) {
            try {
                focusRequester.requestFocus()
                keyboard?.show()
            } catch (_: Exception) { /* field not yet in composition */ }
        }
    }
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) keyboard?.hide()
    }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            ChatPanel(
                state = state,
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = { onNewChat(); scope.launch { drawerState.close() } },
                onSelectChat = { onSelectChat(it); scope.launch { drawerState.close() } },
                onDeleteChat = onDeleteChat,
                onSettings = { scope.launch { drawerState.close() }; settingsOpen = true },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Gemini", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { keyboard?.hide(); scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, stringResource(R.string.chat_history), Modifier.size(22.dp))
                        }
                    },
                    actions = {
                        // Use Image so the actual PNG renders in full colour, not as a tinted silhouette
                        IconButton(onClick = { settingsOpen = true }) {
                            Image(
                                painter = painterResource(R.drawable.settings),
                                contentDescription = "Settings",
                                modifier = Modifier.size(26.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isRestoring) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.messages.isEmpty() && !state.isLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_gemini_mark), null,
                            Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    if (windowClass.heightSizeClass != WindowHeightSizeClass.Compact) {
                        Suggestions { prompt ->
                            onPromptChange(prompt)
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }
                    }
                    state.errorMessage?.let { ErrorCard(it, Modifier.padding(horizontal = 16.dp)) }
                } else {
                    key(state.activeId) {
                        MessageList(state, Modifier.weight(1f).widthIn(max = contentWidth).fillMaxWidth())
                    }
                }
                PromptBar(
                    state = state,
                    focusRequester = focusRequester,
                    keyboard = keyboard,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onAttachmentsAdded = onAttachmentsAdded,
                    onAttachmentRemoved = onAttachmentRemoved,
                    modifier = Modifier
                        .widthIn(max = contentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
    if (settingsOpen) SettingsDialog(state, onSaveSettings, onClose = { settingsOpen = false })
}

// ── Suggestions ───────────────────────────────────────────────────────────────

@Composable
private fun Suggestions(onSelect: (String) -> Unit) {
    val suggestions = listOf(
        Triple(R.string.suggestion_write, R.string.suggestion_write_detail, R.string.suggestion_write_prompt),
        Triple(R.string.suggestion_explain, R.string.suggestion_explain_detail, R.string.suggestion_explain_prompt),
        Triple(R.string.suggestion_ideas, R.string.suggestion_ideas_detail, R.string.suggestion_ideas_prompt),
    )
    LazyRow(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(suggestions, key = { it.first }) { (title, subtitle, prompt) ->
            val promptText = stringResource(prompt)
            Surface(onClick = { onSelect(promptText) }, color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)) {
                Column(
                    Modifier.width(210.dp).heightIn(min = 82.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(stringResource(subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ── Message list ──────────────────────────────────────────────────────────────

@Composable
private fun MessageList(state: ChatUiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + (if (state.isLoading) 1 else 0) + (if (state.errorMessage != null) 1 else 0)
    LaunchedEffect(state.messages.size, state.isLoading, state.errorMessage) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }
    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        items(state.messages, key = { it.id }, contentType = { it.role }) { ChatBubble(it) }
        if (state.isLoading) item(key = "loading") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.gemini_thinking), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.errorMessage?.let { message -> item(key = "error") { ErrorCard(message) } }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        if (!isUser) {
            Icon(painterResource(R.drawable.ic_gemini_mark), null, Modifier.padding(bottom = 12.dp).size(24.dp))
            when (message.kind) {
                MessageKind.TEXT -> Box(Modifier.fillMaxWidth()) { ResponseContent(message.text) }
                MessageKind.IMAGE -> GeneratedImageMessage(message)
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = 620.dp).padding(start = 32.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (message.attachments.isNotEmpty()) {
                    SentAttachmentGallery(message.attachments)
                }
                Surface(
                    color = Color(0xFF253D56),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    SelectionContainer {
                        Text(
                            message.text,
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SentAttachmentGallery(attachments: List<PendingAttachment>) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attachments.forEach { attachment ->
            if (attachment.mimeType.startsWith("image/")) {
                val bitmap by produceState<android.graphics.Bitmap?>(null, attachment.uri) {
                    value = withContext(Dispatchers.IO) {
                        loadPreviewBitmap(context, Uri.parse(attachment.uri), maxDimension = 900)
                    }
                }
                Surface(
                    modifier = Modifier.widthIn(max = 280.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = attachment.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        )
                    } else {
                        Text(
                            "Image unavailable",
                            Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        attachment.name,
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageMessage(message: ChatMessage) {
    when (message.imageState) {
        ImageState.READY -> {
            val bitmap = remember(message.imagePath) { message.imagePath?.let(BitmapFactory::decodeFile) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).clip(RoundedCornerShape(18.dp)),
                )
            } else ImageGenerationCard("The generated image is unavailable on this device.", false)
        }
        ImageState.FAILED -> ImageGenerationCard(message.text.ifBlank { "Image creation failed." }, false)
        else -> ImageGenerationCard("Creating image…", true)
    }
}

@Composable
private fun ImageGenerationCard(label: String, loading: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.widthIn(max = 420.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = RoundedCornerShape(16.dp)) {
        Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Voice Decibel Meter Canvas ───────────────────────────────────────────────

@Composable
private fun VoiceDecibelMeter(
    levels: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barWidth = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val step = barWidth + gap
        val maxBars = (size.width / step).toInt().coerceAtLeast(1)
        val recentLevels = levels.takeLast(maxBars)
        val startX = size.width - (recentLevels.size * step)

        for (i in recentLevels.indices) {
            val level = recentLevels[i]
            val barHeight = (size.height * level).coerceIn(6.dp.toPx(), size.height)
            val x = startX + (i * step)
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                color = Color(0xFF10A37F), // ChatGPT accent green
                topLeft = Offset(x, y),
                size = GeoSize(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

// ── Transcribing Animation ───────────────────────────────────────────────────

@Composable
private fun TranscribingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha1 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "d1")
    val alpha2 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(400, delayMillis = 150), RepeatMode.Reverse), label = "d2")
    val alpha3 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(400, delayMillis = 300), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            "Transcribing...",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFECECEC), fontWeight = FontWeight.Medium)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(6.dp).background(Color(0xFF10A37F).copy(alpha = alpha1), CircleShape))
            Box(Modifier.size(6.dp).background(Color(0xFF10A37F).copy(alpha = alpha2), CircleShape))
            Box(Modifier.size(6.dp).background(Color(0xFF10A37F).copy(alpha = alpha3), CircleShape))
        }
    }
}

// ── Prompt bar ────────────────────────────────────────────────────────────────

@Composable
private fun PromptBar(
    state: ChatUiState,
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentsAdded: (List<PendingAttachment>) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val promptLabel = stringResource(R.string.enter_your_prompt_here)
    val interactionSource = remember { MutableInteractionSource() }
    var actionsOpen by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    val audioLevels = remember { mutableStateListOf<Float>() }
    var smoothedAudioLevel by remember { mutableFloatStateOf(0.2f) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var userRequestedListening by remember { mutableStateOf(false) }

    fun attachmentFor(uri: Uri, fallbackName: String): PendingAttachment {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: fallbackName
        return PendingAttachment(uri.toString(), name, resolver.getType(uri) ?: "application/octet-stream")
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        uris.forEach { uri -> runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } }
        onAttachmentsAdded(uris.map { attachmentFor(it, "Photo") })
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } }
        onAttachmentsAdded(uris.map { attachmentFor(it, "File") })
    }
    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingCameraUri
        if (captured && uri != null) onAttachmentsAdded(listOf(attachmentFor(uri, "Camera photo")))
        pendingCameraUri = null
    }

    fun launchCamera() {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", file)
        pendingCameraUri = uri
        cameraPicker.launch(uri)
    }

    DisposableEffect(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        onDispose {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
        }
    }

    val startListening = {
        val recognizer = speechRecognizer
        if (recognizer != null) {
            userRequestedListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Prevent sudden early timeout during speech pauses
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300000L) // 5 mins
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L) // 15s silence threshold
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    isTranscribing = false
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val target = ((rmsdB + 2f) / 12f).coerceIn(0.15f, 1f)
                    smoothedAudioLevel += (target - smoothedAudioLevel) * 0.18f
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (!userRequestedListening) {
                        isListening = false
                        isTranscribing = true
                    }
                }
                override fun onError(error: Int) {
                    // If error occurs (e.g. speech timeout or no match) but user still wants to listen, keep session active
                    if (userRequestedListening && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        try { recognizer.startListening(intent) } catch (_: Exception) {
                            isListening = false
                            isTranscribing = false
                            userRequestedListening = false
                        }
                    } else {
                        isListening = false
                        isTranscribing = false
                        userRequestedListening = false
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        val currentText = state.prompt
                        val updated = if (currentText.isBlank()) text else "$currentText $text"
                        onPromptChange(updated)
                    }
                    isListening = false
                    isTranscribing = false
                    userRequestedListening = false
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        onPromptChange(text)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(intent)
            isListening = true
            isTranscribing = false
            audioLevels.clear()
            smoothedAudioLevel = 0.2f
        } else {
            Toast.makeText(context, "Speech recognition unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val stopListening = {
        userRequestedListening = false
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        isListening = false
        isTranscribing = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleVoiceInput = {
        if (isListening) {
            stopListening()
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Continuously add voice decibel samples over time while listening so decibel meter moves ahead with time
    LaunchedEffect(isListening) {
        if (isListening) {
            var timeMs = 0L
            while (isListening) {
                delay(130)
                timeMs += 130
                val wave = (kotlin.math.sin(timeMs / 360.0) * 0.06f).toFloat()
                val liveLevel = (smoothedAudioLevel + wave).coerceIn(0.12f, 0.95f)
                audioLevels.add(liveLevel)
                while (audioLevels.size > 80) audioLevels.removeAt(0)
            }
        }
    }

    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(if (state.pendingAttachments.isEmpty()) CircleShape else RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isListening && !isTranscribing,
                ) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                },
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (state.pendingAttachments.isNotEmpty()) {
                    AttachmentStrip(state.pendingAttachments, onAttachmentRemoved)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Box {
                    IconButton(
                        onClick = { actionsOpen = !actionsOpen },
                        enabled = !state.isLoading && !state.isRestoring && !isListening,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "More actions",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AttachmentPopover(
                        expanded = actionsOpen,
                        onDismiss = { actionsOpen = false },
                        onPhotos = {
                            actionsOpen = false
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        onCamera = { actionsOpen = false; launchCamera() },
                        onFiles = { actionsOpen = false; filePicker.launch(arrayOf("*/*")) },
                    )
                }

                // Middle area: Text Field OR Decibel Meter OR Transcribing Animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isListening) {
                        VoiceDecibelMeter(
                            levels = audioLevels,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    } else if (isTranscribing) {
                        TranscribingAnimation(Modifier.fillMaxWidth())
                    } else {
                        BasicTextField(
                            value = state.prompt,
                            enabled = !state.isRestoring,
                            onValueChange = onPromptChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .semantics { contentDescription = promptLabel },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (!state.isRestoring && !state.isLoading && (state.prompt.isNotBlank() || state.pendingAttachments.isNotEmpty())) {
                                        onSend()
                                    }
                                },
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (state.prompt.isEmpty()) {
                                        Text(
                                            promptLabel,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }
                }

                // Microphone
                IconButton(onClick = toggleVoiceInput, modifier = Modifier.size(38.dp)) {
                    if (isListening) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(Color(0xFF10A37F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_mic),
                                "Stop listening",
                                Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_mic),
                            "Voice input",
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Send button
                IconButton(
                    onClick = onSend,
                    enabled = (state.prompt.isNotBlank() || state.pendingAttachments.isNotEmpty()) && !state.isLoading && !state.isRestoring && !isListening,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(
                                if ((state.prompt.isNotBlank() || state.pendingAttachments.isNotEmpty()) && !state.isLoading && !isListening) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_up),
                            stringResource(R.string.send),
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                }
            }
        }
        if (state.promptError != null) {
            Text(
                stringResource(R.string.field_cannot_be_empty),
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ── Left panel (ChatGPT-style, minimalistic) ──────────────────────────────────

@Composable
private fun AttachmentStrip(attachments: List<PendingAttachment>, onRemove: (String) -> Unit) {
    val context = LocalContext.current
    LazyRow(
        Modifier.fillMaxWidth().height(108.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(attachments, key = { it.uri }) { attachment ->
            AttachmentPreview(attachment, onRemove, context)
        }
    }
}

@Composable
private fun AttachmentPreview(
    attachment: PendingAttachment,
    onRemove: (String) -> Unit,
    context: android.content.Context,
) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, attachment.uri, attachment.mimeType) {
        value = if (!attachment.mimeType.startsWith("image/")) null
        else withContext(Dispatchers.IO) { loadPreviewBitmap(context, Uri.parse(attachment.uri)) }
    }
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        bitmap?.let { preview ->
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: run {
            Text(
                attachment.name.take(1).uppercase(),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                .clickable { onRemove(attachment.uri) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, "Remove ${attachment.name}", Modifier.size(15.dp), tint = Color.White)
        }
    }
}

private fun loadPreviewBitmap(
    context: android.content.Context,
    uri: Uri,
    maxDimension: Int = 184,
): android.graphics.Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) }
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return@runCatching null
    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    decoded.rotateForExifOrientation(orientation)
}.getOrNull()

private fun android.graphics.Bitmap.rotateForExifOrientation(orientation: Int): android.graphics.Bitmap {
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(270f)
                postScale(-1f, 1f)
            }
        }
    }
    return if (matrix.isIdentity) this else android.graphics.Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

@Composable
private fun ChatPanel(
    state: ChatUiState,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val title = stringResource(R.string.chat_history)
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth(0.82f)
            .semantics { paneTitle = title },
        drawerContainerColor = SidebarBg,
        drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
        drawerTonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxHeight()) {

            // ── Top: logo + close ────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Gemini",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = SidebarText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_chat_history),
                        tint = SidebarSubText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── New Chat button ──────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BtnGrey)
                    .clickable(enabled = !state.isRestoring, onClick = onNewChat)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_gemini_mark),
                    contentDescription = null,
                    tint = SidebarText,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "New chat",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SidebarText,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Recents label ────────────────────────────────────────────────
            Text(
                "Recent",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SidebarSubText,
                    letterSpacing = 0.8.sp,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // ── Conversation list ────────────────────────────────────────────
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(state.conversations, key = { it.id }) { chat ->
                    val isActive = chat.id == state.activeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isActive) SidebarItemHover else Color.Transparent)
                            .clickable { onSelectChat(chat.id) }
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            chat.title,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isActive) SidebarText else SidebarSubText,
                            ),
                        )
                        // delete icon – always visible on active, else visible on row hover
                        IconButton(
                            onClick = { pendingDelete = chat.id },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = SidebarSubText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                if (state.conversations.isEmpty()) {
                    item {
                        Text(
                            "No conversations yet",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = SidebarSubText),
                        )
                    }
                }
            }

            // ── Divider ──────────────────────────────────────────────────────
            HorizontalDivider(color = SidebarDivider, thickness = 0.5.dp)

            // ── Bottom: Settings ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Image instead of Icon so the PNG renders in full colour
                Image(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    "Settings",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SidebarText),
                )
            }
        }
    }

    // ── Delete confirmation dialog ───────────────────────────────────────────
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this chat?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDeleteChat(id); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Empty", widthDp = 360, heightDp = 720)
@Preview(name = "Compact", widthDp = 320, heightDp = 480)
@Composable
private fun EmptyChatPreview() {
    GeminiApiComposeStarterTheme(darkTheme = true) { ChatScreen(ChatUiState(), {}, {}) }
}

@Preview(name = "Conversation", widthDp = 393, heightDp = 851)
@Composable
private fun ConversationPreview() {
    GeminiApiComposeStarterTheme(darkTheme = true) {
        ChatScreen(ChatUiState(messages = listOf(
            ChatMessage(0, ChatRole.USER, "What makes a good daily routine?"),
            ChatMessage(1, ChatRole.GEMINI, "Start small. Pick **one thing** you want to make time for, and give it a regular place in your day.\n\nA routine should make your day easier, not busier."),
        )), {}, {})
    }
}
