package com.trilogi.kantin.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFD84315),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    background = Color(0xFFFFF4EC),
    onBackground = Color(0xFF201A18),
    surface = Color.White,
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF872300),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF00390F),
    background = Color(0xFF1A1210),
    onBackground = Color(0xFFF1DFDA),
    surface = Color(0xFF261C19),
    onSurface = Color(0xFFF1DFDA),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BC),
    error = Color(0xFFFFB4AB)
)

@Composable
fun KantinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
