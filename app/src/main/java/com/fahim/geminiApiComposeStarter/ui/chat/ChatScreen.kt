package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatRoute(
    viewModel: ChatViewModel
) {
    val state by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    val context = LocalContext.current

    val windowSizeClass =
        calculateWindowSizeClass(
            activity = context as Activity
        )

    ChatScreen(
        state = state,

        windowWidthSizeClass =
            windowSizeClass.widthSizeClass,

        onPromptChange =
            viewModel::onPromptChange,

        onSend =
            viewModel::onSend,

        onShowUserMessagesChange =
            viewModel::setShowUserMessages
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,

    windowWidthSizeClass:
    WindowWidthSizeClass =
        WindowWidthSizeClass.Compact,

    onPromptChange: (String) -> Unit,

    onSend: () -> Unit,

    onShowUserMessagesChange:
        (Boolean) -> Unit = {},
) {

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val listState =
        rememberLazyListState()

    val speechLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartActivityForResult()
        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                        )
                        ?.firstOrNull()

                if (
                    !spokenText.isNullOrBlank()
                ) {
                    onPromptChange(
                        spokenText
                    )
                }
            }
        }

    LaunchedEffect(
        state.errorMessage
    ) {

        state.errorMessage?.let {
            snackbarHostState
                .showSnackbar(it)
        }
    }

    LaunchedEffect(
        state.messages.size
    ) {

        if (
            state.messages.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                state.messages.lastIndex
            )
        }
    }

    val horizontalPadding =
        when (windowWidthSizeClass) {

            WindowWidthSizeClass.Compact ->
                16.dp

            WindowWidthSizeClass.Medium ->
                48.dp

            WindowWidthSizeClass.Expanded ->
                120.dp

            else ->
                16.dp
        }

    Scaffold(

        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),

        snackbarHost = {
            SnackbarHost(
                snackbarHostState
            )
        }

    ) { innerPadding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal =
                            horizontalPadding,
                        vertical = 12.dp
                    )
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        "Gemini Assistant",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "My messages",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        modifier =
                            Modifier.padding(
                                end = 8.dp
                            )
                    )

                    Switch(
                        checked =
                            state.showUserMessages,

                        onCheckedChange =
                            onShowUserMessagesChange
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(12.dp)
            )

            if (
                state.messages.isEmpty()
            ) {

                Box(

                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "Start a conversation with Gemini",

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )
                }

            } else {

                LazyColumn(

                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),

                    state =
                        listState,

                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    items(

                        items =
                            state.messages,

                        key = {
                                message ->
                            message.id
                        }

                    ) { message ->

                        ChatBubble(
                            message = message
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.size(8.dp)
            )

            PromptBar(

                prompt =
                    state.prompt,

                promptError =
                    state.promptError,

                enabled =
                    !state.isLoading,

                onPromptChange =
                    onPromptChange,

                onSend =
                    onSend,

                onVoiceClick = {

                    val intent =
                        Intent(
                            RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                        ).apply {

                            putExtra(
                                RecognizerIntent
                                    .EXTRA_LANGUAGE_MODEL,

                                RecognizerIntent
                                    .LANGUAGE_MODEL_FREE_FORM
                            )

                            putExtra(
                                RecognizerIntent
                                    .EXTRA_PROMPT,

                                "Speak your question"
                            )
                        }

                    speechLauncher.launch(
                        intent
                    )
                }
            )
        }

        if (state.isLoading) {

            Box(

                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (message.isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {

        Surface(

            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,

                            bottomStart =
                                if (message.isUser) {
                                    18.dp
                                } else {
                                    4.dp
                                },

                            bottomEnd =
                                if (message.isUser) {
                                    4.dp
                                } else {
                                    18.dp
                                }
                        )
                    ),

            color =
                if (message.isUser) {
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                } else {
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                }
        ) {

            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {

                Text(

                    text =
                        if (message.isUser) {
                            "You"
                        } else {
                            "Gemini"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Text(

                    text =
                        message.text
                            .toBoldAnnotatedString(),

                    fontSize =
                        16.sp,

                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun PromptBar(

    prompt: String,

    promptError:
    PromptError?,

    enabled: Boolean,

    onPromptChange:
        (String) -> Unit,

    onSend:
        () -> Unit,

    onVoiceClick:
        () -> Unit,
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        OutlinedTextField(

            value =
                prompt,

            onValueChange =
                onPromptChange,

            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),

            label = {
                Text("Ask Gemini")
            },

            minLines = 1,

            maxLines = 4,

            enabled =
                enabled,

            isError =
                promptError != null,

            supportingText =
                promptError?.let {

                    {
                        Text(
                            "Please enter a message"
                        )
                    }
                }
        )

        FilledIconButton(

            onClick =
                onVoiceClick,

            enabled =
                enabled
        ) {

            Text(
                text = "🎤"
            )
        }

        FilledIconButton(

            onClick =
                onSend,

            enabled =
                enabled &&
                        prompt.isNotBlank()
        ) {

            Icon(

                imageVector =
                    Icons.AutoMirrored.Filled.Send,

                contentDescription =
                    "Send"
            )
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
private fun ChatScreenPreview() {

    GeminiApiComposeStarterTheme {

        ChatScreen(

            state =
                ChatUiState(

                    messages =
                        listOf(

                            ChatMessage(
                                id = 1,
                                text = "Hello",
                                isUser = true
                            ),

                            ChatMessage(
                                id = 2,
                                text =
                                    "Hello! How can I help you today?",
                                isUser = false
                            )
                        )
                ),

            onPromptChange = {},

            onSend = {}
        )
    }
}


