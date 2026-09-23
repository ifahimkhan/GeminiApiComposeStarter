package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import androidx.compose.ui.res.stringResource

@Composable
fun ChatRoute(viewModel: ChatViewModel) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit
) {

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Header()

            ChatContent(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend
            )
        }
    }
}

@Composable
private fun Header() {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Assistant",
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Gemini Study Assistant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ask questions • Learn • Understand",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatContent(
    state: ChatUiState,
    modifier: Modifier = Modifier
) {

    val responseText = state.response.ifEmpty {
        stringResource(R.string.response_placeholder)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        verticalArrangement = Arrangement.spacedBy(14.dp),

        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 20.dp
        )
    ) {

        if (state.prompt.isNotBlank()) {

            item {
                UserMessage(
                    message = state.prompt
                )
            }
        }

        item {
            AssistantMessage(
                message = responseText
            )
        }

        if (state.isLoading) {

            item {
                LoadingMessage()
            }
        }
    }
}

@Composable
private fun UserMessage(
    message: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {

        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),

            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 4.dp
            ),

            color = MaterialTheme.colorScheme.primaryContainer
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "You",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(
    message: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {

        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),

            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 4.dp,
                bottomEnd = 20.dp
            ),

            color = MaterialTheme.colorScheme.surfaceVariant
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Gemini",
                            modifier = Modifier.padding(7.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Gemini",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Text(
                    text = message.toBoldAnnotatedString(),
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingMessage() {

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Text(
                    text = "Gemini is thinking..."
                )
            }
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.Bottom
        ) {

            OutlinedTextField(
                value = prompt,

                onValueChange = onPromptChange,

                modifier = Modifier.weight(1f),

                placeholder = {
                    Text(
                        text = stringResource(
                            R.string.enter_your_prompt_here
                        )
                    )
                },

                minLines = 1,
                maxLines = 5,

                enabled = enabled,

                isError = promptError != null,

                shape = RoundedCornerShape(22.dp),

                supportingText = {
                    if (promptError != null) {
                        Text(
                            text = stringResource(
                                R.string.field_cannot_be_empty
                            )
                        )
                    }
                }
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            FilledIconButton(
                onClick = onSend,

                enabled = enabled && prompt.isNotBlank(),

                modifier = Modifier.size(52.dp)
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(
                        R.string.send
                    )
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                prompt = "Explain photosynthesis",
                response = "**Photosynthesis** is the process by which green plants convert light energy into chemical energy."
            ),
            onPromptChange = {},
            onSend = {}
        )
    }
}