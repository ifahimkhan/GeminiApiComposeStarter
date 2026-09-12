package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.local.ChatMessage
import com.fahim.geminiApiComposeStarter.data.local.ChatSession
import com.fahim.geminiApiComposeStarter.data.local.MessageAuthor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch


private val ChatBackground = Color(0xFF1E1E1E)
private val TopBarBackground = Color(0xFF252525)
private val InputBackground = Color(0xFF292929)
private val DrawerItemBackground = Color(0xFF303030)

private val UserBubbleColor = Color(0xFF2E7D32)
private val GeminiBubbleColor = Color(0xFF2D659C)

private val OnlineBackground = Color(0xFF173A24)
private val OnlineText = Color(0xFF9DE2AF)

private val OfflineBackground = Color(0xFF472424)
private val OfflineText = Color(0xFFFFB4AB)

private val ErrorBackground = Color(0xFF452127)


/* ---------------------------------------------------------
   CHAT ROUTE
   --------------------------------------------------------- */

@Composable
fun ChatRoute(
    viewModel: ChatViewModel
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current


    /*
     * Speech recognition launcher.
     *
     * The recognized text is returned to the ViewModel.
     * If text already exists in the input box, the ViewModel
     * appends the speech result instead of deleting it.
     */
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            val text =
                result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()

            if (!text.isNullOrBlank()) {

                viewModel.onVoiceResult(text)
            }
        }
    }


    val startVoiceInput = {

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    context.getString(
                        R.string.listening_hint
                    )
                )
            }

        try {

            voiceLauncher.launch(intent)

        } catch (_: ActivityNotFoundException) {

            viewModel.onVoiceInputUnavailable()
        }
    }


    ChatScreen(

        state = state,

        onPromptChange =
        viewModel::onPromptChange,

        onSend =
        viewModel::onSend,

        onVoiceInput =
        startVoiceInput,

        onErrorConsumed =
        viewModel::onErrorConsumed,

        onRetry =
        viewModel::onRetry,

        onNewChat =
        viewModel::onNewChat,

        onSelectSession =
        viewModel::onSelectSession,

        onDeleteSession =
        viewModel::onDeleteSession,

        onOpenSettings =
        viewModel::onOpenSettings,

        onSettingsNameChange =
        viewModel::onSettingsNameChange,

        onDismissSettings =
        viewModel::onDismissSettings,

        onSaveSettings =
        viewModel::onSaveSettings,

        onClearHistory =
        viewModel::onClearHistory
    )
}


