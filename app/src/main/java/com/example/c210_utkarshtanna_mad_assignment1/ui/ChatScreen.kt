package com.example.c210_utkarshtanna_mad_assignment1.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c210_utkarshtanna_mad_assignment1.ChatMessage
import com.example.c210_utkarshtanna_mad_assignment1.ChatViewModel
import com.example.c210_utkarshtanna_mad_assignment1.TextFormatter
import com.example.c210_utkarshtanna_mad_assignment1.ui.theme.AiBubbleColor
import com.example.c210_utkarshtanna_mad_assignment1.ui.theme.AppPurple
import com.example.c210_utkarshtanna_mad_assignment1.ui.theme.AppTeal
import com.example.c210_utkarshtanna_mad_assignment1.ui.theme.UserBubbleColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()

    // Speech-to-text launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.setVoicePrompt(spokenText)
            }
        }
    }

    // Scroll to bottom when new messages arrive or loading state changes
    LaunchedEffect(messages.size, isLoading) {
        val totalCount = messages.size + if (isLoading) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "C210 - Utkarsh Tanna",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Abandon All Critical Thinking, Ye Who Enter Here.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppTeal
                            )
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            color = AppPurple,
                            strokeWidth = 2.5.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubbleItem(message = message)
                }

                if (isLoading) {
                    item(key = "loading_bubble") {
                        AiLoadingBubble()
                    }
                }
            }

            // Bottom input bar
            ChatInputBar(
                inputText = inputText,
                errorMessage = errorMessage,
                onTextChanged = { viewModel.onInputTextChanged(it) },
                onSendClicked = { viewModel.sendMessage() },
                onVoiceClicked = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                    }
                    voiceLauncher.launch(intent)
                }
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleColor = if (isUser) UserBubbleColor else AiBubbleColor
    val textColor = if (isUser) Color.White else Color(0xFFE0E0E0)

    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = TextFormatter.formatToAnnotatedString(message.text),
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun AiLoadingBubble(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AiBubbleColor,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AppTeal
                )
                Text(
                    text = "Gemini is thinking...",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    errorMessage: String?,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onVoiceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = "Ask if you must...",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    },
                    isError = errorMessage != null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPurple,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    maxLines = 4,
                    singleLine = false
                )

                // Voice mic button
                IconButton(
                    onClick = onVoiceClicked,
                    modifier = Modifier
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = AppTeal
                    )
                }

                // Send button
                IconButton(
                    onClick = onSendClicked,
                    modifier = Modifier
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = AppPurple
                    )
                }
            }

            // Inline error message if any
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }
        }
    }
}
