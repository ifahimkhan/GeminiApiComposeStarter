package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreference
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import java.util.Locale

private val GeminiRoundedFont = FontFamily(
    Typeface.create(
        "sans-serif-rounded",
        Typeface.NORMAL
    )
)

private data class ChatLayoutConfig(
    val contentMaxWidth: Dp,
    val horizontalPadding: Dp,
    val bubbleMaxWidth: Dp
)

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                viewModel.onPromptChange(spokenText)
            }
        }
    }

    val speechIntent = remember {
        Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault().toLanguageTag()
            )

            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak your prompt"
            )
        }
    }

    ChatScreen(
        state = state,
        windowWidthSizeClass = windowWidthSizeClass,
        themePreference = themePreference,
        onThemePreferenceChange = onThemePreferenceChange,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceClick = {
            speechLauncher.launch(speechIntent)
        }
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val listState = rememberLazyListState()

    val layoutConfig = remember(windowWidthSizeClass) {
        when (windowWidthSizeClass) {

            WindowWidthSizeClass.Compact ->
                ChatLayoutConfig(
                    contentMaxWidth = Dp.Unspecified,
                    horizontalPadding = 16.dp,
                    bubbleMaxWidth = 320.dp
                )

            WindowWidthSizeClass.Medium ->
                ChatLayoutConfig(
                    contentMaxWidth = 720.dp,
                    horizontalPadding = 24.dp,
                    bubbleMaxWidth = 480.dp
                )

            WindowWidthSizeClass.Expanded ->
                ChatLayoutConfig(
                    contentMaxWidth = 900.dp,
                    horizontalPadding = 32.dp,
                    bubbleMaxWidth = 600.dp
                )

            else ->
                ChatLayoutConfig(
                    contentMaxWidth = Dp.Unspecified,
                    horizontalPadding = 16.dp,
                    bubbleMaxWidth = 320.dp
                )
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(
        state.messages.size,
        state.isLoading
    ) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                state.messages.lastIndex
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),

        containerColor =
            MaterialTheme.colorScheme.background,

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (
                            layoutConfig.contentMaxWidth !=
                            Dp.Unspecified
                        ) {
                            Modifier.widthIn(
                                max = layoutConfig.contentMaxWidth
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(
                        horizontal =
                            layoutConfig.horizontalPadding,
                        vertical = 12.dp
                    )
            ) {

                ChatTopBar(
                    themePreference = themePreference,
                    onThemePreferenceChange =
                        onThemePreferenceChange
                )

                if (
                    state.messages.isEmpty() &&
                    !state.isLoading
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = stringResource(
                                R.string.response_placeholder
                            ),
                            style =
                                MaterialTheme.typography.bodyLarge,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                } else {

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp),
                        contentPadding =
                            PaddingValues(
                                vertical = 10.dp
                            )
                    ) {

                        items(
                            items = state.messages,
                            key = { message ->
                                message.id
                            }
                        ) { message ->

                            ChatBubble(
                                message = message,
                                bubbleMaxWidth =
                                    layoutConfig.bubbleMaxWidth
                            )
                        }

                        if (state.isLoading) {

                            item(
                                key = "loading"
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 8.dp
                                        ),
                                    horizontalArrangement =
                                        Arrangement.Start
                                ) {

                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
                }

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceClick = onVoiceClick
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 4.dp,
                bottom = 14.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = "Gemini Starter App",
            style =
                MaterialTheme.typography.headlineSmall,
            fontFamily = GeminiRoundedFont,
            color =
                MaterialTheme.colorScheme.onBackground
        )

        Box {

            IconButton(
                onClick = {
                    menuExpanded = true
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.MoreVert,
                    contentDescription =
                        "Theme options",
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                },
                modifier = Modifier
                    .width(190.dp)
                    .clip(
                        RoundedCornerShape(18.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme
                            .surfaceVariant
                    )
            ) {

                Text(
                    text = "Theme",
                    style =
                        MaterialTheme.typography
                            .labelMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 12.dp,
                        bottom = 6.dp
                    )
                )

                ThemeMenuItem(
                    title = "System",
                    selected =
                        themePreference ==
                                ThemePreference.SYSTEM,
                    onClick = {
                        onThemePreferenceChange(
                            ThemePreference.SYSTEM
                        )
                        menuExpanded = false
                    }
                )

                ThemeMenuItem(
                    title = "Light",
                    selected =
                        themePreference ==
                                ThemePreference.LIGHT,
                    onClick = {
                        onThemePreferenceChange(
                            ThemePreference.LIGHT
                        )
                        menuExpanded = false
                    }
                )

                ThemeMenuItem(
                    title = "Dark",
                    selected =
                        themePreference ==
                                ThemePreference.DARK,
                    onClick = {
                        onThemePreferenceChange(
                            ThemePreference.DARK
                        )
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeMenuItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyLarge,
                color =
                    MaterialTheme.colorScheme.onSurface
            )
        },

        trailingIcon = {

            if (selected) {

                Icon(
                    imageVector =
                        Icons.Filled.Check,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.primary
                )
            }
        },

        onClick = onClick,

        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 2.dp
            )
    )
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    bubbleMaxWidth: Dp
) {
    val isUser =
        message.sender == MessageSender.USER

    val bubbleShape =
        if (isUser) {

            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 6.dp
            )

        } else {

            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 6.dp,
                bottomEnd = 20.dp
            )
        }

    val containerColor =
        if (isUser) {
            MaterialTheme.colorScheme
                .primaryContainer
        } else {
            MaterialTheme.colorScheme
                .secondaryContainer
        }

    val textColor =
        if (isUser) {
            MaterialTheme.colorScheme
                .onPrimaryContainer
        } else {
            MaterialTheme.colorScheme
                .onSecondaryContainer
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {

        Card(
            modifier = Modifier.widthIn(
                max = bubbleMaxWidth
            ),
            shape = bubbleShape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
        ) {

            if (isUser) {

                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    style =
                        MaterialTheme.typography.bodyLarge
                )

            } else {

                Text(
                    text =
                        message.text
                            .toBoldAnnotatedString(),
                    color = textColor,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    style =
                        MaterialTheme.typography.bodyLarge
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
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                bottom = 4.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,

            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),

            placeholder = {

                Text(
                    text = stringResource(
                        R.string.enter_your_prompt_here
                    ),
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            },

            shape =
                RoundedCornerShape(28.dp),

            minLines = 1,
            maxLines = 4,

            enabled = enabled,

            isError =
                promptError != null,

            supportingText =
                if (promptError != null) {

                    {
                        Text(
                            stringResource(
                                R.string
                                    .field_cannot_be_empty
                            )
                        )
                    }

                } else {
                    null
                },

            colors =
                OutlinedTextFieldDefaults.colors(

                    focusedContainerColor =
                        MaterialTheme.colorScheme
                            .surfaceVariant,

                    unfocusedContainerColor =
                        MaterialTheme.colorScheme
                            .surfaceVariant,

                    disabledContainerColor =
                        MaterialTheme.colorScheme
                            .surfaceVariant,

                    focusedBorderColor =
                        MaterialTheme.colorScheme.primary,

                    unfocusedBorderColor =
                        MaterialTheme.colorScheme.outline
                            .copy(alpha = 0.35f)
                )
        )

        FilledIconButton(
            onClick = onVoiceClick,
            enabled = enabled,

            modifier =
                Modifier.size(52.dp),

            colors =
                IconButtonDefaults
                    .filledIconButtonColors(

                        containerColor =
                            MaterialTheme.colorScheme
                                .surfaceVariant,

                        contentColor =
                            MaterialTheme.colorScheme
                                .onSurface
                    )
        ) {

            Icon(
                imageVector =
                    Icons.Filled.Mic,
                contentDescription =
                    "Voice input"
            )
        }

        FilledIconButton(
            onClick = onSend,
            enabled = enabled,

            modifier = Modifier
                .padding(start = 8.dp)
                .size(52.dp),

            colors =
                IconButtonDefaults
                    .filledIconButtonColors(

                        containerColor =
                            MaterialTheme.colorScheme
                                .primary,

                        contentColor =
                            MaterialTheme.colorScheme
                                .onPrimary
                    )
        ) {

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.Send,
                contentDescription =
                    stringResource(
                        R.string.send
                    )
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
                    messages = listOf(

                        ChatMessage(
                            id = 1L,
                            text =
                                "Can you explain Jetpack Compose?",
                            sender =
                                MessageSender.USER
                        ),

                        ChatMessage(
                            id = 2L,
                            text =
                                "**Jetpack Compose** is Android's modern declarative UI toolkit.",
                            sender =
                                MessageSender.GEMINI
                        )
                    )
                ),

            windowWidthSizeClass =
                WindowWidthSizeClass.Compact,

            themePreference =
                ThemePreference.SYSTEM,

            onThemePreferenceChange = {},
            onPromptChange = {},
            onSend = {},
            onVoiceClick = {}
        )
    }
}