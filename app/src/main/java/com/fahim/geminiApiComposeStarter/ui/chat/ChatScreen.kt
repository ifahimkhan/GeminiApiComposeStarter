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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.HapticFeedbackConstants
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import androidx.compose.ui.platform.LocalView

// ── Colour palette (theme-aware) ──────────────────────────────────────────────
private val SidebarBg        @Composable get() = MaterialTheme.colorScheme.surfaceContainer
private val SidebarItemHover @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
private val SidebarDivider   @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val SidebarText      @Composable get() = MaterialTheme.colorScheme.onSurface
private val SidebarSubText   @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val BtnGrey          @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

/** Short, clearly-felt vibration — works on Samsung/Pixel/OnePlus alike. */
@Composable
private fun rememberHapticTap(): () -> Unit {
    val context = LocalContext.current
    return remember {
        {
            @Suppress("DEPRECATION")
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                    as android.os.VibratorManager).defaultVibrator
            } else {
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        }
    }
}

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLiveVoice by remember { mutableStateOf(false) }

    // Wrap the entire composition with the theme so toggling dark/light applies immediately
    com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme(
        darkTheme = state.darkMode,
        dynamicColor = false
    ) {
        if (showLiveVoice) {
            LiveVoiceScreen(
                state = state,
                onDismiss = { showLiveVoice = false },
                onSessionComplete = { turns ->
                    showLiveVoice = false
                    viewModel.saveLiveVoiceSession(turns)
                },
                onCallGemini = viewModel::generateLiveVoiceResponse,
                onNewChat = viewModel::newChat,
                onSelectChat = viewModel::selectChat,
                onDeleteChat = viewModel::deleteChat,
                onSaveSettings = viewModel::saveSettings,
            )
        } else {
            ChatScreen(
                state = state,
                onPromptChange = viewModel::onPromptChange,
                onSend = viewModel::onSend,
                autoFocus = state.openKeyboard,
                onNewChat = viewModel::newChat,
                onSelectChat = viewModel::selectChat,
                onDeleteChat = viewModel::deleteChat,
                onSaveSettings = viewModel::saveSettings,
                onAttachmentsAdded = viewModel::addAttachments,
                onAttachmentRemoved = viewModel::removeAttachment,
                onRegenerate = viewModel::onRegenerateLast,
                onStopGenerating = viewModel::stopGenerating,
                onLiveVoiceStart = { showLiveVoice = true }
            )
        }
    }
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
    onSaveSettings: (String?, Boolean, String, Boolean) -> Unit = { _, _, _, _ -> },
    onAttachmentsAdded: (List<PendingAttachment>) -> Unit = {},
    onAttachmentRemoved: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onStopGenerating: () -> Unit = {},
    onLiveVoiceStart: () -> Unit = {},
) {
    val context = LocalContext.current
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

    var isSearchOpen by remember { mutableStateOf(false) }
    var targetMessageId by remember { mutableStateOf<Long?>(null) }
    var searchQueryHighlight by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHapticTap()

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            drawerContent = {
                ChatPanel(
                    state = state,
                    onClose = { scope.launch { drawerState.close() } },
                    onOpenSearch = {
                        haptic()
                        scope.launch { drawerState.close() }
                        isSearchOpen = true
                    },
                    onNewChat = { targetMessageId = null; searchQueryHighlight = null; onNewChat(); scope.launch { drawerState.close() } },
                    onSelectChat = { targetMessageId = null; searchQueryHighlight = null; onSelectChat(it); scope.launch { drawerState.close() } },
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
                        if (state.messages.isNotEmpty()) {
                            IconButton(onClick = {
                                val markdownText = state.messages.joinToString("\n\n") { msg ->
                                    if (msg.role == ChatRole.USER) "### You\n${msg.text}"
                                    else "### Gemini\n${msg.text}"
                                }
                                val file = File(context.cacheDir, "chat_export.md")
                                file.writeText(markdownText)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/markdown"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Chat"))
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_share_box),
                                    contentDescription = "Export Chat",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Material Settings icon — crisp in both dark and light mode
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
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
                AnimatedContent(
                    targetState = state.activeId,
                    transitionSpec = {
                        // Crossfade + subtle upward slide: new content fades/slides up in, old fades out
                        (fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { (it * 0.06f).toInt() },
                                animationSpec = tween(320, easing = FastOutSlowInEasing)
                            )).togetherWith(
                            fadeOut(animationSpec = tween(180, easing = LinearOutSlowInEasing))
                        )
                    },
                    modifier = Modifier.weight(1f),
                    label = "chatContentSwitch"
                ) { activeId ->
                    if (state.isRestoring) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (state.messages.isEmpty() && !state.isLoading) {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_gemini_mark), null,
                                Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(24.dp))
                            val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                            Text(
                                text = "Hello! How can I help you today?",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp / fontScale,
                                    lineHeight = 28.sp / fontScale
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            state.errorMessage?.let { ErrorCard(it, Modifier.padding(horizontal = 16.dp)) }
                        }
                    } else {
                        // activeId captured so MessageList remounts cleanly per chat
                        key(activeId) {
                            MessageList(
                                state = state,
                                onRegenerate = onRegenerate,
                                onStopGenerating = onStopGenerating,
                                targetMessageId = targetMessageId,
                                searchQueryHighlight = searchQueryHighlight,
                                onTargetScrolled = {
                                    scope.launch {
                                        delay(3000)
                                        targetMessageId = null
                                        searchQueryHighlight = null
                                    }
                                },
                                modifier = Modifier.widthIn(max = contentWidth).fillMaxWidth()
                            )
                        }
                    }
                }
                if (!state.isRestoring && state.messages.isEmpty() && !state.isLoading && windowClass.heightSizeClass != WindowHeightSizeClass.Compact) {
                    Suggestions { prompt ->
                        onPromptChange(prompt)
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                PromptBar(
                    state = state,
                    focusRequester = focusRequester,
                    keyboard = keyboard,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onStop = onStopGenerating,
                    onAttachmentsAdded = onAttachmentsAdded,
                    onAttachmentRemoved = onAttachmentRemoved,
                    onLiveVoiceStart = onLiveVoiceStart,
                    modifier = Modifier
                        .widthIn(max = contentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }

    AnimatedVisibility(
        visible = isSearchOpen,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = Modifier.fillMaxSize()
    ) {
        FullScreenSearch(
            state = state,
            onClose = { isSearchOpen = false },
            onSelectChat = { id, msgId, query ->
                isSearchOpen = false
                targetMessageId = msgId
                searchQueryHighlight = query
                onSelectChat(id)
            }
        )
    }

    if (settingsOpen) SettingsDialog(state, onSaveSettings, onClose = { settingsOpen = false })
    }
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
private fun MessageList(
    state: ChatUiState,
    onRegenerate: () -> Unit,
    onStopGenerating: () -> Unit,
    targetMessageId: Long? = null,
    searchQueryHighlight: String? = null,
    onTargetScrolled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val itemCount = state.messages.size + (if (state.isLoading) 1 else 0) + (if (state.errorMessage != null) 1 else 0)

    // Initial list state — always start at index 0; scroll effects below handle positioning
    val listState = rememberLazyListState()

    // Check if the user is already near the bottom (within 1 item of the end)
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }

    // When opening a chat (activeId changes):
    //   • If we have a search target → scroll to that message and offset to the matched word
    //   • Otherwise (history or new chat) → jump to the absolute bottom
    LaunchedEffect(state.activeId) {
        if (state.messages.isEmpty()) return@LaunchedEffect
        if (targetMessageId != null) {
            // search-navigate: handled by targetMessageId effect below
            return@LaunchedEffect
        }
        // Jump to very bottom: scroll to last index with a huge pixel offset so the
        // bottom of the item is visible, not just the top.
        val lastIdx = (state.messages.size - 1).coerceAtLeast(0)
        listState.scrollToItem(lastIdx, Int.MAX_VALUE)
    }

    // Scroll to exact search target and position the viewport so the matched word is visible.
    // After scrolling, calls onTargetScrolled which schedules a 3-second auto-clear of the highlight.
    LaunchedEffect(targetMessageId, searchQueryHighlight) {
        val msgId = targetMessageId ?: return@LaunchedEffect
        if (state.messages.isEmpty()) return@LaunchedEffect
        val targetIdx = state.messages.indexOfFirst { it.id == msgId }
        if (targetIdx < 0) return@LaunchedEffect
        val targetMsg = state.messages[targetIdx]
        // Estimate pixel offset: count actual newlines + wrapped lines (avg 50 chars/line on phone)
        val matchIdx = if (!searchQueryHighlight.isNullOrBlank()) {
            targetMsg.text.indexOf(searchQueryHighlight, ignoreCase = true)
        } else -1
        val pixelOffset = if (matchIdx > 0) {
            val textBefore = targetMsg.text.substring(0, matchIdx)
            val newlineCount = textBefore.count { it == '\n' }
            val wrapLines = (textBefore.replace("\n", "").length / 50)
            val totalLines = newlineCount + wrapLines
            // Each line ≈ 28px (20sp line height + spacing)
            (totalLines * 28).coerceAtLeast(0)
        } else 0
        listState.animateScrollToItem(targetIdx, pixelOffset)
        // Signal parent to start the 3-second countdown to clear highlights
        onTargetScrolled()
    }

    // Scroll to bottom when a NEW message is added (when not navigating to a search target)
    LaunchedEffect(state.messages.size, state.isLoading) {
        if (itemCount > 0 && targetMessageId == null) {
            val lastIdx = itemCount - 1
            listState.scrollToItem(lastIdx, Int.MAX_VALUE)
        }
    }

    // While streaming the last message, keep scrolling ONLY if user is already at bottom
    // Uses scrollToItem (instant) to avoid animation-vs-animation jitter
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            while (true) {
                if (isNearBottom && itemCount > 0) {
                    listState.scrollToItem(itemCount - 1)
                }
                delay(80)
            }
        }
    }
    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        items(state.messages, key = { it.id }, contentType = { it.role }) { message ->
            ChatBubble(
                message = message,
                onRegenerate = onRegenerate,
                isTarget = (message.id == targetMessageId),
                highlightQuery = searchQueryHighlight
            )
        }
        if (state.isLoading) item(key = "loading") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_gemini_mark),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Gemini is thinking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }
        }
        state.errorMessage?.let { message -> item(key = "error") { ErrorCard(message) } }
    }
}

