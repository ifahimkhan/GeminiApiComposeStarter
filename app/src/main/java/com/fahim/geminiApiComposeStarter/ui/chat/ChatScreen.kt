package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onOpenSettings = viewModel::onOpenApiKeyDialog,
        onDismissSettings = viewModel::onDismissApiKeyDialog,
        onSaveApiKey = viewModel::onSaveApiKey,
        onClearApiKey = viewModel::onClearApiKey,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    onSaveApiKey: (String) -> Unit = {},
    onClearApiKey: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (state.showApiKeyDialog) {
        ApiKeyDialog(
            isConfigured = state.isApiKeyConfigured,
            onDismiss = onDismissSettings,
            onSave = onSaveApiKey,
            onClear = onClearApiKey,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.api_key_settings),
                            tint = if (state.isApiKeyConfigured) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                ResponseArea(
                    text = state.response.ifEmpty { stringResource(R.string.response_placeholder) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ResponseArea(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.verticalScroll(rememberScrollState())) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text.toBoldAnnotatedString(),
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val isOverLimit = prompt.length > ChatViewModel.MAX_PROMPT_LENGTH
    val isSendEnabled = enabled && prompt.isNotBlank() && !isOverLimit

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = 3,
            maxLines = 6,
            enabled = enabled,
            isError = promptError != null,
            supportingText = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val errorText = when (promptError) {
                        PromptError.EMPTY -> stringResource(R.string.field_cannot_be_empty)
                        PromptError.TOO_LONG -> stringResource(
                            R.string.field_too_long,
                            ChatViewModel.MAX_PROMPT_LENGTH
                        )
                        null -> null
                    }
                    if (errorText != null) {
                        Text(
                            text = errorText,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = "${prompt.length}/${ChatViewModel.MAX_PROMPT_LENGTH}",
                        color = if (isOverLimit) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
        )
        FilledIconButton(onClick = onSend, enabled = isSendEnabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    isConfigured: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var isMasked by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.api_key_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.api_key_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isConfigured) {
                        stringResource(R.string.api_key_configured)
                    } else {
                        stringResource(R.string.api_key_not_configured)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isConfigured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(stringResource(R.string.api_key_input_hint)) },
                    singleLine = true,
                    visualTransformation = if (isMasked) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        TextButton(onClick = { isMasked = !isMasked }) {
                            Text(if (isMasked) "Show" else "Hide")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (apiKeyInput.isNotBlank()) {
                        onSave(apiKeyInput)
                    }
                },
                enabled = apiKeyInput.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (isConfigured) {
                    TextButton(onClick = onClear) {
                        Text(
                            stringResource(R.string.clear),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(response = "**Hello** from Gemini."),
            onPromptChange = {},
            onSend = {},
        )
    }
}
