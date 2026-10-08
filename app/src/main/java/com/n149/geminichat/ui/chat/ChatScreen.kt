package com.n149.geminichat.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n149.geminichat.R
import com.n149.geminichat.data.FeedbackEntity
import com.n149.geminichat.data.MessageEntity
import com.n149.geminichat.ui.feedback.FeedbackSheet
import com.n149.geminichat.ui.theme.DeepBlack
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Colour aliases ────────────────────────────────────────────────────────────
private val Black         = DeepBlack.Black
private val Surf          = DeepBlack.Surface
private val Elevated      = DeepBlack.Elevated
private val Accent        = DeepBlack.Accent
private val AccentGlow    = DeepBlack.AccentGlow
private val GeminiBg      = DeepBlack.GeminiBg
private val TextPri       = DeepBlack.TextPrimary
private val TextSec       = DeepBlack.TextSecondary
private val TextMut       = DeepBlack.TextMuted
private val ErrColor      = DeepBlack.Error
private val ErrBg         = DeepBlack.ErrorBg

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Error → Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            scope.launch {
                snackbarHost.showSnackbar(it, "Dismiss", duration = SnackbarDuration.Long)
                viewModel.dismissError()
            }
        }
    }

    // Voice launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            viewModel.onVoiceResult(spoken)
        } else viewModel.setListening(false)
    }

    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Black,
                drawerContentColor = TextPri,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                ChatHistorySidebar(
                    sessions = uiState.sessions,
                    currentSessionId = uiState.currentSessionId,
                    onNewChat = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onSelectSession = { id ->
                        viewModel.selectSession(id)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { id ->
                        viewModel.deleteSession(id)
                    },
                    onClearAll = {
                        viewModel.clearAllHistory()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Black,
            snackbarHost = {
                SnackbarHost(snackbarHost) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Elevated,
                        contentColor = TextPri,
                        actionColor = AccentGlow,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            topBar = {
                ChatTopBar(
                    topic = uiState.detectedTopic,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNewChat = { viewModel.startNewChat() },
                    onClearHistory = { viewModel.clearAllHistory() }
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText  = uiState.inputText,
                    isLoading  = uiState.isLoading,
                    isListening= uiState.isListening,
                    onInputChanged = viewModel::onInputChanged,
                    onSend     = { viewModel.sendMessage() },
                    onVoiceClick = {
                        viewModel.setListening(true)
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message…")
                        }
                        voiceLauncher.launch(intent)
                    }
                )
            }
        ) { padding ->
            val baseMod = Modifier.fillMaxSize().background(Black).padding(padding)
            if (isCompact) {
                MessageList(uiState, viewModel, baseMod)
            } else {
                Row(baseMod, horizontalArrangement = Arrangement.Center) {
                    MessageList(uiState, viewModel, Modifier.widthIn(max = 840.dp).fillMaxHeight())
                }
            }
        }
    }

    // ── Text-feedback bottom sheet ───────────────────────────────────────
    uiState.pendingFeedbackMessageId?.let { msgId ->
        val existing = uiState.feedbackMap[msgId]?.userText ?: ""
        FeedbackSheet(
            existingText = existing,
            onSubmit     = { txt -> viewModel.submitFeedback(msgId, txt) },
            onDismiss    = { viewModel.dismissFeedback() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    topic: String,
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    onClearHistory: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text("Gemini Chat", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPri)
                Text("N149 · $topic", fontSize = 11.sp, color = AccentGlow)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Chat History Sidebar",
                    tint = TextPri
                )
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, "New chat", tint = AccentGlow)
            }
            IconButton({ showMenu = true }) {
                Icon(Icons.Default.MoreVert, "More options", tint = TextSec)
            }
            DropdownMenu(showMenu, { showMenu = false }, Modifier.background(Elevated)) {
                DropdownMenuItem(
                    text = { Text("New chat", color = TextPri) },
                    onClick = { onNewChat(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Add, null, tint = AccentGlow) }
                )
                DropdownMenuItem(
                    text = { Text("Clear all history", color = ErrColor) },
                    onClick = { onClearHistory(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrColor) }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surf),
        modifier = Modifier.drawBehind {
            drawLine(Accent.copy(.4f), Offset(0f, size.height),
                Offset(size.width, size.height), 1.dp.toPx())
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat History Sidebar (Slidebar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHistorySidebar(
    sessions: List<ChatSessionItem>,
    currentSessionId: String,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(vertical = 16.dp)
    ) {
        // ── Drawer Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Accent, Color(0xFF3D2B8C)), radius = 60f)
                    ),
                Alignment.Center
            ) {
                Text("G", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Gemini Chat",
                    color = TextPri,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Student ID: N149 · AMOLED",
                    color = AccentGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Subtitle badge
        Surface(
            color = Elevated,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "⚡ gemini-3.5-flash-lite · Keystore AES-256",
                color = TextSec,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── + New Chat Button ─────────────────────────────────────────
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("New Chat", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(14.dp))

        HorizontalDivider(
            color = Surf,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // ── Section Title ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHAT HISTORY",
                color = TextMut,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (sessions.isNotEmpty()) {
                Surface(
                    color = Elevated,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${sessions.size}",
                        color = TextSec,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // ── Conversations List ─────────────────────────────────────────
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = TextMut,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No past chats yet",
                        color = TextSec,
                        fontSize = 13.sp
                    )
                    Text(
                        "Tap 'New Chat' to start one!",
                        color = TextMut,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    val isSelected = session.sessionId == currentSessionId
                    val bg = if (isSelected) Color(0xFF1E1838) else Color.Transparent
                    val borderStroke = if (isSelected) Accent.copy(alpha = 0.6f) else Color.Transparent

                    Surface(
                        onClick = { onSelectSession(session.sessionId) },
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = if (isSelected) AccentGlow else TextSec,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    color = if (isSelected) Color.White else TextPri,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${timeFormat.format(Date(session.timestamp))} · ${session.messageCount} msg",
                                    color = TextMut,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSession(session.sessionId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete chat",
                                    tint = TextMut,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Drawer Footer ───────────────────────────────────────────────
        HorizontalDivider(
            color = Surf,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showClearDialog = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = ErrColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Clear All History",
                color = ErrColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Elevated,
            titleContentColor = TextPri,
            textContentColor = TextSec,
            title = { Text("Clear All History?") },
            text = { Text("This will permanently remove all past chat conversations and feedback.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text("Clear All", color = ErrColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSec)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    uiState: ChatUiState,
    viewModel: ChatViewModel,
    modifier: Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty() || uiState.isLoading) {
            val idx = if (uiState.isLoading) uiState.messages.size else uiState.messages.size - 1
            listState.animateScrollToItem(maxOf(0, idx))
        }
    }

    if (uiState.messages.isEmpty() && !uiState.isLoading) {
        EmptyHint(modifier); return
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        uiState.messages.groupByDate().forEach { (label, msgs) ->
            item(key = "hdr_$label") { DateDivider(label) }
            items(msgs, key = { it.id }) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically { it / 2 } + fadeIn()
                ) {
                    MessageBubble(
                        message       = msg,
                        feedback      = uiState.feedbackMap[msg.id],
                        onFeedback    = { viewModel.openFeedback(msg.id) },
                        onDeleteFeedback = { viewModel.deleteFeedback(msg.id) }
                    )
                }
            }
        }
        if (uiState.isLoading) item(key = "loading") { LoadingBubble() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date grouping helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun List<MessageEntity>.groupByDate(): Map<String, List<MessageEntity>> {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val today = sdf.format(Date())
    val yesterday = sdf.format(Date(System.currentTimeMillis() - 86_400_000L))
    return groupBy { msg ->
        when (val l = sdf.format(Date(msg.timestamp))) {
            today     -> "Today"
            yesterday -> "Yesterday"
            else      -> l
        }
    }
}

@Composable
private fun DateDivider(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f), color = DeepBlack.Divider)
        Text(label, Modifier.padding(horizontal = 10.dp),
            color = TextMut, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(Modifier.weight(1f), color = DeepBlack.Divider)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyHint(modifier: Modifier) {
    Column(modifier, Arrangement.Center, Alignment.CenterHorizontally) {
        Box(
            Modifier.size(80.dp).clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Accent.copy(.3f), Color.Transparent))
                ),
            Alignment.Center
        ) { Text("✦", fontSize = 36.sp, color = Accent) }
        Spacer(Modifier.height(20.dp))
        Text("Start a conversation", fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, color = TextPri)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask Gemini anything.\nAfter each answer you can leave\ntext feedback to personalise future replies.",
            fontSize = 14.sp, color = TextSec, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: MessageEntity,
    feedback: FeedbackEntity?,
    onFeedback: () -> Unit,
    onDeleteFeedback: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeStr = timeFmt.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Gemini avatar
            if (!isUser) {
                Avatar("G", listOf(Accent, Color(0xFF1A0055)))
                Spacer(Modifier.width(8.dp))
            }

            Column(
                Modifier.widthIn(max = 290.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // ── Bubble ────────────────────────────────────────────
                if (message.errorMessage != null) {
                    ErrBubble(message.errorMessage, isUser)
                } else if (isUser) {
                    UserBubble(message.content)
                } else {
                    GeminiBubble(message.content)
                }

                // ── Timestamp ─────────────────────────────────────────
                Text(timeStr, color = TextMut, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }

            // User avatar
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Avatar("N", listOf(Color(0xFF303F9F), Color(0xFF1A237E)))
            }
        }

        // ── Feedback section (Gemini bubbles only) ────────────────────
        if (!isUser && message.errorMessage == null) {
            Spacer(Modifier.height(4.dp))
            FeedbackRow(
                feedback      = feedback,
                onFeedback    = onFeedback,
                onDeleteFeedback = onDeleteFeedback
            )
        }
    }
}

// ─── Bubble variants ──────────────────────────────────────────────────────────

@Composable
private fun UserBubble(content: String) {
    Box(
        Modifier.clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF5B4FCF), Color(0xFF3D2B8C)),
                    Offset(0f, 0f), Offset(200f, 100f))
            )
    ) {
        Text(content, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun GeminiBubble(content: String) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
            .background(GeminiBg)
    ) {
        Text(content, color = DeepBlack.GeminiText, fontSize = 14.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun ErrBubble(errorMessage: String, isUser: Boolean) {
    Surface(
        shape = RoundedCornerShape(
            topStart = if (isUser) 18.dp else 4.dp,
            topEnd = if (isUser) 4.dp else 18.dp,
            bottomStart = 18.dp, bottomEnd = 18.dp
        ),
        color = ErrBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrColor.copy(.4f))
    ) {
        Row(Modifier.padding(14.dp, 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Warning, null, tint = ErrColor, modifier = Modifier.size(15.dp))
            Text(errorMessage, color = ErrColor, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feedback row — appears below each Gemini bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedbackRow(
    feedback: FeedbackEntity?,
    onFeedback: () -> Unit,
    onDeleteFeedback: () -> Unit
) {
    // Indent to align with bubble (avatar width + gap)
    Column(Modifier.padding(start = 38.dp)) {
        if (feedback == null) {
            // ── No feedback yet → "Leave feedback" link ───────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFeedback() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = "Leave feedback",
                    tint = TextMut,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Leave feedback", color = TextMut, fontSize = 11.sp)
            }
        } else {
            // ── Saved feedback note card ──────────────────────────────
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0E0E1C),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Accent.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(Modifier.padding(10.dp, 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Your feedback",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGlow,
                            letterSpacing = 0.5.sp
                        )
                        Row {
                            // Edit
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit feedback",
                                tint = TextMut,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onFeedback() }
                            )
                            Spacer(Modifier.width(10.dp))
                            // Delete
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete feedback",
                                tint = TextMut,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onDeleteFeedback() }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        feedback.userText,
                        color = TextSec,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingBubble() {
    val inf = rememberInfiniteTransition(label = "dots")
    val alphas = (0..2).map { i ->
        inf.animateFloat(0.3f, 1f,
            infiniteRepeatable(tween(600, i * 200), RepeatMode.Reverse),
            label = "d$i"
        ).value
    }
    Row(Modifier.fillMaxWidth(), Arrangement.Start, Alignment.Bottom) {
        Avatar("G", listOf(Accent, Color(0xFF1A0055)))
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                .background(GeminiBg).padding(18.dp, 14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                alphas.forEach { a ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccentGlow.copy(a)))
                }
                Spacer(Modifier.width(4.dp))
                Text("Gemini is thinking…", color = TextSec, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    isListening: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), color = Surf, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(stringResource(R.string.input_hint), color = TextMut, fontSize = 14.sp)
                },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPri, unfocusedTextColor = TextPri,
                    cursorColor = AccentGlow,
                    focusedBorderColor = Accent, unfocusedBorderColor = DeepBlack.Divider,
                    focusedContainerColor = Elevated, unfocusedContainerColor = Elevated
                )
            )
            CircleBtn(
                onClick = onVoiceClick, enabled = !isLoading,
                brush = if (isListening)
                    Brush.linearGradient(listOf(ErrColor, Color(0xFF8B0000)))
                else
                    Brush.linearGradient(listOf(Color(0xFF1C1C2E), Color(0xFF0D0D1A)))
            ) {
                Icon(
                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    stringResource(R.string.voice_input),
                    tint = if (isListening) Color.White else AccentGlow,
                    modifier = Modifier.size(22.dp)
                )
            }
            val canSend = inputText.isNotBlank() && !isLoading
            CircleBtn(
                onClick = onSend, enabled = canSend,
                brush = if (canSend)
                    Brush.linearGradient(listOf(Accent, Color(0xFF3D2B8C)))
                else
                    Brush.linearGradient(listOf(Elevated, Elevated))
            ) {
                if (isLoading)
                    CircularProgressIndicator(Modifier.size(20.dp), color = AccentGlow, strokeWidth = 2.dp)
                else
                    Icon(Icons.Default.Send, stringResource(R.string.send),
                        tint = if (canSend) Color.White else TextMut, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Avatar(label: String, gradient: List<Color>) {
    Box(
        Modifier.size(30.dp).clip(CircleShape)
            .background(Brush.linearGradient(gradient, Offset(0f, 0f), Offset(60f, 60f))),
        Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
private fun CircleBtn(
    onClick: () -> Unit,
    enabled: Boolean,
    brush: Brush,
    content: @Composable () -> Unit
) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(brush), Alignment.Center) {
        IconButton(onClick = onClick, enabled = enabled) { content() }
    }
}
