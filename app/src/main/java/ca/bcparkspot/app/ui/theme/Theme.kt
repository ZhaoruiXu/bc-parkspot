package ca.bcparkspot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemePreference { SYSTEM, LIGHT, DARK }

fun resolveDarkTheme(preference: ThemePreference, systemIsDark: Boolean): Boolean = when (preference) {
    ThemePreference.SYSTEM -> systemIsDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

fun windowBackgroundFor(dark: Boolean): Color = if (dark) DarkColorScheme.background else LightColorScheme.background

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

object BCParkSpotColors {
    val semantic: SemanticColors
        @Composable @ReadOnlyComposable get() = LocalSemanticColors.current
}

// Dynamic (wallpaper) colour is deliberately not supported: the brand palette is the design.
@Composable
fun BCParkSpotTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = resolveDarkTheme(preference, isSystemInDarkTheme())
    CompositionLocalProvider(LocalSemanticColors provides if (dark) DarkSemantic else LightSemantic) {
        MaterialTheme(
            colorScheme = if (dark) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