/* ---------------------------------------------------------
   MAIN CHAT SCREEN
   --------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(

    state: ChatUiState,

    onPromptChange: (String) -> Unit,

    onSend: () -> Unit,

    onVoiceInput: () -> Unit,

    onErrorConsumed: () -> Unit,

    onRetry: () -> Unit,

    onNewChat: () -> Unit,

    onSelectSession: (Long) -> Unit,

    onDeleteSession: (Long) -> Unit,

    onOpenSettings: () -> Unit,

    onSettingsNameChange: (String) -> Unit,

    onDismissSettings: () -> Unit,

    onSaveSettings: () -> Unit,

    onClearHistory: () -> Unit
) {

    val listState =
        rememberLazyListState()

    val drawerState =
        rememberDrawerState(
            initialValue = DrawerValue.Closed
        )

    val scope =
        rememberCoroutineScope()


    /*
     * Automatically keep the latest message,
     * loading bubble or error visible.
     */
    LaunchedEffect(
        state.messages.size,
        state.isLoading,
        state.errorMessage
    ) {

        val extraItems =

            (if (state.isLoading) 1 else 0) +

                    (if (state.errorMessage != null) 1 else 0)


        val totalItems =
            state.messages.size + extraItems


        if (totalItems > 0) {

            listState.animateScrollToItem(
                totalItems - 1
            )
        }
    }


    /*
     * Settings popup.
     */
    if (state.isSettingsOpen) {

        SettingsDialog(

            displayName =
            state.settingsDisplayName,

            onNameChange =
            onSettingsNameChange,

            onDismiss =
            onDismissSettings,

            onSave =
            onSaveSettings
        )
    }


    ModalNavigationDrawer(

        drawerState =
        drawerState,

        drawerContent = {

            ChatDrawer(

                sessions =
                state.sessions,

                currentSessionId =
                state.currentSessionId,

                isLoading =
                state.isLoading,

                onNewChat = {

                    onNewChat()

                    scope.launch {

                        drawerState.close()
                    }
                },

                onSelectSession = { sessionId ->

                    onSelectSession(sessionId)

                    scope.launch {

                        drawerState.close()
                    }
                },

                onDeleteSession =
                onDeleteSession
            )
        }
    ) {

        Scaffold(

            modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),

            containerColor =
            ChatBackground,

            topBar = {

                TopAppBar(

                    navigationIcon = {

                        IconButton(

                            onClick = {

                                scope.launch {

                                    drawerState.open()
                                }
                            }
                        ) {

                            Icon(

                                imageVector =
                                Icons.Filled.Menu,

                                contentDescription =
                                stringResource(
                                    R.string.open_chat_history
                                )
                            )
                        }
                    },

                    title = {

                        Column {

                            Text(

                                text =
                                stringResource(
                                    R.string.chat_title
                                ),

                                fontWeight =
                                FontWeight.SemiBold
                            )


                            Text(

                                text =
                                "${state.displayName.ifBlank { DEFAULT_DISPLAY_NAME }} • $STUDENT_ROLL_NUMBER",

                                style =
                                MaterialTheme.typography.labelSmall,

                                color =
                                Color.White.copy(
                                    alpha = 0.75f
                                )
                            )
                        }
                    },

                    actions = {

                        IconButton(

                            onClick =
                            onClearHistory,

                            enabled =
                            state.currentSessionId != null &&
                                    !state.isLoading
                        ) {

                            Icon(

                                imageVector =
                                Icons.Filled.Delete,

                                contentDescription =
                                stringResource(
                                    R.string.clear_history
                                )
                            )
                        }


                        IconButton(

                            onClick =
                            onOpenSettings
                        ) {

                            Icon(

                                imageVector =
                                Icons.Filled.Settings,

                                contentDescription =
                                stringResource(
                                    R.string.settings
                                )
                            )
                        }
                    },

                    colors =
                    TopAppBarDefaults.topAppBarColors(

                        containerColor =
                        TopBarBackground,

                        titleContentColor =
                        Color.White,

                        navigationIconContentColor =
                        Color.White,

                        actionIconContentColor =
                        Color.White
                    )
                )
            }

        ) { innerPadding ->


            /*
             * The content is capped at 900dp so it also
             * remains readable on tablets, without using
             * experimental WindowSizeClass APIs.
             */
            Box(

                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),

                contentAlignment =
                Alignment.TopCenter
            ) {


                Column(

                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(
                            max = 900.dp
                        )
                        .fillMaxSize()
                        .padding(
                            horizontal = 14.dp
                        )
                ) {


                    ConnectivityBanner(

                        isOnline =
                        state.isOnline,

                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp
                            )
                    )


                    Spacer(
                        Modifier.height(4.dp)
                    )


                    if (

                        state.messages.isEmpty() &&

                        !state.isLoading &&

                        state.errorMessage == null

                    ) {

                        EmptyConversation(

                            modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                    } else {


                        LazyColumn(

                            state =
                            listState,

                            modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag(
                                    "messageList"
                                ),

                            contentPadding =
                            PaddingValues(

                                top = 10.dp,

                                bottom = 12.dp
                            ),

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


                                MessageBubble(

                                    message =
                                    message,

                                    displayName =
                                    state.displayName
                                )
                            }


                            if (state.isLoading) {

                                item(
                                    key = "loading"
                                ) {

                                    LoadingBubble()
                                }
                            }


                            state.errorMessage
                                ?.let { error ->

                                    item(
                                        key = "error"
                                    ) {

                                        ErrorCard(

                                            message =
                                            error,

                                            showRetry =
                                            state.retryAvailable,

                                            onRetry =
                                            onRetry,

                                            onDismiss =
                                            onErrorConsumed
                                        )
                                    }
                                }
                        }
                    }


                    PromptBar(

                        prompt =
                        state.prompt,

                        promptError =
                        state.promptError,

                        isLoading =
                        state.isLoading,

                        isOnline =
                        state.isOnline,

                        onPromptChange =
                        onPromptChange,

                        onSend =
                        onSend,

                        onVoiceInput =
                        onVoiceInput
                    )
                }
            }
        }
    }
}


/* ---------------------------------------------------------
   CHAT HISTORY DRAWER
   --------------------------------------------------------- */

