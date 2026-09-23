package com.fahim.geminiApiComposeStarter.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationDestination(
    val title: String,
    val icon: ImageVector,
    val description: String,
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "Study Overview & Quick Actions"),
    CHAT("Workspace", Icons.Default.Chat, "AI Study Chat & Tools"),
    STUDY_WORKFLOWS("Workflows", Icons.Default.School, "Quizzes, Flashcards & Plans"),
    KNOWLEDGE_LIBRARY("Library", Icons.Default.LibraryBooks, "Saved Notes & Study Materials"),
    MEMORY("Memory", Icons.Default.AutoAwesome, "Personal AI Knowledge"),
    SETTINGS("Settings", Icons.Default.Settings, "Workspace & AI Preferences"),
}
