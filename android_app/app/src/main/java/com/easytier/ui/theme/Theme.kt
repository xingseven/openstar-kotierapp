package com.easytier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF66CCFF)
private val BgDark = Color(0xFF1A1A2E)
private val SurfaceDark = Color(0xFF16213E)
private val BgLight = Color(0xFFF5F5F5)
private val SurfaceLight = Color.White
private val TextDark = Color.White
private val TextLight = Color(0xFF2D2D2D)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    background = BgLight,
    surface = SurfaceLight,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = TextLight.copy(alpha = 0.6f),
)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    background = BgDark,
    surface = SurfaceDark,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFF0F3460),
    onSurfaceVariant = TextDark.copy(alpha = 0.6f),
)

@Composable
fun EasyTierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