@Composable
private fun ChatDrawer(

    sessions: List<ChatSession>,

    currentSessionId: Long?,

    isLoading: Boolean,

    onNewChat: () -> Unit,

    onSelectSession: (Long) -> Unit,

    onDeleteSession: (Long) -> Unit

) {

    ModalDrawerSheet(

        modifier =
        Modifier.width(
            310.dp
        )
    ) {


        Column(

            modifier =
            Modifier
                .fillMaxHeight()
                .padding(
                    vertical = 12.dp
                )
        ) {


            Text(

                text =
                stringResource(
                    R.string.conversations
                ),

                style =
                MaterialTheme.typography.titleLarge,

                fontWeight =
                FontWeight.Bold,

                modifier =
                Modifier.padding(
                    horizontal = 18.dp
                )
            )


            Spacer(
                Modifier.height(6.dp)
            )


            TextButton(

                onClick =
                onNewChat,

                enabled =
                !isLoading,

                modifier =
                Modifier.padding(
                    horizontal = 8.dp
                )
            ) {


                Icon(

                    imageVector =
                    Icons.Filled.Add,

                    contentDescription =
                    null
                )


                Spacer(
                    Modifier.width(8.dp)
                )


                Text(
                    stringResource(
                        R.string.new_chat
                    )
                )
            }


            Divider()


            if (sessions.isEmpty()) {


                Text(

                    text =
                    stringResource(
                        R.string.no_saved_chats
                    ),

                    style =
                    MaterialTheme.typography.bodyMedium,

                    modifier =
                    Modifier.padding(
                        18.dp
                    )
                )


            } else {


                LazyColumn(

                    modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),

                    contentPadding =
                    PaddingValues(
                        vertical = 6.dp
                    ),

                    verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
                ) {


                    items(

                        items =
                        sessions,

                        key = {
                            it.id
                        }

                    ) { session ->


                        DrawerSessionRow(

                            session =
                            session,

                            selected =
                            currentSessionId ==
                                    session.id,

                            enabled =
                            !isLoading,

                            onClick = {

                                onSelectSession(
                                    session.id
                                )
                            },

                            onDelete = {

                                onDeleteSession(
                                    session.id
                                )
                            }
                        )
                    }
                }
            }


            Divider()


            Text(

                text =
                stringResource(
                    R.string.chat_subtitle
                ),

                style =
                MaterialTheme.typography.labelSmall,

                modifier =
                Modifier.padding(
                    18.dp
                )
            )
        }
    }
}


/* ---------------------------------------------------------
   DRAWER SESSION ROW
   --------------------------------------------------------- */

@Composable
private fun DrawerSessionRow(

    session: ChatSession,

    selected: Boolean,

    enabled: Boolean,

    onClick: () -> Unit,

    onDelete: () -> Unit

) {

    val background =

        if (selected) {

            MaterialTheme.colorScheme
                .secondaryContainer

        } else {

            Color.Transparent
        }


    Row(

        modifier =
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp
            )
            .background(

                color =
                background,

                shape =
                RoundedCornerShape(
                    12.dp
                )
            )
            .clickable(

                enabled =
                enabled,

                onClick =
                onClick
            )
            .padding(

                start = 12.dp,

                top = 8.dp,

                bottom = 8.dp,

                end = 4.dp
            ),

        verticalAlignment =
        Alignment.CenterVertically
    ) {


        Column(

            modifier =
            Modifier.weight(1f)
        ) {


            Text(

                text =
                session.title.ifBlank {

                    stringResource(
                        R.string.new_chat
                    )
                },

                style =
                MaterialTheme.typography.bodyMedium,

                fontWeight =

                if (selected) {

                    FontWeight.SemiBold

                } else {

                    FontWeight.Normal
                },

                maxLines = 1,

                overflow =
                TextOverflow.Ellipsis
            )


            Text(

                text =
                formatDrawerTimestamp(
                    session.updatedAt
                ),

                style =
                MaterialTheme.typography.labelSmall,

                color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
            )
        }


        IconButton(

            onClick =
            onDelete,

            enabled =
            enabled
        ) {


            Icon(

                imageVector =
                Icons.Filled.Delete,

                contentDescription =
                stringResource(
                    R.string.delete_chat
                ),

                modifier =
                Modifier.size(
                    19.dp
                )
            )
        }
    }
}


