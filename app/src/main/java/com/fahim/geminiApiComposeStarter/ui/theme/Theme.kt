package com.fahim.geminiApiComposeStarter.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF042148),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF7CBAFF),
    onSecondary = Color(0xFF00325B),
    secondaryContainer = Color(0xFF00497D),
    onSecondaryContainer = Color(0xFFD2E4FF),
    tertiary = Color(0xFF6DD5ED),
    onTertiary = Color(0xFF00363F),
    background = Color(0xFF131314),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF131314),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF282A2D),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF444746),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00639B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2E7FF),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF006877),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFF0F4F9),
    onSurfaceVariant = Color(0xFF444746),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5),
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Dynamic system bars update based on theme
    DisposableEffect(darkTheme) {
        val activity = context as? ComponentActivity
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
        )
        onDispose {}
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}