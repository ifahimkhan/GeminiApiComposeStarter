package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.core.content.ContextCompat

import kotlinx.coroutines.launch


// ==========================================================
// CHAT ROUTE
// ==========================================================

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onCancelRequest = viewModel::cancelRequest,
        onClearError = viewModel::clearError,
        onNewChat = viewModel::createNewChat,
        onSelectConversation = viewModel::selectConversation,
        isDarkMode = isDarkMode,
        onDarkModeChange = onDarkModeChange
    )
}


// ==========================================================
// MAIN CHAT SCREEN
// ==========================================================

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelRequest: () -> Unit,
    onClearError: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {

    val context = LocalContext.current

    val activity = context as Activity

    val windowSizeClass = calculateWindowSizeClass(activity)

    val isExpanded =
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()


    // ======================================================
    // VOICE INPUT
    // ======================================================

    val voiceLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val results =
                    result.data?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )

                val spokenText = results?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {

                    onPromptChange(
                        if (state.prompt.isBlank()) {
                            spokenText
                        } else {
                            "${state.prompt} $spokenText"
                        }
                    )
                }
            }
        }


    // ======================================================
    // MICROPHONE PERMISSION
    // ======================================================

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {

                val intent = Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        java.util.Locale.getDefault()
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_PROMPT,
                        "Speak your message"
                    )
                }

                voiceLauncher.launch(intent)
            }
        }


    // ======================================================
    // START VOICE INPUT
    // ======================================================

    fun startVoiceInput() {

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED


        if (permissionGranted) {

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    java.util.Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your message"
                )
            }

            voiceLauncher.launch(intent)

        } else {

            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }


    // ======================================================
    // CHAT CONTENT
    // ======================================================

    val chatContent: @Composable () -> Unit = {

        ChatMainContent(
            state = state,
            onPromptChange = onPromptChange,
            onSend = onSend,
            onCancelRequest = onCancelRequest,
            onClearError = onClearError,

            onOpenDrawer = {
                scope.launch {
                    drawerState.open()
                }
            },

            onVoiceInput = ::startVoiceInput,

            showMenuButton = !isExpanded
        )
    }


    // ======================================================
    // LARGE SCREEN
    // ======================================================

    if (isExpanded) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
        ) {

            ChatSidebar(
                state = state,

                onNewChat = onNewChat,

                onSelectConversation =
                    onSelectConversation,

                isDarkMode = isDarkMode,

                onDarkModeChange =
                    onDarkModeChange,

                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            )

            chatContent()
        }

    } else {

        // ==================================================
        // PHONE
        // ==================================================

        ModalNavigationDrawer(
            drawerState = drawerState,

            drawerContent = {

                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp)
                ) {

                    ChatSidebar(
                        state = state,

                        onNewChat = {

                            onNewChat()

                            scope.launch {
                                drawerState.close()
                            }
                        },

                        onSelectConversation = { conversationId ->

                            onSelectConversation(
                                conversationId
                            )

                            scope.launch {
                                drawerState.close()
                            }
                        },

                        isDarkMode = isDarkMode,

                        onDarkModeChange =
                            onDarkModeChange,

                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        ) {

            chatContent()
        }
    }
}


// ==========================================================
// SIDEBAR
// ==========================================================

