package com.fahim.geminiApiComposeStarter.ui.model

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.data.AiProvider

@Composable
fun ModelSelectorScreen(
    selectedProvider: AiProvider,
    availableProviders: Set<AiProvider>,
    widthSizeClass: WindowWidthSizeClass,
    onSelect: (AiProvider) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Compact) 680.dp else 840.dp
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("Back to chat") }
                Text("Choose a model", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Select the AI provider for your next message.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AiProvider.entries.forEach { provider ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(provider) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(provider.modelId, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (provider in availableProviders) "Key configured"
                                        else "Add ${provider.keyProperty} to local.properties",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (provider in availableProviders) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                    )
                                }
                                RadioButton(selected = provider == selectedProvider, onClick = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
