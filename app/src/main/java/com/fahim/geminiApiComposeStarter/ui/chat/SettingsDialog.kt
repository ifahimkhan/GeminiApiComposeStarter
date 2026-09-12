package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(state: ChatUiState, onSave: (String?, Boolean, String) -> Unit, onClose: () -> Unit) {
    // Intentionally not rememberSaveable: never place an API key in saved instance state.
    var key by remember { mutableStateOf("") }
    var keyboard by remember { mutableStateOf(state.openKeyboard) }
    var model by remember { mutableStateOf(state.modelName) }
    var removeKey by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Chats stay on this device. Gemini receives conversation text when you send a message.")
                Text(if (state.hasSavedKey) "API key saved securely on this device." else "Add your own Gemini API key.")
                OutlinedTextField(key, { key = it }, label = { Text("Gemini API key") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true)
                if (state.hasSavedKey) {
                    Row { Checkbox(removeKey, { removeKey = it }); Text("Remove saved key", Modifier.padding(top = 12.dp)) }
                }
                OutlinedTextField(model, { model = it }, label = { Text("Gemini model") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Open keyboard on launch", Modifier.weight(1f).padding(top = 12.dp))
                    Switch(keyboard, { keyboard = it })
                }
                state.settingsMessage?.let { Text(it) }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(
            enabled = model.isNotBlank(),
            onClick = { onSave(if (removeKey) "" else key.takeIf { it.isNotBlank() }, keyboard, model); key = "" },
        ) { Text("Save") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Done") } },
    )
}
