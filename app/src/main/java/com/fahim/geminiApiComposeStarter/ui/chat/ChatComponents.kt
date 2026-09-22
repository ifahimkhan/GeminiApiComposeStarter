package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import java.text.DateFormat
import java.util.Date

private val UserBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
private val GeminiBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

/** Width taken by the avatar and its gap, so bubble width limits account for it. */
internal val AvatarSlot = 40.dp

@Composable
private fun isDarkPalette(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Pink-to-purple gradient used for the user's bubbles and the avatar. */
@Composable
private fun accentBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return if (isDarkPalette()) {
        Brush.linearGradient(listOf(scheme.primaryContainer, scheme.secondaryContainer))
    } else {
        Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
    }
}

@Composable
private fun onAccentColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (isDarkPalette()) scheme.onPrimaryContainer else scheme.onPrimary
}

@Composable
internal fun GeminiAvatar(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(accentBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = stringResource(R.string.gemini),
            modifier = Modifier.size(size * 0.58f),
            tint = onAccentColor(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: ChatMessage,
    maxBubbleWidth: Dp,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.isUser
    val time = remember(message.timestamp) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp))
    }
    val shape = if (isUser) UserBubbleShape else GeminiBubbleShape
    val background = if (isUser) {
        Modifier.background(accentBrush(), shape)
    } else {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer, shape)
    }
    val contentColor = if (isUser) onAccentColor() else MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            GeminiAvatar()
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = maxBubbleWidth),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Box(
                modifier = background
                    .clip(shape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onCopy(message.text) },
                        onLongClickLabel = stringResource(R.string.copy_hint),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (isUser) androidx.compose.ui.text.AnnotatedString(message.text)
                    else message.text.toBoldAnnotatedString(),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp).testTag("time_${message.id}"),
            )
        }
    }
}

/** Shown as the last list item while Gemini is generating a reply. */
@Composable
internal fun TypingBubble(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        GeminiAvatar()
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, GeminiBubbleShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.thinking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
internal fun EmptyState(onSuggestionClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val suggestions = listOf(
        stringResource(R.string.suggestion_one),
        stringResource(R.string.suggestion_two),
        stringResource(R.string.suggestion_three),
    )
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GeminiAvatar(size = 84.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        suggestions.forEach { suggestion ->
            AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

@Composable
internal fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    sendEnabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
            shape = RoundedCornerShape(28.dp),
            minLines = 1,
            maxLines = 4,
            isError = promptError != null,
            supportingText = promptError?.let { { Text(stringResource(R.string.field_cannot_be_empty)) } },
            trailingIcon = {
                IconButton(onClick = onMicClick) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.voice_input),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSend,
            enabled = sendEnabled,
            modifier = Modifier.padding(top = 2.dp).size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}