@Composable
private fun ChatSidebar(
    state: ChatUiState,
    onNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow
            )
            .padding(
                horizontal = 12.dp,
                vertical = 16.dp
            )
    ) {

        // ==================================================
        // LOGO / TITLE
        // ==================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.SmartToy,

                    contentDescription = null,

                    tint =
                        MaterialTheme.colorScheme
                            .onPrimaryContainer
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Gemini AI",

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = "Your AI Assistant",

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // ==================================================
        // NEW CHAT
        // ==================================================

        NavigationDrawerItem(

            label = {

                Text(
                    text = "New Chat",
                    fontWeight = FontWeight.Medium
                )
            },

            selected = false,

            onClick = onNewChat,

            icon = {

                Icon(
                    imageVector = Icons.Filled.Add,

                    contentDescription =
                        "New Chat"
                )
            },

            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        HorizontalDivider()


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // ==================================================
        // PREVIOUS CHATS
        // ==================================================

        Text(
            text = "Previous Chats",

            style =
                MaterialTheme.typography.titleSmall,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,

            modifier =
                Modifier.padding(
                    horizontal = 12.dp
                )
        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            if (state.conversations.isEmpty()) {

                item {

                    Text(
                        text = "No previous chats",

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,

                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 16.dp
                            )
                    )
                }

            } else {

                items(
                    items = state.conversations,

                    key = {
                            conversation ->
                        conversation.id
                    }

                ) { conversation ->

                    NavigationDrawerItem(

                        label = {

                            Text(
                                text =
                                    conversation.title
                                        .ifBlank {
                                            "New Chat"
                                        },

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        },

                        selected = false,

                        onClick = {

                            onSelectConversation(
                                conversation.id
                            )
                        },

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Filled
                                        .ChatBubbleOutline,

                                contentDescription = null
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            }
        }


        // ==================================================
        // SETTINGS
        // ==================================================

        HorizontalDivider()


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Filled.Settings,

                contentDescription =
                    "Settings",

                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Settings",

                    style =
                        MaterialTheme.typography.bodyLarge,

                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text = "Dark Mode",

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }


            Switch(
                checked = isDarkMode,

                onCheckedChange =
                    onDarkModeChange
            )
        }


        Spacer(
            modifier =
                Modifier.windowInsetsBottomHeight(
                    WindowInsets.navigationBars
                )
        )
    }
}


// ==========================================================
// MAIN CHAT CONTENT
// ==========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatMainContent(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelRequest: () -> Unit,
    onClearError: () -> Unit,
    onOpenDrawer: () -> Unit,
    onVoiceInput: () -> Unit,
    showMenuButton: Boolean
) {

    val listState =
        rememberLazyListState()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }


    // ======================================================
    // AUTO SCROLL
    // ======================================================

    LaunchedEffect(
        state.messages.size,
        state.isLoading
    ) {

        if (state.messages.isNotEmpty()) {

            listState.animateScrollToItem(
                index = state.messages.lastIndex
            )
        }
    }


    // ======================================================
    // ERROR SNACKBAR
    // ======================================================

    LaunchedEffect(
        state.errorMessage
    ) {

        val error =
            state.errorMessage

        if (!error.isNullOrBlank()) {

            snackbarHostState.showSnackbar(
                message = error
            )

            onClearError()
        }
    }


    // ======================================================
    // SCAFFOLD
    // ======================================================

    Scaffold(

        modifier = Modifier
            .fillMaxSize()
            .imePadding(),


        // ==================================================
        // TOP BAR
        // ==================================================

        topBar = {

            TopAppBar(

                title = {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                                ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.SmartToy,

                                contentDescription =
                                    "Gemini AI",

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )


                        Column {

                            Text(
                                text = "Gemini AI",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge
                            )

                            Text(
                                text =
                                    "Your AI Assistant",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                },


                navigationIcon = {

                    if (showMenuButton) {

                        IconButton(
                            onClick = onOpenDrawer
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.Menu,

                                contentDescription =
                                    "Open previous chats"
                            )
                        }
                    }
                }
            )
        },


        // ==================================================
        // SNACKBAR
        // ==================================================

        snackbarHost = {

            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        },


        // ==================================================
        // BOTTOM PROMPT BAR
        // ==================================================

        bottomBar = {

            PromptBar(
                prompt = state.prompt,

                isLoading =
                    state.isLoading,

                onPromptChange =
                    onPromptChange,

                onSend =
                    onSend,

                onCancelRequest =
                    onCancelRequest,

                onVoiceInput =
                    onVoiceInput,

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
            )
        },


        contentWindowInsets =
            WindowInsets.safeDrawing

    ) { innerPadding ->


        // ==================================================
        // CHAT MESSAGES
        // ==================================================

        LazyColumn(

            state = listState,

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 12.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {


            // ==================================================
            // WELCOME MESSAGE
            // ==================================================

            if (state.messages.isEmpty()) {

                item(
                    key = "welcome"
                ) {

                    WelcomeMessage()
                }
            }


            // ==================================================
            // MESSAGES
            // ==================================================

            items(

                items = state.messages,

                key = {
                        message ->
                    message.id
                }

            ) { message ->

                MessageBubble(
                    message = message
                )
            }


            // ==================================================
            // LOADING
            // ==================================================

            if (state.isLoading) {

                item(
                    key = "loading"
                ) {

                    LoadingBubble()
                }
            }


            item(
                key = "bottom_space"
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }
        }
    }
}


