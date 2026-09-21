package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.DarkGradientEnd
import com.fahim.geminiApiComposeStarter.ui.theme.DarkGradientMid
import com.fahim.geminiApiComposeStarter.ui.theme.DarkGradientStart
import com.fahim.geminiApiComposeStarter.ui.theme.GradientEnd
import com.fahim.geminiApiComposeStarter.ui.theme.GradientMid
import com.fahim.geminiApiComposeStarter.ui.theme.GradientStart
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel, widthSizeClass: WindowWidthSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.let(viewModel::updateInput)
        }
    }
    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onInputChange = viewModel::updateInput,
        onSend = viewModel::sendMessage,
        onDismissError = viewModel::clearError,
        onCompactChange = viewModel::setCompactMessages,
        onClearHistory = viewModel::clearHistory,
        onToggleDarkMode = viewModel::toggleDarkMode,
        onVoice = {
            try {
                speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Gemini")
                })
            } catch (_: ActivityNotFoundException) {
                viewModel.reportError("Speech recognition is unavailable on this device.")
            } catch (_: SecurityException) {
                viewModel.reportError("Speech recognition could not start.")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissError: () -> Unit,
    onCompactChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onVoice: () -> Unit,
) {
    val listState = rememberLazyListState()
    val wide = widthSizeClass != WindowWidthSizeClass.Compact
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Light: soft pastel lavender → white  |  Dark: deep purple → black
    val bgGradient = if (isDark)
        Brush.linearGradient(listOf(DarkGradientStart, DarkGradientMid, DarkGradientEnd))
    else
        Brush.linearGradient(listOf(GradientStart, GradientMid, GradientEnd))

    // Content colors that adapt to the background
    val contentColor = if (isDark) Color.White else Color(0xFF2D0052)
    val subtleColor  = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B3FA0)
    val bubbleUser   = if (isDark) Color(0xFF6A1B9A).copy(alpha = 0.6f) else Color(0xFFE1D0F8)
    val bubbleBot    = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
    val inputBg      = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.75f)
    val iconTint     = if (isDark) Color.White else Color(0xFF6650A4)

    LaunchedEffect(state.messages.lastOrNull()?.id, state.isLoading) {
        val count = state.messages.size + if (state.isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(Modifier.fillMaxSize().background(bgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = contentColor,
                        actionIconContentColor = iconTint,
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color.White.copy(0.15f) else Color(0xFF7C3AED).copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null,
                                    tint = if (isDark) Color.White else Color(0xFF6650A4),
                                    modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Gemini Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor)
                                Text("N103 · Mobile Application Development",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subtleColor)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleDarkMode) {
                            Icon(
                                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = iconTint
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Compact", style = MaterialTheme.typography.labelSmall, color = subtleColor)
                            Spacer(Modifier.width(2.dp))
                            Switch(
                                checked = state.compactMessages,
                                onCheckedChange = onCompactChange,
                                modifier = Modifier.testTag("compactSwitch").height(28.dp),
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF7C3AED).copy(alpha = 0.5f),
                                    checkedThumbColor = if (isDark) Color.White else Color(0xFF6650A4),
                                )
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = onClearHistory,
                            enabled = state.messages.isNotEmpty() && !state.isLoading
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear chat",
                                tint = if (state.messages.isNotEmpty() && !state.isLoading)
                                    Color(0xFFE53935) else iconTint.copy(alpha = 0.3f))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier
                        .widthIn(max = if (wide) 840.dp else 600.dp)
                        .weight(1f)
                        .padding(horizontal = if (wide) 24.dp else 12.dp)
                ) {
                    if (state.messages.isEmpty() && !state.isLoading) {
                        // Empty state
                        Column(
                            Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                                    .background(if (isDark) Color.White.copy(0.10f) else Color(0xFF7C3AED).copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null,
                                    tint = if (isDark) Color.White else Color(0xFF6650A4),
                                    modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(24.dp))
                            Text("What would you like to explore?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            Text("Ask a question, work through an idea, or use your voice.\nYour conversation stays on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = subtleColor,
                                textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxWidth().testTag("messages"),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(if (state.compactMessages) 6.dp else 10.dp)
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                val isUser = message.isFromUser
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                                    Surface(
                                        modifier = Modifier.widthIn(max = if (wide) 560.dp else 280.dp),
                                        shape = RoundedCornerShape(
                                            topStart = 20.dp, topEnd = 20.dp,
                                            bottomStart = if (isUser) 20.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 20.dp
                                        ),
                                        color = if (isUser) bubbleUser else bubbleBot,
                                        shadowElevation = if (isDark) 0.dp else 1.dp
                                    ) {
                                        Column(Modifier.padding(
                                            horizontal = if (state.compactMessages) 12.dp else 16.dp,
                                            vertical = if (state.compactMessages) 8.dp else 12.dp
                                        )) {
                                            Text(
                                                if (isUser) "You" else "Gemini",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUser) Color(0xFF9C27B0) else Color(0xFF1565C0)
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = message.text.toBoldAnnotatedString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isDark) Color.White else Color(0xFF1A0033)
                                            )
                                        }
                                    }
                                }
                            }
                            if (state.isLoading) item(key = "loading") {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
                                        color = bubbleBot,
                                        shadowElevation = if (isDark) 0.dp else 1.dp
                                    ) {
                                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                Modifier.size(16.dp).testTag("loading"),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF7C3AED)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text("Gemini is thinking…",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = subtleColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error card
                    state.errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF5C0000).copy(0.9f) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("error"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(error, Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C))
                                TextButton(onClick = onDismissError) {
                                    Text("Dismiss",
                                        color = if (isDark) Color(0xFFFF8A80) else Color(0xFFE53935))
                                }
                            }
                        }
                    }

                    // Input bar
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = inputBg,
                        shadowElevation = if (isDark) 0.dp else 2.dp
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = state.input,
                                onValueChange = onInputChange,
                                modifier = Modifier.weight(1f).testTag("prompt"),
                                placeholder = {
                                    Text("Ask Gemini…",
                                        color = subtleColor,
                                        style = MaterialTheme.typography.bodyMedium)
                                },
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color.White else Color(0xFF1A0033)
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFF7C3AED),
                                )
                            )
                            IconButton(onClick = onVoice, enabled = !state.isLoading) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice input",
                                    tint = if (!state.isLoading) iconTint else iconTint.copy(alpha = 0.35f))
                            }
                            FilledIconButton(
                                onClick = onSend,
                                enabled = state.input.isNotBlank() && !state.isLoading && state.historyLoaded,
                                modifier = Modifier.testTag("send"),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF7C3AED),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF7C3AED).copy(alpha = 0.25f),
                                    disabledContentColor = Color.White.copy(alpha = 0.4f),
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
