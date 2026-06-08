package com.example.bbc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

enum class ScreenSize {
    COMPACT, // 300 - 500
    MEDIUM,  // 500 - 800
    LARGE,   // 800 - 1024
    EXTRA_LARGE // 1024+
}

@Composable
fun ResponsiveLayout(
    content: @Composable (ScreenSize) -> Unit
) {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val screenSize = when {
        width < 500.dp -> ScreenSize.COMPACT
        width < 800.dp -> ScreenSize.MEDIUM
        width < 1024.dp -> ScreenSize.LARGE
        else -> ScreenSize.EXTRA_LARGE
    }
    
    // Use the screenSize to drive layout changes
    content(screenSize)
}
