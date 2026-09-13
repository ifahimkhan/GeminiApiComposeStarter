package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.R

@Composable
fun SettingsDialog(
    state: ChatUiState,
    onSave: (key: String?, openKeyboard: Boolean, modelName: String, darkMode: Boolean) -> Unit,
    onClose: () -> Unit
) {
    // Intentionally not rememberSaveable: never place an API key in saved instance state.
    var key by remember { mutableStateOf("") }
    var keyboard by remember { mutableStateOf(state.openKeyboard) }
    var model by remember { mutableStateOf(state.modelName) }
    var removeKey by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(state.darkMode) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Appearance ───────────────────────────────────────────────
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                )

                // Dark / Light animated pill toggle
                val trackColor by animateColorAsState(
                    targetValue = if (darkMode) Color(0xFF1E1E1E) else Color(0xFFE8E8E8),
                    animationSpec = tween(300),
                    label = "trackColor"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(trackColor)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Dark option
                    val darkSelected = darkMode
                    val darkBg by animateColorAsState(
                        targetValue = if (darkSelected) Color(0xFF333333) else Color.Transparent,
                        animationSpec = tween(250),
                        label = "darkBg"
                    )
                    val darkContent by animateColorAsState(
                        targetValue = if (darkSelected) Color.White else Color(0xFF888888),
                        animationSpec = tween(250),
                        label = "darkContent"
                    )
                    Surface(
                        onClick = { darkMode = true },
                        modifier = Modifier.weight(1f),
                        color = darkBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_dark_mode),
                                contentDescription = "Dark mode",
                                tint = darkContent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Dark",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (darkSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = darkContent
                                )
                            )
                        }
                    }

                    // Light option
                    val lightSelected = !darkMode
                    val lightBg by animateColorAsState(
                        targetValue = if (lightSelected) Color.White else Color.Transparent,
                        animationSpec = tween(250),
                        label = "lightBg"
                    )
                    val lightContent by animateColorAsState(
                        targetValue = if (lightSelected) Color(0xFF111111) else Color(0xFF888888),
                        animationSpec = tween(250),
                        label = "lightContent"
                    )
                    Surface(
                        onClick = { darkMode = false },
                        modifier = Modifier.weight(1f),
                        color = lightBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_light_mode),
                                contentDescription = "Light mode",
                                tint = lightContent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Light",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (lightSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = lightContent
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // ── API Key ───────────────────────────────────────────────────
                Text(
                    "API KEY",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                )
                Text(
                    if (state.hasSavedKey) "API key saved securely on this device." else "Add your own Gemini API key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Gemini API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.hasSavedKey) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = removeKey,
                            onCheckedChange = { removeKey = it }
                        )
                        Text("Remove saved key", Modifier.padding(start = 4.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // ── Model & Keyboard ─────────────────────────────────────────
                Text(
                    "ADVANCED",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Gemini model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open keyboard on launch", Modifier.weight(1f))
                    Switch(
                        checked = keyboard,
                        onCheckedChange = { keyboard = it }
                    )
                }

                state.settingsMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = model.isNotBlank(),
                onClick = {
                    onSave(
                        if (removeKey) "" else key.takeIf { it.isNotBlank() },
                        keyboard,
                        model,
                        darkMode
                    )
                    key = ""
                    onClose()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        },
    )
}
