package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitBorder
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitBotBubble
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitCyan
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitCyanSubtle
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitSurface
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitSurfaceElevated
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitTextPrimary
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitTextSecondary
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitUltraviolet
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitUserBubbleEnd
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitUserBubbleStart
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitVoiceActive
import com.fahim.geminiApiComposeStarter.ui.theme.OrbitVoid
import com.fahim.geminiApiComposeStarter.ui.voice.VoiceSpeechManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val voiceManager = remember {
        VoiceSpeechManager(
            context = context,
            onResult = { text -> viewModel.onVoiceResult(text) },
            onListeningStateChanged = { isListening -> viewModel.setVoiceListening(isListening) },
            onError = { err -> viewModel.onVoiceError(err) }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopListening()
        }
    }

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onClearChat = viewModel::clearChat,
        onStartVoice = {
            voiceManager.startListening()
        },
        onStopVoice = {
            voiceManager.stopListening()
        },
        onDismissError = viewModel::clearErrorMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onDismissError: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartVoice()
        } else {
            Toast.makeText(context, context.getString(R.string.mic_permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Reset Orbital Session", color = OrbitTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear transmission history? Local message logs will be purged.", color = OrbitTextSecondary) },
            containerColor = OrbitSurfaceElevated,
            confirmButton = {
                TextButton(onClick = {
                    onClearChat()
                    showClearDialog = false
                }) {
                    Text("Purge", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = OrbitCyan)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(OrbitCyan, OrbitUltraviolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦",
                                color = OrbitVoid,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Gemini Orbit",
                                    color = OrbitTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OrbitCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "N104",
                                        color = OrbitCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Abhay Telrandhe • Orbit 1.5 Active",
                                color = OrbitTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = OrbitTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrbitVoid
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = OrbitVoid
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Voice Active Banner
            AnimatedVisibility(
                visible = state.isListeningVoice,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = OrbitVoiceActive.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrbitVoiceActive.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pulseAnim = rememberInfiniteTransition(label = "voicePulse")
                        val scale by pulseAnim.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(OrbitVoiceActive)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.voice_listening),
                            color = OrbitTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Message Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    EmptyOrbitWelcome()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            OrbitMessageBubble(message = message)
                        }

                        if (state.isLoading) {
                            item {
                                OrbitLoadingIndicator()
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            OrbitInputBar(
                prompt = state.prompt,
                enabled = !state.isLoading,
                isListening = state.isListeningVoice,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onMicClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPerm) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (state.isListeningVoice) {
                            onStopVoice()
                        } else {
                            onStartVoice()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyOrbitWelcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowScale"
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            OrbitCyan.copy(alpha = 0.3f),
                            OrbitUltraviolet.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, OrbitCyan.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🪐",
                fontSize = 44.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Gemini Orbit AI",
            color = OrbitTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Abhay Telrandhe • Roll No. N104\nOrbital communication protocol online. Ask a question, request code architecture, or tap the microphone to transmit voice.",
            color = OrbitTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun OrbitMessageBubble(message: ChatMessage) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!message.isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Text(
                    text = "✦ Orbit Core",
                    color = OrbitCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            shape = if (message.isUser) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
            },
            color = if (message.isUser) Color.Transparent else OrbitBotBubble,
            border = if (!message.isUser) androidx.compose.foundation.BorderStroke(1.dp, OrbitBorder) else null,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .then(
                    if (message.isUser) {
                        Modifier.background(
                            Brush.horizontalGradient(listOf(OrbitUserBubbleStart, OrbitUserBubbleEnd)),
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                        )
                    } else Modifier
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    color = OrbitTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        color = if (message.isUser) Color.White.copy(alpha = 0.7f) else OrbitTextSecondary,
                        fontSize = 11.sp
                    )

                    if (!message.isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = OrbitTextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Orbit Transmission", message.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(R.string.copy_copied), Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitLoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = OrbitBotBubble,
            border = androidx.compose.foundation.BorderStroke(1.dp, OrbitBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = OrbitCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Orbit AI is formulating transmission…",
                    color = OrbitCyanSubtle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OrbitInputBar(
    prompt: String,
    enabled: Boolean,
    isListening: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Surface(
        color = OrbitSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, OrbitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Voice Button
            val micColor by animateColorAsState(
                targetValue = if (isListening) OrbitVoiceActive else OrbitCyan,
                label = "micColor"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(micColor.copy(alpha = if (isListening) 0.25f else 0.12f))
                    .clickable(enabled = enabled, onClick = onMicClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = micColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text input
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                enabled = enabled,
                placeholder = {
                    Text(
                        text = stringResource(R.string.enter_your_prompt_here),
                        color = OrbitTextSecondary,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OrbitSurfaceElevated,
                    unfocusedContainerColor = OrbitSurfaceElevated,
                    disabledContainerColor = OrbitSurfaceElevated.copy(alpha = 0.5f),
                    focusedBorderColor = OrbitCyan,
                    unfocusedBorderColor = OrbitBorder,
                    focusedTextColor = OrbitTextPrimary,
                    unfocusedTextColor = OrbitTextPrimary
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Send Button
            val canSend = prompt.isNotBlank() && enabled
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            Brush.linearGradient(listOf(OrbitCyan, OrbitUltraviolet))
                        } else {
                            Brush.linearGradient(listOf(OrbitBorder, OrbitBorder))
                        }
                    )
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = if (canSend) OrbitVoid else OrbitTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
