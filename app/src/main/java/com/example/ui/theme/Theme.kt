package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SafirTheme(
    darkTheme: Boolean = true,
    colorPresetId: String = "ROYAL_BLUE",
    content: @Composable () -> Unit
) {
    val palette = applyKalbarqColorPalette(colorPresetId)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.outgoingBubbleDark,
            onPrimaryContainer = DarkTextPrimary,
            secondary = palette.primaryLight,
            onSecondary = Color.White,
            secondaryContainer = DarkSurfaceElevated,
            onSecondaryContainer = DarkTextPrimary,
            tertiary = CheckmarkBlue,
            onTertiary = Color.White,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder,
            outlineVariant = DarkBorder,
            error = ErrorRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = palette.primaryDark,
            onPrimary = Color.White,
            primaryContainer = palette.outgoingBubbleLight,
            onPrimaryContainer = LightTextPrimary,
            secondary = palette.primary,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceElevated,
            onSecondaryContainer = LightTextPrimary,
            tertiary = CheckmarkBlue,
            onTertiary = Color.White,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceElevated,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorder,
            outlineVariant = LightBorder,
            error = ErrorRed,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