/* ---------------------------------------------------------
   CONNECTIVITY STATUS
   --------------------------------------------------------- */

@Composable
private fun ConnectivityBanner(

    isOnline: Boolean,

    modifier: Modifier = Modifier

) {

    val background =

        if (isOnline) {

            OnlineBackground

        } else {

            OfflineBackground
        }


    val foreground =

        if (isOnline) {

            OnlineText

        } else {

            OfflineText
        }


    Surface(

        modifier =
        modifier,

        color =
        background,

        shape =
        RoundedCornerShape(
            12.dp
        )

    ) {


        Text(

            text =

            if (isOnline) {

                "●  Online • Gemini ready"

            } else {

                "●  Offline • Sending is unavailable"
            },

            color =
            foreground,

            style =
            MaterialTheme.typography.labelMedium,

            modifier =
            Modifier.padding(

                horizontal =
                12.dp,

                vertical =
                7.dp
            )
        )
    }
}


/* ---------------------------------------------------------
   EMPTY CHAT
   --------------------------------------------------------- */

@Composable
private fun EmptyConversation(

    modifier: Modifier = Modifier

) {


    Box(

        modifier =
        modifier,

        contentAlignment =
        Alignment.Center
    ) {


        Surface(

            color =
            DrawerItemBackground,

            shape =
            RoundedCornerShape(
                22.dp
            )
        ) {


            Column(

                modifier =
                Modifier.padding(
                    24.dp
                ),

                horizontalAlignment =
                Alignment.CenterHorizontally
            ) {


                Text(

                    text =
                    stringResource(
                        R.string.empty_history_title
                    ),

                    style =
                    MaterialTheme.typography.titleLarge,

                    fontWeight =
                    FontWeight.SemiBold,

                    color =
                    Color.White
                )


                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )


                Text(

                    text =
                    stringResource(
                        R.string.empty_history_body
                    ),

                    style =
                    MaterialTheme.typography.bodyMedium,

                    color =
                    Color.White.copy(
                        alpha = 0.76f
                    )
                )
            }
        }
    }
}


/* ---------------------------------------------------------
   MESSAGE BUBBLE
   --------------------------------------------------------- */

