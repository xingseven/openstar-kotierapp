package com.easytier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightPrimary = Color(0xFF1868F5)
private val LightSecondary = Color(0xFF7FAEFF)
private val LightBackground = Color(0xFFF2F6FC)
private val LightSurface = Color(0xF2FFFFFF)
private val LightSurfaceVariant = Color(0xE6F0FF)
private val LightText = Color(0xFF16314F)
private val LightMutedText = Color(0xFF607A99)
private val LightOutline = Color(0xFFB8CBE5)

private val DarkPrimary = Color(0xFF8FB8FF)
private val DarkSecondary = Color(0xFF4E7BE5)
private val DarkBackground = Color(0xFF09111D)
private val DarkSurface = Color(0xD9192538)
private val DarkSurfaceVariant = Color(0xCC20314C)
private val DarkText = Color(0xFFF3F7FF)
private val DarkMutedText = Color(0xFFA8B9D5)
private val DarkOutline = Color(0xFF3D5373)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E7FF),
    onPrimaryContainer = Color(0xFF0A2D72),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EDFF),
    onSecondaryContainer = Color(0xFF173A7B),
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedText,
    outline = LightOutline,
    surfaceTint = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF08111E),
    primaryContainer = Color(0xFF163154),
    onPrimaryContainer = Color(0xFFD7E6FF),
    secondary = DarkSecondary,
    onSecondary = DarkText,
    secondaryContainer = Color(0xFF213755),
    onSecondaryContainer = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedText,
    outline = DarkOutline,
    surfaceTint = Color.White.copy(alpha = 0.18f),
)

private val EasyTierShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun EasyTierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = EasyTierShapes,
        content = content
    )
}
