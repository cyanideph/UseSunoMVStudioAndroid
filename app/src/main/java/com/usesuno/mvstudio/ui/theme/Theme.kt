package com.usesuno.mvstudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color palette matching UseSunoMVStudio's dark theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFF006D6C),
    tertiary = Color(0xFFFF0187),
    tertiaryContainer = Color(0xFF6D004A),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    surfaceContainer = Color(0xFF252525),
    surfaceContainerHigh = Color(0xFF2E2E2E),
    surfaceContainerHighest = Color(0xFF383838),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF7F7F7F),
    outlineVariant = Color(0xFF4F4F4F),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF6200EE),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    primaryContainer = Color(0xFFE8E0FF),
    secondary = Color(0xFF018786),
    secondaryContainer = Color(0xFF00F5F3),
    tertiary = Color(0xFFB0005E),
    tertiaryContainer = Color(0xFFFFE0F0),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    onBackground = Color(0xFF1F1F1F),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF4F4F4F),
    outline = Color(0xFF7F7F7F),
    outlineVariant = Color(0xFFB0B0B0),
    inverseSurface = Color(0xFF1F1F1F),
    inverseOnSurface = Color(0xFFFAFAFA),
    inversePrimary = Color(0xFFBB86FC),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
)

@Composable
fun UseSunoMVStudioTheme(
    darkTheme: Boolean = true, // Force dark theme like the web app
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

object Typography {
    val displayLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = -0.25.dp,
    )

    val displayMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.dp,
    )

    val displaySmall = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.dp,
    )

    val headlineLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.dp,
    )

    val headlineMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.dp,
    )

    val headlineSmall = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.dp,
    )

    val titleLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.dp,
    )

    val titleMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.dp,
    )

    val titleSmall = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.dp,
    )

    val bodyLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.dp,
    )

    val bodyMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.dp,
    )

    val bodySmall = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.dp,
    )

    val labelLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.dp,
    )

    val labelMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.dp,
    )

    val labelSmall = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.dp,
    )
}

object Shapes {
    val extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    val small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
}