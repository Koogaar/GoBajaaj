package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SomaliLightColorScheme = lightColorScheme(
    primary = GoBajaajNavy,
    onPrimary = Color.White,
    primaryContainer = GoBajaajBlueLight,
    onPrimaryContainer = GoBajaajNavy,
    secondary = GoBajaajAmber,
    onSecondary = BrandBlack,
    secondaryContainer = GoBajaajYellow,
    onSecondaryContainer = BrandBlack,
    tertiary = GoBajaajBlue,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = BrandBlack,
    onSurface = BrandBlack,
    onSurfaceVariant = TextBlack,
    outline = BrandBorder,
    error = TrafficHeavyRed
)

private val SomaliDarkColorScheme = lightColorScheme(
    primary = GoBajaajNavy,
    onPrimary = Color.White,
    primaryContainer = GoBajaajBlueLight,
    onPrimaryContainer = GoBajaajNavy,
    secondary = GoBajaajAmber,
    onSecondary = BrandBlack,
    secondaryContainer = GoBajaajYellow,
    onSecondaryContainer = BrandBlack,
    tertiary = GoBajaajBlue,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = BrandBlack,
    onSurface = BrandBlack,
    onSurfaceVariant = TextBlack,
    outline = BrandBorder,
    error = TrafficHeavyRed
)

@Composable
fun GoBajaajTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SomaliDarkColorScheme else SomaliLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun SomaliTrafficTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    GoBajaajTheme(darkTheme = darkTheme, content = content)
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GoBajaajTheme(darkTheme = darkTheme, content = content)
}
