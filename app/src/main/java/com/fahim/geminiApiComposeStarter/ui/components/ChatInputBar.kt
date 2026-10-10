package com.fahim.geminiApiComposeStarter.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.HairlineOutline
import com.fahim.geminiApiComposeStarter.ui.theme.OnPetronasTeal
import com.fahim.geminiApiComposeStarter.ui.theme.PetronasTeal
import com.fahim.geminiApiComposeStarter.ui.theme.SurfaceDark
import com.fahim.geminiApiComposeStarter.ui.theme.TealGlow
import com.fahim.geminiApiComposeStarter.ui.theme.TextPrimary
import com.fahim.geminiApiComposeStarter.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatInputBar(
    prompt: String,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var isFlashing by remember { mutableStateOf(false) }

    // Voice input launcher via RecognizerIntent
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onPromptChange(spokenText)
            }
        }
    }

    // Flash send button animation (teal -> white briefly)
    val sendButtonBgColor by animateColorAsState(
        targetValue = if (isFlashing) Color.White else PetronasTeal,
        animationSpec = tween(durationMillis = 150),
        label = "SendButtonFlashColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = TealGlow)
            .navigationBarsPadding()
            .imePadding(),
        color = SurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Input Mic Button
            IconButton(
                onClick = {
                    try {
                        isListening = true
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "TRANSMIT TO PITWALL...")
                        }
                        voiceLauncher.launch(intent)
                    } catch (e: Exception) {
                        isListening = false
                    }
                },
                enabled = !isLoading
            ) {
                if (isListening) {
                    TyreLoader(size = 20.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isLoading) TextSecondary else PetronasTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Square Monospace Text Field
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "TRANSMIT TO PITWALL...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(2.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PetronasTeal,
                    unfocusedBorderColor = HairlineOutline,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    disabledContainerColor = SurfaceDark
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (prompt.isNotBlank() && !isLoading) {
                        scope.launch {
                            isFlashing = true
                            delay(150)
                            isFlashing = false
                        }
                        onSend()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button as a Teal Filled Square
            val canSend = prompt.isNotBlank() && !isLoading
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (canSend) sendButtonBgColor else HairlineOutline,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .clickable(enabled = canSend) {
                        scope.launch {
                            isFlashing = true
                            delay(150)
                            isFlashing = false
                        }
                        onSend()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    TyreLoader(size = 20.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Transmit",
                        tint = if (canSend) OnPetronasTeal else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun ChatInputBarPreview() {
    GeminiApiComposeStarterTheme {
        ChatInputBar(
            prompt = "What is the pit window for lap 20?",
            isLoading = false,
            onPromptChange = {},
            onSend = {}
        )
    }
}
