package com.example.scholartrack

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = YellowDark,
    tertiary = BlueSecondary,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkSlate,
    onSurface = DarkSlate,
)

@Composable
fun ScholarTrackTheme(
    content: @Composable () -> Unit
) {
    // Always use LightColorScheme to remove dark theme support
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
