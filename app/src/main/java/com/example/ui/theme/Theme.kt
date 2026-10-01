package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryBlueDark,
        onPrimary = PrimaryNavy,
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = SecondaryTealLight,
        onSecondary = Color(0xFF042F2C),
        secondaryContainer = Color(0xFF115E59),
        onSecondaryContainer = Color(0xFFCCFBF1),
        tertiary = AccentAmberLight,
        background = BackgroundDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = Color(0xFF334155),
        onSurfaceVariant = TextSecondaryDark,
        error = AlertRed,
        onError = Color.White
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryBlueLight,
        onPrimary = Color.White,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        inversePrimary = Color(0xFF93C5FD),
        secondary = SecondaryTealLight,
        onSecondary = Color.White,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = AccentAmberLight,
        onTertiary = Color.White,
        tertiaryContainer = AccentContainerLight,
        onTertiaryContainer = OnAccentContainerLight,
        background = BackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = TextSecondaryLight,
        outline = OutlineLight,
        outlineVariant = CardBorderLight,
        error = AlertRed,
        onError = Color.White,
        errorContainer = AlertRedContainer,
        onErrorContainer = OnAlertRedContainer
    )

@Composable
fun SocietyGateTheme(
    darkTheme: Boolean = false, // Defaults to Light theme
    dynamicColor: Boolean = false, // Set to false so custom bright brand colors shine consistently
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SocietyGateTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
