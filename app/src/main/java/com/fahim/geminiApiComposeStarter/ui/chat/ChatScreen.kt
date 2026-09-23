@file:OptIn(
    androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

import kotlinx.coroutines.launch


// =====================================================
// CHAT ROUTE
// Connects ViewModel with ChatScreen
// =====================================================

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    ChatScreen(
        state = state,

        onPromptChange = viewModel::onPromptChange,

        onSend = viewModel::onSend,

        onResponseStyleChange = viewModel::onResponseStyleChange,

        onClearChat = viewModel::clearChatHistory,

        onExportChat = {

            coroutineScope.launch {

                try {

                    val chatText = viewModel.exportChatHistory()

                    if (chatText == "No chat history available.") {

                        Toast.makeText(
                            context,
                            chatText,
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        val shareIntent = Intent(
                            Intent.ACTION_SEND
                        ).apply {

                            type = "text/plain"

                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "Gemini Chat History"
                            )

                            putExtra(
                                Intent.EXTRA_TEXT,
                                chatText
                            )
                        }

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Export Chat History"
                            )
                        )
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        context,
                        "Unable to export chat history",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },

        isDarkTheme = isDarkTheme,

        onThemeToggle = onThemeToggle
    )
}


// =====================================================
// CHAT SCREEN
// =====================================================

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onResponseStyleChange: (String) -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as? Activity

    val windowSizeClass = activity?.let {
        calculateWindowSizeClass(it)
    }

    val isCompact =
        windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact

    val isMedium =
        windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium

    val isExpanded =
        windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    val horizontalPadding = when {
        isExpanded -> 48.dp
        isMedium -> 32.dp
        else -> 16.dp
    }

    val contentMaxWidth = when {
        isExpanded -> 900.dp
        isMedium -> 700.dp
        else -> 600.dp
    }

    val listState = rememberLazyListState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    var showClearDialog by remember {
        mutableStateOf(false)
    }


    // =================================================
    // VOICE INPUT
    // =================================================

    val voiceLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val results =
                    result.data?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )

                val spokenText = results?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    onPromptChange(spokenText)
                }
            }
        }


    // =================================================
    // AUTO SCROLL
    // =================================================

    LaunchedEffect(
        state.messages.size,
        state.isLoading
    ) {

        if (state.messages.isNotEmpty()) {

            val lastIndex =
                if (state.isLoading) {
                    state.messages.size
                } else {
                    state.messages.lastIndex
                }

            listState.animateScrollToItem(lastIndex)
        }
    }


    // =================================================
    // ERROR SNACKBAR
    // =================================================

    LaunchedEffect(state.errorMessage) {

        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }


    // =================================================
    // CLEAR CHAT CONFIRMATION DIALOG
    // =================================================

    if (showClearDialog) {

        AlertDialog(

            onDismissRequest = {
                showClearDialog = false
            },

            title = {
                Text("Clear Chat History?")
            },

            text = {
                Text(
                    "Are you sure you want to delete all messages? " +
                            "This action cannot be undone."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        onClearChat()
                        showClearDialog = false
                    }
                ) {

                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }


    // =====================================================
    // MAIN SCAFFOLD
    // =====================================================

    Scaffold(

        modifier = Modifier
            .fillMaxSize()
            .imePadding(),

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {

            Column(

                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
                    .padding(innerPadding)
                    .padding(horizontal = horizontalPadding)

            ) {


                // =====================================
                // HEADER
                // =====================================

                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (isCompact) 16.dp else 24.dp,
                            bottom = 8.dp
                        ),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    Text(
                        text = "Gemini Chat",

                        style =
                            if (isExpanded) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                MaterialTheme.typography.headlineSmall
                            }
                    )


                    // Theme + Export + Clear buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Theme Toggle
                        IconButton(
                            onClick = onThemeToggle
                        ) {

                            Text(
                                text = if (isDarkTheme) {
                                    "☀️"
                                } else {
                                    "🌙"
                                },
                                fontSize = 22.sp
                            )
                        }


                        // Export Button
                        TextButton(
                            onClick = onExportChat,

                            enabled = state.messages.isNotEmpty() &&
                                    !state.isLoading

                        ) {

                            Text("Export")
                        }


                        // Clear Button
                        TextButton(

                            onClick = {
                                showClearDialog = true
                            },

                            enabled = state.messages.isNotEmpty() &&
                                    !state.isLoading

                        ) {

                            Text(
                                text = "Clear",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }


                // =====================================
                // RESPONSE STYLE SELECTOR
                // =====================================

                ResponseStyleSelector(

                    selectedStyle = state.responseStyle,

                    onStyleSelected = onResponseStyleChange
                )


                // =====================================
                // CHAT AREA
                // =====================================

                Box(

                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()

                ) {

                    if (state.messages.isEmpty()) {

                        Box(

                            modifier = Modifier.fillMaxSize(),

                            contentAlignment = Alignment.Center

                        ) {

                            Text(
                                text = "Start a conversation with Gemini",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                    } else {

                        LazyColumn(

                            modifier = Modifier.fillMaxSize(),

                            state = listState,

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)

                        ) {

                            items(

                                items = state.messages,

                                key = { message ->
                                    message.id
                                }

                            ) { message ->

                                ChatBubble(
                                    message = message,
                                    isExpanded = isExpanded
                                )
                            }


                            // Loading indicator
                            if (state.isLoading) {

                                item(
                                    key = "loading"
                                ) {

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement.Start

                                    ) {

                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                // =====================================
                // PROMPT BAR
                // =====================================

                PromptBar(

                    prompt = state.prompt,

                    promptError = state.promptError,

                    enabled = !state.isLoading,

                    onPromptChange = onPromptChange,

                    onSend = onSend,

                    onVoiceClick = {

                        val voiceIntent =
                            Intent(
                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                            ).apply {

                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )

                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE,
                                    "en-IN"
                                )

                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Speak your question"
                                )
                            }

                        if (
                            voiceIntent.resolveActivity(
                                context.packageManager
                            ) != null
                        ) {

                            voiceLauncher.launch(voiceIntent)
                        }
                    }
                )
            }
        }
    }
}


// =====================================================
// RESPONSE STYLE SELECTOR
// =====================================================

@Composable
private fun ResponseStyleSelector(
    selectedStyle: String,
    onStyleSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text("Response style:")

            TextButton(
                onClick = {
                    expanded = true
                }
            ) {

                Text(selectedStyle)
            }
        }


        DropdownMenu(

            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }

        ) {

            DropdownMenuItem(

                text = {
                    Text(UserPreferencesRepository.CONCISE)
                },

                onClick = {

                    onStyleSelected(
                        UserPreferencesRepository.CONCISE
                    )

                    expanded = false
                }
            )


            DropdownMenuItem(

                text = {
                    Text(UserPreferencesRepository.NORMAL)
                },

                onClick = {

                    onStyleSelected(
                        UserPreferencesRepository.NORMAL
                    )

                    expanded = false
                }
            )


            DropdownMenuItem(

                text = {
                    Text(UserPreferencesRepository.DETAILED)
                },

                onClick = {

                    onStyleSelected(
                        UserPreferencesRepository.DETAILED
                    )

                    expanded = false
                }
            )
        }
    }
}


