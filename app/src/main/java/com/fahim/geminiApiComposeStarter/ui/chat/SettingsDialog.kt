package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    maskedApiKey: String,
    isUsingCustomKey: Boolean,
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onSaveKey: (String) -> Unit,
    onResetKey: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text("Gemini API key", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                Text(if (isUsingCustomKey) "Custom key: $maskedApiKey" else "Build default: $maskedApiKey")
                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("Paste a new API key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onSaveKey(keyInput)
                        keyInput = ""
                    }, enabled = keyInput.isNotBlank()) {
                        Text("Save key")
                    }
                    if (isUsingCustomKey) {
                        TextButton(onClick = onResetKey) {
                            Text("Reset to default")
                        }
                    }
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.fillMaxWidth().height(12.dp))

                Text("Theme", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChange(ThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChange(ThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeChange(ThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dynamic color (Android 12+)", modifier = Modifier.weight(1f))
                    Switch(checked = dynamicColorEnabled, onCheckedChange = onDynamicColorChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
