package com.example.c008_nishit_assignment1.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Bottom prompt input bar featuring text field, voice input launcher, and send button.
 */
@Composable
fun PromptBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNull_or_Empty()) {
                onPromptChange(spokenText!!)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            placeholder = { Text("Ask Gemini...") },
            enabled = !isLoading,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            maxLines = 4
        )

        IconButton(
            onClick = {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
                    }
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Voice input is not supported on this device.", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading
        ) {
            Text("🎤")
        }

        Button(
            onClick = {
                if (prompt.isNotBlank()) {
                    onSendPrompt(prompt)
                }
            },
            enabled = !isLoading && prompt.isNotBlank()
        ) {
            Text("Send")
        }
    }
}

private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()