// =====================================================
// CHAT BUBBLE
// =====================================================

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isExpanded: Boolean
) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (message.isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }

    ) {

        Card(

            modifier = Modifier.widthIn(

                max =
                    if (isExpanded) {
                        600.dp
                    } else {
                        320.dp
                    }
            ),

            shape = RoundedCornerShape(16.dp),

            colors = CardDefaults.cardColors(

                containerColor =
                    if (message.isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
            )

        ) {

            Text(

                text = message.text,

                modifier = Modifier.padding(12.dp),

                fontSize = 16.sp
            )
        }
    }
}


// =====================================================
// PROMPT BAR
// =====================================================

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 8.dp,
                bottom = 12.dp
            ),

        verticalAlignment = Alignment.CenterVertically

    ) {

        OutlinedTextField(

            value = prompt,

            onValueChange = onPromptChange,

            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),

            label = {
                Text("Enter your prompt")
            },

            minLines = 2,

            enabled = enabled,

            isError = promptError != null,

            supportingText =
                if (promptError != null) {

                    {
                        Text("Message cannot be empty")
                    }

                } else {
                    null
                }
        )


        IconButton(

            onClick = onVoiceClick,

            enabled = enabled

        ) {

            Text(
                text = "🎤",
                fontSize = 22.sp
            )
        }


        FilledTonalButton(

            onClick = onSend,

            enabled = enabled && prompt.isNotBlank()

        ) {

            Text("Send")
        }
    }
}


// =====================================================
// PREVIEW
// =====================================================

@Composable
private fun ChatScreenPreview() {

    GeminiApiComposeStarterTheme {

        ChatScreen(

            state = ChatUiState(

                responseStyle =
                    UserPreferencesRepository.NORMAL,

                messages = listOf(

                    ChatMessage(
                        id = "1",
                        text = "Hello! How can I help you?",
                        isUser = false
                    ),

                    ChatMessage(
                        id = "2",
                        text = "Explain Android in simple words.",
                        isUser = true
                    ),

                    ChatMessage(
                        id = "3",
                        text = "Android is a mobile operating system.",
                        isUser = false
                    )
                )
            ),

            onPromptChange = {},

            onSend = {},

            onResponseStyleChange = {},

            onClearChat = {},

            onExportChat = {},

            isDarkTheme = false,

            onThemeToggle = {}
        )
    }
}