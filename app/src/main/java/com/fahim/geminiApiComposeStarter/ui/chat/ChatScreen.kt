package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat

import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {

    val state by
    viewModel.uiState.collectAsStateWithLifecycle()


    MaterialTheme(

        colorScheme =
            if (state.isDarkMode) {

                androidx.compose.material3.darkColorScheme()

            } else {

                androidx.compose.material3.lightColorScheme()
            }

    ) {

        ChatScreenContent(

            state = state,

            onPromptChange =
                viewModel::onPromptChange,

            onSend =
                viewModel::onSend,

            onImageSelected =
                viewModel::onImageSelected,

            onClearImage =
                viewModel::clearSelectedImage,

            onToggleDarkMode =
                viewModel::toggleDarkMode,

            onOpenPreferences =
                viewModel::openPreferences,

            onClosePreferences =
                viewModel::closePreferences,

            onNameChange =
                viewModel::updateNameDraft,

            onSavePreferences =
                viewModel::savePreferences,

            onClearError =
                viewModel::clearError
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    state: ChatUiState,

    onPromptChange: (String) -> Unit,

    onSend: () -> Unit,

    onImageSelected: (String) -> Unit,

    onClearImage: () -> Unit,

    onToggleDarkMode: () -> Unit,

    onOpenPreferences: () -> Unit,

    onClosePreferences: () -> Unit,

    onNameChange: (String) -> Unit,

    onSavePreferences: () -> Unit,

    onClearError: () -> Unit
) {

    val context =
        LocalContext.current


    val listState =
        rememberLazyListState()


    /*
     * Android image picker.
     */
    val imagePicker =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri ->

            uri?.let {

                onImageSelected(
                    it.toString()
                )
            }
        }


    /*
     * Speech-recognition intent.
     */
    val speechIntent =
        remember {

            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "en-US"
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your message"
                )
            }
        }


    /*
     * Speech result launcher.
     */
    val voiceLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.StartActivityForResult()

        ) { result ->

            val recognizedText =
                result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()


            if (
                !recognizedText.isNullOrBlank()
            ) {

                onPromptChange(
                    recognizedText
                )
            }
        }


    /*
     * Microphone permission launcher.
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.RequestPermission()

        ) { granted ->

            if (granted) {

                voiceLauncher.launch(
                    speechIntent
                )
            }
        }


    /*
     * Start voice input.
     */
    fun startVoiceInput() {

        val permission =
            ContextCompat.checkSelfPermission(

                context,

                Manifest.permission.RECORD_AUDIO
            )


        if (
            permission ==
            PackageManager.PERMISSION_GRANTED
        ) {

            voiceLauncher.launch(
                speechIntent
            )

        } else {

            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }


    /*
     * Automatically scroll to the newest message.
     */
    LaunchedEffect(

        state.messages.size,

        state.isLoading,

        state.errorMessage
    ) {

        val itemCount =
            state.messages.size +

                    (if (
                        state.isLoading
                    ) {
                        1
                    } else {
                        0
                    }) +

                    (if (
                        state.promptError != null
                    ) {
                        1
                    } else {
                        0
                    }) +

                    (if (
                        state.errorMessage != null
                    ) {
                        1
                    } else {
                        0
                    })


        if (itemCount > 0) {

            listState.animateScrollToItem(
                itemCount - 1
            )
        }
    }


    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Gemini Chat"
                        )


                        if (
                            state.userName.isNotBlank()
                        ) {

                            Text(

                                text =
                                    "Hi, ${state.userName}",

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )
                        }
                    }
                },


                actions = {

                    /*
                     * Theme toggle.
                     */
                    IconButton(

                        onClick =
                            onToggleDarkMode
                    ) {

                        Text(

                            text =
                                if (
                                    state.isDarkMode
                                ) {
                                    "☀️"
                                } else {
                                    "🌙"
                                }
                        )
                    }


                    /*
                     * Preferences.
                     */
                    TextButton(

                        onClick =
                            onOpenPreferences
                    ) {

                        Text(
                            text =
                                "Preferences"
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 12.dp
                    )
        ) {


            /*
             * ACTUAL CHAT HISTORY.
             *
             * Notice that this uses state.messages,
             * NOT state.inputText.
             *
             * Therefore typing cannot alter old bubbles.
             */
            LazyColumn(

                state =
                    listState,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
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
                        message =
                            message
                    )
                }


                /*
                 * Loading indicator appears as a
                 * temporary item after the messages.
                 */
                if (
                    state.isLoading
                ) {

                    item(
                        key = "loading"
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        16.dp
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            CircularProgressIndicator()
                        }
                    }
                }


                /*
                 * Empty prompt error.
                 */
                state.promptError?.let {

                    item(
                        key =
                            "prompt_error"
                    ) {

                        Text(

                            text =
                                "Please enter a message before sending.",

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        8.dp
                                    ),

                            textAlign =
                                TextAlign.Center,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }


                /*
                 * General error.
                 */
                state.errorMessage?.let { error ->

                    item(
                        key =
                            "error"
                    ) {

                        Card(

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Column(

                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    )
                            ) {

                                Text(

                                    text =
                                        error,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )


                                TextButton(

                                    onClick =
                                        onClearError
                                ) {

                                    Text(
                                        text =
                                            "Dismiss"
                                    )
                                }
                            }
                        }
                    }
                }
            }


            HorizontalDivider()


            /*
             * Image attachment indicator.
             */
            state.selectedImageUri?.let {

                Card(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp
                            )
                ) {

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    10.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(

                            text =
                                "🖼️ Image attached",

                            modifier =
                                Modifier.weight(1f)
                        )


                        TextButton(

                            onClick =
                                onClearImage
                        ) {

                            Text(
                                text =
                                    "Remove"
                            )
                        }
                    }
                }
            }


            /*
             * INPUT BAR.
             */
            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp
                        ),

                verticalAlignment =
                    Alignment.Bottom
            ) {

                OutlinedTextField(

                    /*
                     * IMPORTANT:
                     * This is now inputText.
                     *
                     * It is NOT the message history.
                     */
                    value =
                        state.inputText,

                    onValueChange =
                        onPromptChange,

                    modifier =
                        Modifier.weight(1f),

                    label = {

                        Text(
                            text =
                                "Ask Gemini"
                        )
                    },

                    placeholder = {

                        Text(
                            text =
                                "Type your message..."
                        )
                    },

                    maxLines = 4,

                    enabled =
                        !state.isLoading
                )


                Spacer(
                    modifier =
                        Modifier.width(2.dp)
                )


                /*
                 * Image button.
                 */
                IconButton(

                    onClick = {

                        imagePicker.launch(
                            "image/*"
                        )
                    },

                    enabled =
                        !state.isLoading
                ) {

                    Text(
                        text =
                            "🖼️"
                    )
                }


                /*
                 * Microphone button.
                 */
                IconButton(

                    onClick =
                        ::startVoiceInput,

                    enabled =
                        !state.isLoading
                ) {

                    Text(
                        text =
                            "🎤"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(2.dp)
                )


                /*
                 * Send button.
                 */
                Button(

                    onClick =
                        onSend,

                    enabled =
                        state.inputText.isNotBlank() &&
                                !state.isLoading
                ) {

                    Text(
                        text =
                            "Send"
                    )
                }
            }


            /*
             * Assignment information.
             */
            Text(

                text =
                    "Name: Neelansh Pandey • Roll No: N071 • Assignment 1",

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 8.dp
                        ),

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                textAlign =
                    TextAlign.Center
            )
        }
    }


    /*
     * Preferences dialog.
     */
    if (
        state.showPreferences
    ) {

        AlertDialog(

            onDismissRequest =
                onClosePreferences,


            title = {

                Text(
                    text =
                        "Preferences"
                )
            },


            text = {

                Column {

                    OutlinedTextField(

                        value =
                            state.nameDraft,

                        onValueChange =
                            onNameChange,

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {

                            Text(
                                text =
                                    "Your name"
                            )
                        },

                        singleLine = true
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(

                            text =
                                "Dark mode",

                            modifier =
                                Modifier.weight(1f)
                        )


                        TextButton(

                            onClick =
                                onToggleDarkMode
                        ) {

                            Text(

                                text =
                                    if (
                                        state.isDarkMode
                                    ) {
                                        "ON"
                                    } else {
                                        "OFF"
                                    }
                            )
                        }
                    }
                }
            },


            confirmButton = {

                TextButton(

                    onClick =
                        onSavePreferences
                ) {

                    Text(
                        text =
                            "Save"
                    )
                }
            },


            dismissButton = {

                TextButton(

                    onClick =
                        onClosePreferences
                ) {

                    Text(
                        text =
                            "Cancel"
                    )
                }
            }
        )
    }
}


/*
 * Individual chat bubble.
 */
@Composable
private fun ChatBubble(
    message: ChatMessage
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (
                message.isUser
            ) {

                Arrangement.End

            } else {

                Arrangement.Start
            }
    ) {

        Surface(

            modifier =
                Modifier.fillMaxWidth(
                    0.88f
                ),

            shape =
                MaterialTheme
                    .shapes
                    .large,

            color =
                if (
                    message.isUser
                ) {

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
                    Modifier.padding(
                        14.dp
                    )
            ) {

                Text(

                    text =
                        if (
                            message.isUser
                        ) {
                            "You"
                        } else {
                            "Gemini"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge
                )


                /*
                 * Image indicator inside the
                 * historical user message.
                 */
                if (
                    message.imageUri != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "🖼️ Image attached"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Text(

                    text =
                        message.text,

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )
            }
        }
    }
}