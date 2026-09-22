package com.fahim.geminiApiComposeStarter.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Material 3 WindowSizeClass specification.
 * Provides breakpoints for responsive, adaptive layouts across phones, foldables, and tablets.
 */
enum class WindowWidthSizeClass {
    Compact,   // Typical phone in portrait (< 600dp)
    Medium,    // Small tablet / phone in landscape (600dp .. 839dp)
    Expanded,  // Large tablet / desktop (>= 840dp)
}

enum class WindowHeightSizeClass {
    Compact,   // Phone in landscape (< 480dp)
    Medium,    // Typical phone in portrait (480dp .. 899dp)
    Expanded,  // Tablet in portrait / desktop (>= 900dp)
}

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
) {
    companion object {
        fun calculateFromDimensions(widthDp: Int, heightDp: Int): WindowSizeClass {
            val width = when {
                widthDp < 600 -> WindowWidthSizeClass.Compact
                widthDp < 840 -> WindowWidthSizeClass.Medium
                else -> WindowWidthSizeClass.Expanded
            }
            val height = when {
                heightDp < 480 -> WindowHeightSizeClass.Compact
                heightDp < 900 -> WindowHeightSizeClass.Medium
                else -> WindowHeightSizeClass.Expanded
            }
            return WindowSizeClass(width, height)
        }
    }
}

/**
 * Calculates current WindowSizeClass adaptively using Compose LocalConfiguration.
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return WindowSizeClass.calculateFromDimensions(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
    )
}