// ==========================================================
// WELCOME MESSAGE
// ==========================================================

@Composable
private fun WelcomeMessage() {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                bottom = 4.dp
            ),

        horizontalArrangement =
            Arrangement.Start
    ) {

        Avatar(
            isUser = false
        )


        Spacer(
            modifier = Modifier.width(8.dp)
        )


        Box(

            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )

                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                )

                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {

            Text(
                text =
                    "Hello! How can I help you today?",

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


// ==========================================================
// MESSAGE BUBBLE
// ==========================================================

@Composable
private fun MessageBubble(
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
            },

        verticalAlignment =
            Alignment.Bottom
    ) {

        if (!message.isUser) {

            Avatar(
                isUser = false
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )
        }


        Box(

            modifier = Modifier
                .clip(
                    RoundedCornerShape(

                        topStart = 16.dp,

                        topEnd = 16.dp,

                        bottomStart =
                            if (message.isUser) {
                                16.dp
                            } else {
                                4.dp
                            },

                        bottomEnd =
                            if (message.isUser) {
                                4.dp
                            } else {
                                16.dp
                            }
                    )
                )

                .background(

                    if (message.isUser) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
                )

                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {

            Text(

                text =
                    message.text,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                color =

                    if (message.isUser) {

                        MaterialTheme
                            .colorScheme
                            .onPrimary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )
        }


        if (message.isUser) {

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Avatar(
                isUser = true
            )
        }
    }
}


// ==========================================================
// AVATAR
// ==========================================================

@Composable
private fun Avatar(
    isUser: Boolean
) {

    Box(

        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(

                if (isUser) {

                    MaterialTheme
                        .colorScheme
                        .secondaryContainer

                } else {

                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                }
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Icon(

            imageVector =

                if (isUser) {

                    Icons.Filled.Person

                } else {

                    Icons.Filled.SmartToy
                },

            contentDescription = null,

            tint =

                if (isUser) {

                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer

                } else {

                    MaterialTheme
                        .colorScheme
                        .onPrimaryContainer
                }
        )
    }
}


// ==========================================================
// LOADING BUBBLE
// ==========================================================

@Composable
private fun LoadingBubble() {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Avatar(
            isUser = false
        )


        Spacer(
            modifier =
                Modifier.width(8.dp)
        )


        Box(

            modifier = Modifier
                .clip(
                    RoundedCornerShape(16.dp)
                )

                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                )

                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                )
        ) {

            CircularProgressIndicator(

                modifier =
                    Modifier.size(22.dp),

                strokeWidth = 2.dp
            )
        }
    }
}


// ==========================================================
// PROMPT BAR
// ==========================================================

@Composable
private fun PromptBar(
    prompt: String,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelRequest: () -> Unit,
    onVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(

        modifier = modifier,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        OutlinedTextField(

            value = prompt,

            onValueChange =
                onPromptChange,

            modifier =
                Modifier.weight(1f),

            placeholder = {

                Text(
                    "Ask Gemini anything..."
                )
            },

            singleLine = true,

            enabled = !isLoading,

            shape =
                RoundedCornerShape(24.dp),

            leadingIcon = {

                IconButton(

                    onClick =
                        onVoiceInput,

                    enabled =
                        !isLoading
                ) {

                    Icon(

                        imageVector =
                            Icons.Filled.Mic,

                        contentDescription =
                            "Voice input"
                    )
                }
            }
        )


        Spacer(
            modifier =
                Modifier.width(8.dp)
        )


        if (isLoading) {

            IconButton(
                onClick =
                    onCancelRequest
            ) {

                CircularProgressIndicator(

                    modifier =
                        Modifier.size(24.dp),

                    strokeWidth = 2.dp
                )
            }

        } else {

            IconButton(

                onClick =
                    onSend,

                enabled =
                    prompt.isNotBlank()
            ) {

                Icon(

                    imageVector =
                        Icons.Filled.Send,

                    contentDescription =
                        "Send"
                )
            }
        }
    }
}