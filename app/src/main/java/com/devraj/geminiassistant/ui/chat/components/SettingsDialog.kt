package com.devraj.geminiassistant.ui.chat.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

val AVAILABLE_MODELS = listOf(
    "gemini-3.6-flash" to "Gemini 3.6 Flash (Recommended & Active)",
    "gemini-2.0-flash" to "Gemini 2.0 Flash (Next-Gen)",
    "gemini-1.5-flash-latest" to "Gemini 1.5 Flash (Latest)"
)

val PERSONA_PRESETS = listOf(
    "Helpful AI" to "You are an intelligent, concise, and helpful AI assistant.",
    "Android Architect" to "You are a Principal Android Engineer specializing in Jetpack Compose, Kotlin Coroutines, and Clean Architecture. Provide concise, clean, and production-ready code.",
    "Code Tutor" to "You are a friendly computer science instructor who explains programming concepts step-by-step with clear examples.",
    "Concise Bot" to "Answer questions with extreme brevity. Avoid filler words, get straight to the point."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentInstruction: String,
    currentTemperature: Float,
    currentModel: String = "gemini-3.6-flash",
    onDismiss: () -> Unit,
    onSave: (instruction: String, temperature: Float, model: String) -> Unit
) {
    var instruction by remember { mutableStateOf(currentInstruction) }
    var temperature by remember { mutableFloatStateOf(currentTemperature) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "AI Model & Preferences",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp)
            ) {
                // Model Selector Dropdown
                Text(
                    text = "Gemini Model",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = AVAILABLE_MODELS.find { it.first == selectedModel }?.second ?: selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false }
                    ) {
                        AVAILABLE_MODELS.forEach { (modelId, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedModel = modelId
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Persona Presets Chips
                Text(
                    text = "Persona Presets",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PERSONA_PRESETS.forEach { (label, presetInstruction) ->
                        FilterChip(
                            selected = (instruction == presetInstruction),
                            onClick = { instruction = presetInstruction },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom System Instruction
                Text(
                    text = "System Instruction",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    placeholder = { Text("Custom behavior or persona...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Temperature (Creativity)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..1.0f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (temperature < 0.4f) "Focused, deterministic & analytical"
                    else if (temperature > 0.8f) "Creative, exploratory & varied"
                    else "Balanced accuracy & creativity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(instruction, temperature, selectedModel)
                }
            ) {
                Text("Save Preferences")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Overload for backward compatibility
@Composable
fun SettingsDialog(
    currentInstruction: String,
    currentTemperature: Float,
    onDismiss: () -> Unit,
    onSave: (instruction: String, temperature: Float) -> Unit
) {
    SettingsDialog(
        currentInstruction = currentInstruction,
        currentTemperature = currentTemperature,
        currentModel = "gemini-3.6-flash",
        onDismiss = onDismiss,
        onSave = { instruction, temperature, _ ->
            onSave(instruction, temperature)
        }
    )
}