@Composable
private fun MessageActions(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val haptic = rememberHapticTap()
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        IconButton(onClick = { haptic(); onCopy() }, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_clipboard),
                contentDescription = "Copy",
                tint = Color(0xFFB4B4B4),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = { haptic(); onShare() }, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_share_box),
                contentDescription = "Share",
                tint = Color(0xFFB4B4B4),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = { haptic(); onRegenerate() }, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = "Regenerate",
                tint = Color(0xFFB4B4B4),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onRegenerate: () -> Unit,
    isTarget: Boolean = false,
    highlightQuery: String? = null
) {
    val isUser = message.role == ChatRole.USER
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val timeLabel = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }
    // Highlight stays visible as long as isTarget is true (we never clear targetMessageId after search)
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isTarget) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "targetHighlight"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlightAlpha > 0.01f) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10A37F).copy(alpha = 0.22f * highlightAlpha))
                        .padding(6.dp)
                } else Modifier
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Icon(painterResource(R.drawable.ic_gemini_mark), null, Modifier.padding(bottom = 12.dp).size(24.dp))
            when (message.kind) {
                MessageKind.TEXT -> {
                    Box(Modifier.fillMaxWidth()) {
                        // Always pass highlightQuery when isTarget — keep highlight alive permanently
                        ResponseContent(message.text, highlightQuery = if (isTarget) highlightQuery else null)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MessageActions(
                            onCopy = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(message.text)) },
                            onShare = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, message.text)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onRegenerate = onRegenerate
                        )
                        Text(
                            timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6E6E6E),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                MessageKind.IMAGE -> GeneratedImageMessage(message)
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = 620.dp).padding(start = 32.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (message.attachments.isNotEmpty()) {
                    SentAttachmentGallery(message.attachments)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    SelectionContainer {
                        Text(
                            message.text,
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun TranscribingAnimation(modifier: Modifier = Modifier, text: String = "Transcribing...") {
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
            text,
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
    onStop: () -> Unit = {},
    onAttachmentsAdded: (List<PendingAttachment>) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
    onLiveVoiceStart: () -> Unit,
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

                // Send / Stop button
                if (state.isLoading) {
                    // ── Stop generating ──
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .background(Color.White, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(Color.Black, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                } else if (state.prompt.isNotBlank() || state.pendingAttachments.isNotEmpty()) {
                    // ── Send ──
                    IconButton(
                        onClick = onSend,
                        enabled = !state.isRestoring && !isListening,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
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
                } else {
                    // ── Live Voice (Decibel Icon replacing Send when empty) ──
                    IconButton(
                        onClick = onLiveVoiceStart,
                        enabled = !state.isRestoring && !isListening,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_audio_wave), 
                                contentDescription = "Live Voice",
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
fun ChatPanel(
    state: ChatUiState,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val title = stringResource(R.string.chat_history)
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHapticTap()

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

            // ── Top: logo + search icon (close option removed) ─────────────
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
                IconButton(
                    onClick = {
                        haptic()
                        onOpenSearch()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search chats",
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
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = SidebarText,
                    modifier = Modifier.size(22.dp),
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

// ── Full-Screen Search Overlay ───────────────────────────────────────────────

@Composable
private fun FullScreenSearch(
    state: ChatUiState,
    onClose: () -> Unit,
    onSelectChat: (chatId: String, targetMessageId: Long?, searchQuery: String?) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptic = rememberHapticTap()

    LaunchedEffect(Unit) {
        delay(150) // Allow slide-up animation to smoothly start before opening keyboard
        try {
            searchFocusRequester.requestFocus()
            keyboard?.show()
        } catch (_: Exception) {}
    }

    BackHandler {
        haptic()
        keyboard?.hide()
        onClose()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding() // CRITICAL: Moves search bar up cleanly above the keyboard
        ) {
            // ── Search Results List ──────────────────────────────────────────
            val filteredConversations = remember(searchQuery, state.conversations) {
                if (searchQuery.isBlank()) {
                    state.conversations
                } else {
                    state.conversations.filter { chat ->
                        chat.title.contains(searchQuery, ignoreCase = true) ||
                                chat.messages.any { it.text.contains(searchQuery, ignoreCase = true) }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredConversations, key = { it.id }) { chat ->
                    val matchingMsg = remember(chat, searchQuery) {
                        if (searchQuery.isBlank()) null
                        else chat.messages.find { it.text.contains(searchQuery, ignoreCase = true) }
                    }
                    val snippet = remember(chat, searchQuery, matchingMsg) {
                        if (searchQuery.isBlank()) {
                            chat.messages.lastOrNull()?.text?.replace("\n", " ")?.take(90) ?: ""
                        } else {
                            if (matchingMsg != null) {
                                val text = matchingMsg.text.replace("\n", " ")
                                val idx = text.indexOf(searchQuery, ignoreCase = true)
                                if (idx != -1) {
                                    val start = maxOf(0, idx - 15)
                                    val end = minOf(text.length, idx + searchQuery.length + 40)
                                    val prefix = if (start > 0) "..." else ""
                                    val suffix = if (end < text.length) "..." else ""
                                    prefix + text.substring(start, end) + suffix
                                } else {
                                    text.take(90)
                                }
                            } else {
                                chat.messages.lastOrNull()?.text?.replace("\n", " ")?.take(90) ?: ""
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic()
                                keyboard?.hide()
                                onSelectChat(chat.id, matchingMsg?.id, searchQuery.ifBlank { null })
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chat_bubble),
                                contentDescription = null,
                                tint = SidebarText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = SidebarText,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (snippet.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = snippet,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = SidebarSubText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (filteredConversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No matching results found",
                                style = MaterialTheme.typography.bodyMedium.copy(color = SidebarSubText)
                            )
                        }
                    }
                }
            }

            // ── Bottom Input Field & Dismiss Button ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search bar input container
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = SidebarSubText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = SidebarText),
                        singleLine = true,
                        cursorBrush = SolidColor(SidebarText),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search",
                                        style = MaterialTheme.typography.bodyLarge.copy(color = SidebarSubText)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                haptic()
                                searchQuery = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = SidebarSubText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Circular dismiss 'X' button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable {
                            haptic()
                            keyboard?.hide()
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close search",
                        tint = SidebarText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
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
