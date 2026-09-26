package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.ui.components.*
import com.fahim.geminiApiComposeStarter.ui.theme.*

@Composable
internal fun ModelPill(state: ChatUiState, onClick: () -> Unit) {
    val model = GeminiModels.selected(state.preferences.modelId)
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xA5082D56),
        border = BorderStroke(.7.dp, Color(0xFF456489)),
        modifier = Modifier.widthIn(max = 240.dp).testTag("model-selector")
            .semantics { contentDescription = "Choose model, ${model.title}" }
            .judyClickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(Brush.radialGradient(listOf(Color(0xAA6974FF), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFF9CAFFF), modifier = Modifier.size(17.dp))
            }
            Column(Modifier.padding(horizontal = 7.dp).weight(1f, fill = false)) {
                Text(if (state.category == Category.GENERAL) "AI Host" else if (state.category == Category.RESEARCH) "Research" else "Health", fontSize = 14.sp, lineHeight = 17.sp, color = Color.White)
                Text(model.title.removePrefix("Gemini "), fontSize = 10.sp, lineHeight = 14.sp, color = Color(0xFFADC7EB), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Color(0xFFDBE8FF), modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun ModelPicker(state: ChatUiState, actions: ChatActions) {
    val selected = GeminiModels.selected(state.preferences.modelId)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).testTag("model-picker")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Brush.linearGradient(listOf(Color(0xFF254785), Color(0xFF27213F))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFFAFCBFF), modifier = Modifier.size(22.dp))
            }
            Text("JUDY  /  MODELS", color = Color(0xFFA9BCE0), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(start = 12.dp))
        }
        Text("A mind for every idea.", color = Color.White, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-.8).sp, modifier = Modifier.padding(top = 20.dp))
        Text("Choose how Judy helps you.", color = MutedText, fontSize = 14.sp, modifier = Modifier.padding(top = 7.dp, bottom = 24.dp))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GeminiModels.options.forEachIndexed { index, model ->
                val active = selected.id == model.id
                val border by animateColorAsState(if (active) Color(0xFF699EF0) else Color(0xFF303C53), if (LocalReducedMotion.current) snap() else tween(180), label = "model selection")
                val icon = when {
                    model.id.contains("pro") -> Icons.Outlined.Psychology
                    model.id.contains("lite") -> Icons.Outlined.Bolt
                    index == 0 -> Icons.Outlined.AutoAwesome
                    else -> Icons.Outlined.Code
                }
                Surface(shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = BorderStroke(if (active) 1.dp else .7.dp, border),
                    modifier = Modifier.fillMaxWidth().testTag("model-${model.id}").selectable(active, role = Role.RadioButton, onClick = { actions.model(model.id) })) {
                    Row(Modifier.background(Brush.linearGradient(if (active) listOf(Color(0xFF173B6B), Color(0xFF17243E)) else listOf(Color(0xFF151F32), Color(0xFF101723)))).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(39.dp).background(if (active) Color(0x663C83DB) else Color(0xFF202C42), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = if (active) Color(0xFFBDD8FF) else Color(0xFF90A2C2), modifier = Modifier.size(21.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(model.badge, color = if (active) Color(0xFF94BCFF) else Color(0xFF94A4BE), fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                            Text(model.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
                            Text(model.detail, color = Color(0xFFA2AFC4), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp))
                        }
                        Box(Modifier.size(21.dp).background(if (active) ElectricBlue else Color.Transparent, CircleShape).border(1.dp, if (active) Color(0xFF80B5FF) else Color(0xFF53617A), CircleShape), contentAlignment = Alignment.Center) {
                            if (active) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        Text(if (state.isLoading) "Your reply is still on its way. This choice applies to your next message." else "Saved for your next message. Model availability depends on your account.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 16.dp))
        HorizontalDivider(Modifier.padding(top = 20.dp, bottom = 8.dp), color = Color(0xFF28344A))
        TextButton(onClick = { actions.sheet(Sheet.HOST) }, modifier = Modifier.fillMaxWidth()) {
            Text("Choose an assistant", color = Color(0xFFBCD3FA), modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color(0xFFBCD3FA), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(22.dp))
    }
}
