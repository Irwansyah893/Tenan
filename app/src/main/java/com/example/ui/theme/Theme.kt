package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrangeDim,
    secondary = SecondaryBrownDim,
    tertiary = TertiaryCreamDim,
    background = EspressoBg,
    surface = EspressoSurface,
    outline = BorderDarkAccent,
    outlineVariant = BorderDarkAccent,
    onPrimary = EspressoBg,
    onSecondary = EspressoBg,
    onTertiary = EspressoBg,
    onBackground = TextLightCream,
    onSurface = TextLightCream
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryBrown,
    tertiary = TertiaryCream,
    background = SleekCreamBg,
    surface = SleekCreamSurface,
    outline = BorderCreamAccent,
    outlineVariant = BorderCreamAccent,
    onPrimary = SleekCreamSurface,
    onSecondary = SleekCreamBg,
    onTertiary = SleekCreamBg,
    onBackground = SecondaryBrown,
    onSurface = SecondaryBrown
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
