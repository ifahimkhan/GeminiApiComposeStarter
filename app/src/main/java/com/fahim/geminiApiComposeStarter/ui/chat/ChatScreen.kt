package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreference
import com.fahim.geminiApiComposeStarter.ui.theme.BrandBlue
import com.fahim.geminiApiComposeStarter.ui.theme.BrandTeal
import com.fahim.geminiApiComposeStarter.ui.theme.BrandViolet
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.voice.isSpeechRecognitionAvailable
import com.fahim.geminiApiComposeStarter.ui.voice.rememberVoiceInputLauncher

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    onThemePreferenceChange: (ThemePreference) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        themePreference = themePreference,
        onThemePreferenceChange = onThemePreferenceChange,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onRetry = viewModel::onRetry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    onThemePreferenceChange: (ThemePreference) -> Unit = {},
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var voiceUnavailableMessage by remember { mutableStateOf<String?>(null) }

    val launchVoiceInput = rememberVoiceInputLauncher(
        onResult = { spokenText ->
            val combined = if (state.inputText.isBlank()) spokenText else "${state.inputText} $spokenText"
            onInputChange(combined)
        },
        onUnavailable = {
            voiceUnavailableMessage = "Voice input isn't available on this device."
        },
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(BrandBlue, BrandViolet, BrandTeal)),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_name_display),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                },
                actions = {
                    ThemeMenuButton(
                        current = themePreference,
                        onSelected = onThemePreferenceChange,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            if (state.messages.isEmpty() && !state.isLoading) {
                WelcomeEmptyState(
                    onSuggestionSelected = onSuggestionSelected,
                    modifier = Modifier.weight(1f),
                )
            } else {
                MessageList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                val bannerMessage = state.errorMessage ?: voiceUnavailableMessage
                AnimatedVisibility(visible = bannerMessage != null, enter = fadeIn(), exit = fadeOut()) {
                    if (bannerMessage != null) {
                        ErrorRetryCard(
                            message = bannerMessage,
                            canRetry = state.errorMessage != null && state.canRetry,
                            onRetry = {
                                voiceUnavailableMessage = null
                                onRetry()
                            },
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }

                ChatInputBar(
                    text = state.inputText,
                    inputError = state.inputError,
                    enabled = !state.isLoading,
                    onTextChange = onInputChange,
                    onSend = onSend,
                    onMicClick = {
                        voiceUnavailableMessage = null
                        if (isSpeechRecognitionAvailable(context)) {
                            launchVoiceInput()
                        } else {
                            voiceUnavailableMessage = "Voice input isn't available on this device."
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0

    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(messages, key = { _, message -> message.id }) { _, message ->
            MessageBubble(message = message)
        }
        if (isLoading) {
            item(key = "typing_indicator") { TypingIndicatorBubble() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeMenuButton(current: ThemePreference, onSelected: (ThemePreference) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val icon = when (current) {
        ThemePreference.LIGHT -> Icons.Filled.LightMode
        ThemePreference.DARK -> Icons.Filled.DarkMode
        ThemePreference.SYSTEM -> Icons.Filled.SettingsBrightness
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = icon, contentDescription = "Theme")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("System default") },
                onClick = { expanded = false; onSelected(ThemePreference.SYSTEM) },
                leadingIcon = { Icon(Icons.Filled.SettingsBrightness, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Light") },
                onClick = { expanded = false; onSelected(ThemePreference.LIGHT) },
                leadingIcon = { Icon(Icons.Filled.LightMode, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Dark") },
                onClick = { expanded = false; onSelected(ThemePreference.DARK) },
                leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(),
            onInputChange = {},
            onSend = {},
            onSuggestionSelected = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenConversationPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(id = 1, text = "Explain black holes simply", isFromUser = true),
                    ChatMessage(id = 2, text = "**Black holes** are regions where gravity is so strong not even light escapes.", isFromUser = false),
                ),
            ),
            onInputChange = {},
            onSend = {},
            onSuggestionSelected = {},
            onRetry = {},
        )
    }
}
