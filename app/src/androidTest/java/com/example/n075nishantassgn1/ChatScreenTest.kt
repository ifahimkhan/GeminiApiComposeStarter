package com.example.n075nishantassgn1.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    messageText: String,
    onMessageTextChange: (String) -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    val listState = rememberLazyListState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!spokenText.isNullOrBlank()) {
            onMessageTextChange(spokenText)
        }
    }

    // Automatically scroll to the newest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                uiState.messages.lastIndex
            )
        }
    }

    // Show errors using Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gemini Chat",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "N075 • Nishant Parwani",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        },

        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },

        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                isLoading = uiState.isLoading,
                onMessageTextChange = onMessageTextChange,

                onVoiceClick = {

                    val intent = Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    ).apply {

                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )

                        putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "Speak your question"
                        )
                    }

                    voiceLauncher.launch(intent)
                },

                onSendClick = {

                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        onMessageTextChange("")
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            state = listState,

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),

            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Spacer(
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Welcome screen when there are no messages
            if (uiState.messages.isEmpty()) {
                item {
                    WelcomeMessage()
                }
            }

            // Chat messages
            items(
                items = uiState.messages,
                key = { message ->
                    message.id
                }
            ) { message ->

                ChatBubble(
                    message = message
                )
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    LoadingBubble()
                }
            }

            item {
                Spacer(
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}


@Composable
fun WelcomeMessage() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 100.dp,
                bottom = 24.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Hello, Nishant 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp)
        )

        Text(
            text = "Ask anything using text or voice.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


@Composable
fun ChatBubble(
    message: ChatMessage
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(0.86f),

            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,

                bottomStart = if (message.isUser) {
                    20.dp
                } else {
                    4.dp
                },

                bottomEnd = if (message.isUser) {
                    4.dp
                } else {
                    20.dp
                }
            ),

            colors = CardDefaults.cardColors(

                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = if (message.isUser) {
                        "You"
                    } else {
                        "Gemini"
                    },

                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },

                    style = MaterialTheme.typography.labelMedium,

                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = message.text,

                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },

                    style = MaterialTheme.typography.bodyLarge,

                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}


@Composable
fun LoadingBubble() {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.Start
    ) {

        Card(
            shape = RoundedCornerShape(18.dp),

            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    modifier = Modifier.width(18.dp),
                    strokeWidth = 2.dp
                )

                Text(
                    text = "Gemini is thinking...",

                    style = MaterialTheme.typography.bodyMedium,

                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}


@Composable
fun ChatInputBar(
    messageText: String,
    isLoading: Boolean,
    onMessageTextChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit
) {

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,

        tonalElevation = 6.dp,

        modifier = Modifier.navigationBarsPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = messageText,

                onValueChange = onMessageTextChange,

                modifier = Modifier.weight(1f),

                placeholder = {
                    Text("Ask Gemini...")
                },

                enabled = !isLoading,

                maxLines = 4,

                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = onVoiceClick,

                enabled = !isLoading
            ) {

                Icon(
                    imageVector = Icons.Default.Mic,

                    contentDescription = "Voice input"
                )
            }

            IconButton(
                onClick = onSendClick,

                enabled =
                    messageText.isNotBlank() &&
                            !isLoading
            ) {

                Icon(
                    imageVector = Icons.Default.Send,

                    contentDescription = "Send message"
                )
            }
        }
    }
}