package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF), // Cybernetic Cyan
    secondary = Color(0xFFFF9900), // Amber Alerts
    tertiary = Color(0xFF9B59B6), // OSA Psionics
    background = Color(0xFF05080A), // Deep Space Dark
    surface = Color(0xFF0A1A1F), // Geometric metallic slate
    onPrimary = Color(0xFF05080A),
    onSecondary = Color(0xFF05080A),
    onBackground = Color(0xFFF1F5F9), // slate-100
    onSurface = Color(0xFFCBD5E1) // slate-300
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Keep it turned off for uniform pixel art styling
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
