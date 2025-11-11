package com.example.mavmart.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BluePrimary,          // navy
    onPrimary = Color.White,
    background = BackgroundOrange,  // orange
    onBackground = Color.Black,
    surface = Color.White,          // white
    onSurface = Color.Black,        // black
    secondary = BluePrimary,
    onSecondary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BackgroundOrange,     // orange
    onPrimary = Color.Black,
    background = BluePrimary,       // navy
    onBackground = Color.White,
    surface = Color.Black,          // black
    onSurface = Color.White,        // white
    secondary = BackgroundOrange,
    onSecondary = Color.Black
)

@Composable
fun MavMartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}