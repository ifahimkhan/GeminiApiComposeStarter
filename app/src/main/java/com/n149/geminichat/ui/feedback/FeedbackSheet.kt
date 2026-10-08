package com.n149.geminichat.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n149.geminichat.ui.theme.DeepBlack

/**
 * Modal bottom sheet where the user types free-form feedback about a
 * Gemini response in their own words.
 *
 * No ratings, no emojis — just text.
 * The saved text is later parsed by PersonalisationEngine to adapt
 * future answers (length, depth, style, accuracy, examples…).
 *
 * @param existingText  Pre-filled if the user already left a note (editing).
 * @param onSubmit      Called with the trimmed text when the user taps Send.
 * @param onDismiss     Called when the user taps outside or cancels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    existingText: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(existingText) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus keyboard when sheet opens
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepBlack.Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(DeepBlack.Divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────
            Text(
                text = if (existingText.isBlank()) "Leave feedback" else "Edit your feedback",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepBlack.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tell Gemini what you thought of this answer.\n" +
                       "Your words are used to personalise future replies.",
                fontSize = 13.sp,
                color = DeepBlack.TextSecondary,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(16.dp))

            // ── Examples row ─────────────────────────────────────────────
            Text(
                text = "Examples of useful feedback:",
                fontSize = 11.sp,
                color = DeepBlack.TextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            listOf(
                "Too long, keep it shorter",
                "Explain more simply",
                "Give me a code example",
                "This answer was wrong",
                "Perfect, keep this style"
            ).forEach { example ->
                FeedbackExampleChip(
                    label = example,
                    onClick = { text = example }
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Text input ───────────────────────────────────────────────
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 400) text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "Write your feedback here…",
                        color = DeepBlack.TextMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 6,
                minLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (text.isNotBlank()) onSubmit(text) }
                ),
                supportingText = {
                    Text("${text.length}/400", color = DeepBlack.TextMuted, fontSize = 11.sp)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor       = DeepBlack.TextPrimary,
                    unfocusedTextColor     = DeepBlack.TextPrimary,
                    cursorColor            = DeepBlack.AccentGlow,
                    focusedBorderColor     = DeepBlack.Accent,
                    unfocusedBorderColor   = DeepBlack.Divider,
                    focusedContainerColor  = DeepBlack.Elevated,
                    unfocusedContainerColor= DeepBlack.Elevated
                )
            )

            Spacer(Modifier.height(16.dp))

            // ── Submit button ────────────────────────────────────────────
            Button(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = DeepBlack.Accent,
                    disabledContainerColor = DeepBlack.Elevated
                )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = if (text.isNotBlank()) Color.White else DeepBlack.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Submit feedback",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (text.isNotBlank()) Color.White else DeepBlack.TextMuted
                )
            }
        }
    }
}

/**
 * A tappable chip that pre-fills the feedback text field
 * with a common example phrase.
 */
@Composable
private fun FeedbackExampleChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = DeepBlack.Elevated,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Text(
            text = "\"$label\"",
            color = DeepBlack.AccentGlow,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