@Composable
private fun MessageBubble(

    message: ChatMessage,

    displayName: String

) {


    val isUser =
        message.author ==
                MessageAuthor.USER


    /*
     * Fixed maximum width avoids the experimental
     * WindowWidthSizeClass API.
     *
     * On normal phones this still behaves responsively
     * because widthIn() only sets a maximum.
     */
    val bubbleMaxWidth =
        420.dp


    val clipboard =
        LocalClipboardManager.current


    val context =
        LocalContext.current


    Row(

        modifier =
        Modifier.fillMaxWidth(),

        horizontalArrangement =

        if (isUser) {

            Arrangement.End

        } else {

            Arrangement.Start
        }
    ) {


        Surface(

            modifier =
            Modifier.widthIn(
                max = bubbleMaxWidth
            ),

            color =

            if (isUser) {

                UserBubbleColor

            } else {

                GeminiBubbleColor
            },

            shape =
            RoundedCornerShape(

                topStart =
                18.dp,

                topEnd =
                18.dp,

                bottomStart =

                if (isUser) {
                    18.dp
                } else {
                    4.dp
                },

                bottomEnd =

                if (isUser) {
                    4.dp
                } else {
                    18.dp
                }
            )
        ) {


            Column(

                modifier =
                Modifier.padding(

                    start =
                    12.dp,

                    end =
                    12.dp,

                    top =
                    11.dp,

                    bottom =

                    if (isUser) {
                        10.dp
                    } else {
                        6.dp
                    }
                )
            ) {


                Text(

                    text =

                    if (isUser) {

                        displayName.ifBlank {
                            DEFAULT_DISPLAY_NAME
                        }

                    } else {

                        stringResource(
                            R.string.gemini_name
                        )
                    },

                    style =
                    MaterialTheme.typography.labelMedium,

                    fontWeight =
                    FontWeight.Bold,

                    color =
                    Color.White.copy(
                        alpha = 0.88f
                    )
                )


                Spacer(
                    Modifier.height(
                        4.dp
                    )
                )


                Text(

                    text =
                    message.text,

                    style =
                    MaterialTheme.typography.bodyLarge,

                    color =
                    Color.White
                )


                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )


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
                        formatTimestamp(
                            message.timestamp
                        ),

                        style =
                        MaterialTheme.typography.labelSmall,

                        color =
                        Color.White.copy(
                            alpha = 0.66f
                        )
                    )


                    if (!isUser) {


                        TextButton(

                            onClick = {


                                clipboard.setText(

                                    AnnotatedString(
                                        message.text
                                    )
                                )


                                Toast
                                    .makeText(

                                        context,

                                        context.getString(
                                            R.string.copied
                                        ),

                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            },

                            contentPadding =
                            PaddingValues(

                                horizontal =
                                8.dp,

                                vertical =
                                0.dp
                            )
                        ) {


                            Text(

                                text =
                                stringResource(
                                    R.string.copy
                                ),

                                color =
                                Color.White.copy(
                                    alpha = 0.82f
                                ),

                                style =
                                MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}


/* ---------------------------------------------------------
   GEMINI LOADING BUBBLE
   --------------------------------------------------------- */

@Composable
private fun LoadingBubble() {


    Row(

        modifier =
        Modifier.fillMaxWidth(),

        horizontalArrangement =
        Arrangement.Start
    ) {


        Surface(

            shape =
            RoundedCornerShape(

                topStart =
                18.dp,

                topEnd =
                18.dp,

                bottomStart =
                4.dp,

                bottomEnd =
                18.dp
            ),

            color =
            GeminiBubbleColor

        ) {


            Row(

                modifier =
                Modifier.padding(

                    horizontal =
                    16.dp,

                    vertical =
                    12.dp
                ),

                verticalAlignment =
                Alignment.CenterVertically
            ) {


                CircularProgressIndicator(

                    modifier =
                    Modifier.size(
                        19.dp
                    ),

                    strokeWidth =
                    2.dp,

                    color =
                    Color.White
                )


                Spacer(
                    Modifier.width(
                        10.dp
                    )
                )


                Text(

                    text =
                    stringResource(
                        R.string.gemini_thinking
                    ),

                    color =
                    Color.White
                )
            }
        }
    }
}


/* ---------------------------------------------------------
   ERROR CARD
   --------------------------------------------------------- */

@Composable
private fun ErrorCard(

    message: String,

    showRetry: Boolean,

    onRetry: () -> Unit,

    onDismiss: () -> Unit

) {


    Surface(

        modifier =
        Modifier.fillMaxWidth(),

        color =
        ErrorBackground,

        shape =
        RoundedCornerShape(
            16.dp
        )
    ) {


        Column(

            modifier =
            Modifier.padding(

                horizontal =
                14.dp,

                vertical =
                12.dp
            )
        ) {


            Text(

                text =
                "Request problem",

                fontWeight =
                FontWeight.SemiBold,

                color =
                Color.White
            )


            Spacer(
                Modifier.height(
                    4.dp
                )
            )


            Text(

                text =
                message,

                style =
                MaterialTheme.typography.bodyMedium,

                color =
                Color.White.copy(
                    alpha = 0.86f
                )
            )


            Row(

                modifier =
                Modifier.fillMaxWidth(),

                horizontalArrangement =
                Arrangement.End
            ) {


                if (showRetry) {


                    TextButton(

                        onClick =
                        onRetry
                    ) {


                        Text(

                            stringResource(
                                R.string.retry
                            )
                        )
                    }
                }


                TextButton(

                    onClick =
                    onDismiss
                ) {


                    Text(

                        stringResource(
                            R.string.dismiss
                        )
                    )
                }
            }
        }
    }
}


/* ---------------------------------------------------------
   INPUT / COMPOSER
   --------------------------------------------------------- */

@Composable
private fun PromptBar(

    prompt: String,

    promptError: PromptError?,

    isLoading: Boolean,

    isOnline: Boolean,

    onPromptChange: (String) -> Unit,

    onSend: () -> Unit,

    onVoiceInput: () -> Unit

) {


    val sendEnabled =

        prompt.isNotBlank() &&

                !isLoading &&

                isOnline


    Column(

        modifier =
        Modifier
            .fillMaxWidth()
            .padding(

                top =
                6.dp,

                bottom =
                12.dp
            )
    ) {


        Row(

            modifier =
            Modifier.fillMaxWidth(),

            verticalAlignment =
            Alignment.Bottom
        ) {


            OutlinedTextField(
                value =
                prompt,

                onValueChange =
                onPromptChange,

                modifier =
                Modifier
                    .weight(1f)
                    .testTag(
                        "promptInput"
                    ),

                placeholder = {

                    Text(

                        stringResource(
                            R.string.prompt_label
                        )
                    )
                },

                minLines =
                1,

                maxLines =
                4,

                enabled =
                true,

                isError =
                promptError != null,

                supportingText =

                if (
                    promptError ==
                    PromptError.EMPTY
                ) {

                    {

                        Text(

                            stringResource(
                                R.string.prompt_empty
                            )
                        )
                    }

                } else {

                    null
                },

                keyboardOptions =
                KeyboardOptions(

                    imeAction =
                    ImeAction.Send
                ),

                keyboardActions =
                KeyboardActions(

                    onSend = {

                        if (sendEnabled) {

                            onSend()
                        }
                    }
                ),

                trailingIcon = {


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
                            stringResource(
                                R.string.voice_input
                            )
                        )
                    }
                },

                shape =
                RoundedCornerShape(
                    26.dp
                ),

                colors =
                OutlinedTextFieldDefaults.colors(

                    focusedTextColor =
                    Color.White,

                    unfocusedTextColor =
                    Color.White,

                    cursorColor =
                    Color.White,

                    focusedPlaceholderColor =
                    Color.Gray,

                    unfocusedPlaceholderColor =
                    Color.Gray,

                    focusedContainerColor =
                    InputBackground,

                    unfocusedContainerColor =
                    InputBackground,

                    disabledContainerColor =
                    InputBackground
                )
            )


            Spacer(
                Modifier.width(
                    9.dp
                )
            )


            FilledIconButton(

                onClick =
                onSend,

                enabled =
                sendEnabled,

                modifier =
                Modifier
                    .size(
                        52.dp
                    )
                    .testTag(
                        "sendButton"
                    )
            ) {


                if (isLoading) {


                    CircularProgressIndicator(

                        modifier =
                        Modifier.size(
                            20.dp
                        ),

                        strokeWidth =
                        2.dp
                    )


                } else {


                    Icon(

                        imageVector =
                        Icons.Filled.Send,

                        contentDescription =
                        stringResource(
                            R.string.send
                        )
                    )
                }
            }
        }
    }
}


/* ---------------------------------------------------------
   SETTINGS
   --------------------------------------------------------- */

@Composable
private fun SettingsDialog(

    displayName: String,

    onNameChange: (String) -> Unit,

    onDismiss: () -> Unit,

    onSave: () -> Unit

) {


    AlertDialog(

        onDismissRequest =
        onDismiss,

        title = {


            Text(

                stringResource(
                    R.string.settings_title
                )
            )
        },

        text = {


            Column {


                Text(

                    text =
                    stringResource(
                        R.string.settings_explanation
                    ),

                    style =
                    MaterialTheme.typography.bodySmall
                )


                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )


                OutlinedTextField(

                    value =
                    displayName,

                    onValueChange =
                    onNameChange,

                    label = {


                        Text(

                            stringResource(
                                R.string.display_name_label
                            )
                        )
                    },

                    singleLine =
                    true
                )


                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )


                Text(

                    text =
                    "Roll No: $STUDENT_ROLL_NUMBER",

                    style =
                    MaterialTheme.typography.labelMedium
                )
            }
        },

        confirmButton = {


            TextButton(

                onClick =
                onSave
            ) {


                Text(

                    stringResource(
                        R.string.save
                    )
                )
            }
        },

        dismissButton = {


            TextButton(

                onClick =
                onDismiss
            ) {


                Text(

                    stringResource(
                        R.string.cancel
                    )
                )
            }
        }
    )
}


/* ---------------------------------------------------------
   TIME FORMATTERS
   --------------------------------------------------------- */

@Composable
private fun formatTimestamp(
    timestamp: Long
): String {


    val formatter =
        remember {

            SimpleDateFormat(

                "hh:mm a",

                Locale.getDefault()
            )
        }


    return remember(timestamp) {

        formatter.format(
            Date(timestamp)
        )
    }
}


@Composable
private fun formatDrawerTimestamp(
    timestamp: Long
): String {


    val formatter =
        remember {

            SimpleDateFormat(

                "dd MMM • hh:mm a",

                Locale.getDefault()
            )
        }


    return remember(timestamp) {

        formatter.format(
            Date(timestamp)
        )
    }
}
