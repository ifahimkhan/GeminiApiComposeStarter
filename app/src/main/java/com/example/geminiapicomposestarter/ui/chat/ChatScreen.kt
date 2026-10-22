package com.example.geminiapicomposestarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geminiapicomposestarter.data.ChatMessage
import com.example.geminiapicomposestarter.data.Participant
import com.example.geminiapicomposestarter.ui.theme.ModelBubbleDark
import com.example.geminiapicomposestarter.ui.theme.ModelBubbleLight
import com.example.geminiapicomposestarter.ui.theme.UserBubbleDark
import com.example.geminiapicomposestarter.ui.theme.UserBubbleLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // STT (Speech-to-Text) Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            spokenText?.let {
                viewModel.updatePrompt(it)
            }
        }
    }

    // Auto-scroll to newest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        val horizontalPadding = if (isTablet) 32.dp else 12.dp
        val maxContentWidth = if (isTablet) 720.dp else maxWidth

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Gemini Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gatha Jain (N038)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Dark mode",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Switch(
                                checked = uiState.isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) },
                                modifier = Modifier.testTag("darkModeSwitch")
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = maxContentWidth)
                        .fillMaxSize()
                ) {
                    // Chat messages list
                    if (uiState.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("emptyChatBox"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start a conversation with Gemini!\nAsk questions, explore ideas, or use voice input.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding)
                                .testTag("chatMessagesList"),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                ChatMessageBubble(
                                    message = message,
                                    isDarkMode = uiState.isDarkMode
                                )
                            }
                        }
                    }

                    // Loading indicator
                    AnimatedVisibility(
                        visible = uiState.isLoading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("loadingIndicator")
                        )
                    }

                    // Bottom Input Area
                    ChatInputBar(
                        prompt = uiState.currentPrompt,
                        isLoading = uiState.isLoading,
                        onPromptChange = { viewModel.updatePrompt(it) },
                        onSend = { viewModel.sendMessage(uiState.currentPrompt) },
                        onVoiceInput = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {
                                // Handled gracefully if speech recognition not available
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.participant == Participant.USER

    val bubbleColor = if (isUser) {
        if (isDarkMode) UserBubbleDark else UserBubbleLight
    } else {
        if (isDarkMode) ModelBubbleDark else ModelBubbleLight
    }

    val textColor = if (isUser) {
        if (isDarkMode) Color.White else Color(0xFF1D192B)
    } else {
        if (isDarkMode) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isUser) "userMessageBubble" else "modelMessageBubble"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Gemini AI",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun ChatInputBar(
    prompt: String,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            placeholder = { Text("Enter your prompt here") },
            modifier = Modifier
                .weight(1f)
                .testTag("promptInputField")
                .semantics { contentDescription = "Enter your prompt here" },
            shape = RoundedCornerShape(24.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Speech-to-Text Button
        IconButton(
            onClick = onVoiceInput,
            modifier = Modifier
                .testTag("voiceInputButton")
                .semantics { contentDescription = "Voice Input" }
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Send Button
        IconButton(
            onClick = onSend,
            enabled = prompt.isNotBlank() && !isLoading,
            modifier = Modifier
                .testTag("sendButton")
                .semantics { contentDescription = "Send prompt" }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send prompt",
                tint = if (prompt.isNotBlank() && !isLoading) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}
