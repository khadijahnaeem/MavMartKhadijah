package com.example.mavmart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnPrimary,
    background = BackgroundOrange,
    onBackground = OnBackground,
    surface = SurfaceColor,
    onSurface = OnSurface,
    // optional extras:
    secondary = BluePrimary,
    onSecondary = OnPrimary
)

@Composable
fun MavMartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = Typography,
        content = content
    )
}

