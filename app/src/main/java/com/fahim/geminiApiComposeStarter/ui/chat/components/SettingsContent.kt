package com.fahim.geminiApiComposeStarter.ui.chat.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences

/** Stateless preferences UI; used inside a dialog on phones and as a side pane on tablets. */
@Composable
fun SettingsContent(
    preferences: UserPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onResponseStyleChange: (ResponseStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionTitle(stringResource(R.string.settings_response_style))
        Column(Modifier.selectableGroup()) {
            ResponseStyle.entries.forEach { style ->
                RadioRow(
                    label = stringResource(style.labelRes()),
                    selected = preferences.responseStyle == style,
                    onClick = { onResponseStyleChange(style) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.settings_theme))
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = stringResource(mode.labelRes()),
                    selected = preferences.themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferences.dynamicColor,
                        role = Role.Switch,
                        onValueChange = onDynamicColorChange,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_dynamic_color),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = preferences.dynamicColor, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun ResponseStyle.labelRes(): Int = when (this) {
    ResponseStyle.CONCISE -> R.string.style_concise
    ResponseStyle.BALANCED -> R.string.style_balanced
    ResponseStyle.DETAILED -> R.string.style_detailed
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
