package ca.bcparkspot.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// The only file allowed to contain colour literals (enforced by ColorLiteralScanTest).

// Each scheme is built from a role map keyed by ColorScheme property name, so ThemePaletteTest can check every role is set.
private fun lightSchemeFrom(roles: Map<String, Color>): ColorScheme = lightColorScheme(
    primary = roles.getValue("primary"),
    onPrimary = roles.getValue("onPrimary"),
    primaryContainer = roles.getValue("primaryContainer"),
    onPrimaryContainer = roles.getValue("onPrimaryContainer"),
    inversePrimary = roles.getValue("inversePrimary"),
    secondary = roles.getValue("secondary"),
    onSecondary = roles.getValue("onSecondary"),
    secondaryContainer = roles.getValue("secondaryContainer"),
    onSecondaryContainer = roles.getValue("onSecondaryContainer"),
    tertiary = roles.getValue("tertiary"),
    onTertiary = roles.getValue("onTertiary"),
    tertiaryContainer = roles.getValue("tertiaryContainer"),
    onTertiaryContainer = roles.getValue("onTertiaryContainer"),
    background = roles.getValue("background"),
    onBackground = roles.getValue("onBackground"),
    surface = roles.getValue("surface"),
    onSurface = roles.getValue("onSurface"),
    surfaceVariant = roles.getValue("surfaceVariant"),
    onSurfaceVariant = roles.getValue("onSurfaceVariant"),
    surfaceTint = roles.getValue("surfaceTint"),
    inverseSurface = roles.getValue("inverseSurface"),
    inverseOnSurface = roles.getValue("inverseOnSurface"),
    error = roles.getValue("error"),
    onError = roles.getValue("onError"),
    errorContainer = roles.getValue("errorContainer"),
    onErrorContainer = roles.getValue("onErrorContainer"),
    outline = roles.getValue("outline"),
    outlineVariant = roles.getValue("outlineVariant"),
    scrim = roles.getValue("scrim"),
    surfaceBright = roles.getValue("surfaceBright"),
    surfaceContainer = roles.getValue("surfaceContainer"),
    surfaceContainerHigh = roles.getValue("surfaceContainerHigh"),
    surfaceContainerHighest = roles.getValue("surfaceContainerHighest"),
    surfaceContainerLow = roles.getValue("surfaceContainerLow"),
    surfaceContainerLowest = roles.getValue("surfaceContainerLowest"),
    surfaceDim = roles.getValue("surfaceDim")
)

private fun darkSchemeFrom(roles: Map<String, Color>): ColorScheme = darkColorScheme(
    primary = roles.getValue("primary"),
    onPrimary = roles.getValue("onPrimary"),
    primaryContainer = roles.getValue("primaryContainer"),
    onPrimaryContainer = roles.getValue("onPrimaryContainer"),
    inversePrimary = roles.getValue("inversePrimary"),
    secondary = roles.getValue("secondary"),
    onSecondary = roles.getValue("onSecondary"),
    secondaryContainer = roles.getValue("secondaryContainer"),
    onSecondaryContainer = roles.getValue("onSecondaryContainer"),
    tertiary = roles.getValue("tertiary"),
    onTertiary = roles.getValue("onTertiary"),
    tertiaryContainer = roles.getValue("tertiaryContainer"),
    onTertiaryContainer = roles.getValue("onTertiaryContainer"),
    background = roles.getValue("background"),
    onBackground = roles.getValue("onBackground"),
    surface = roles.getValue("surface"),
    onSurface = roles.getValue("onSurface"),
    surfaceVariant = roles.getValue("surfaceVariant"),
    onSurfaceVariant = roles.getValue("onSurfaceVariant"),
    surfaceTint = roles.getValue("surfaceTint"),
    inverseSurface = roles.getValue("inverseSurface"),
    inverseOnSurface = roles.getValue("inverseOnSurface"),
    error = roles.getValue("error"),
    onError = roles.getValue("onError"),
    errorContainer = roles.getValue("errorContainer"),
    onErrorContainer = roles.getValue("onErrorContainer"),
    outline = roles.getValue("outline"),
    outlineVariant = roles.getValue("outlineVariant"),
    scrim = roles.getValue("scrim"),
    surfaceBright = roles.getValue("surfaceBright"),
    surfaceContainer = roles.getValue("surfaceContainer"),
    surfaceContainerHigh = roles.getValue("surfaceContainerHigh"),
    surfaceContainerHighest = roles.getValue("surfaceContainerHighest"),
    surfaceContainerLow = roles.getValue("surfaceContainerLow"),
    surfaceContainerLowest = roles.getValue("surfaceContainerLowest"),
    surfaceDim = roles.getValue("surfaceDim")
)

// Brand: forest green on warm paper (light) and mint on night navy (dark).
private val ForestGreen = Color(0xFF1F5C45)
private val Mint = Color(0xFF34D399)

val LightRoles: Map<String, Color> = mapOf(
    "primary" to ForestGreen,
    "onPrimary" to Color(0xFFFFFFFF),
    "primaryContainer" to Color(0xFFCFE9DA),
    "onPrimaryContainer" to Color(0xFF0B3325),
    "inversePrimary" to Color(0xFF9BD3B6),
    "secondary" to Color(0xFF3F6354),
    "onSecondary" to Color(0xFFFFFFFF),
    "secondaryContainer" to Color(0xFFD3E8DC),
    "onSecondaryContainer" to Color(0xFF0D2B1F),
    "tertiary" to Color(0xFF8A5A12),
    "onTertiary" to Color(0xFFFFFFFF),
    "tertiaryContainer" to Color(0xFFFBE2BC),
    "onTertiaryContainer" to Color(0xFF3D2600),
    "background" to Color(0xFFFAF7F0),
    "onBackground" to Color(0xFF1C1B17),
    "surface" to Color(0xFFFAF7F0),
    "onSurface" to Color(0xFF1C1B17),
    "surfaceVariant" to Color(0xFFE7E2D6),
    "onSurfaceVariant" to Color(0xFF4A473F),
    "surfaceTint" to ForestGreen,
    "inverseSurface" to Color(0xFF31302B),
    "inverseOnSurface" to Color(0xFFF3EFE6),
    "error" to Color(0xFFB3261E),
    "onError" to Color(0xFFFFFFFF),
    "errorContainer" to Color(0xFFF9DEDC),
    "onErrorContainer" to Color(0xFF410E0B),
    "outline" to Color(0xFF7A766B),
    "outlineVariant" to Color(0xFFCBC6B9),
    "scrim" to Color(0xFF000000),
    "surfaceBright" to Color(0xFFFAF7F0),
    "surfaceContainer" to Color(0xFFEFEBE1),
    "surfaceContainerHigh" to Color(0xFFE9E5DB),
    "surfaceContainerHighest" to Color(0xFFE3DFD5),
    "surfaceContainerLow" to Color(0xFFF5F1E8),
    "surfaceContainerLowest" to Color(0xFFFFFFFF),
    "surfaceDim" to Color(0xFFDDD9CF)
)

val DarkRoles: Map<String, Color> = mapOf(
    "primary" to Mint,
    "onPrimary" to Color(0xFF053B2C),
    "primaryContainer" to Color(0xFF0F4C3A),
    "onPrimaryContainer" to Color(0xFFA7F3D0),
    "inversePrimary" to ForestGreen,
    "secondary" to Color(0xFF38BDF8),
    "onSecondary" to Color(0xFF082F49),
    "secondaryContainer" to Color(0xFF0B4A6F),
    "onSecondaryContainer" to Color(0xFFBAE6FD),
    "tertiary" to Color(0xFFFBBF24),
    "onTertiary" to Color(0xFF451A03),
    "tertiaryContainer" to Color(0xFF6B3A0A),
    "onTertiaryContainer" to Color(0xFFFDE68A),
    "background" to Color(0xFF0B131E),
    "onBackground" to Color(0xFFF1F5F9),
    "surface" to Color(0xFF0B131E),
    "onSurface" to Color(0xFFF1F5F9),
    "surfaceVariant" to Color(0xFF1E293B),
    "onSurfaceVariant" to Color(0xFFA5B3C6),
    "surfaceTint" to Mint,
    "inverseSurface" to Color(0xFFE2E8F0),
    "inverseOnSurface" to Color(0xFF1E293B),
    "error" to Color(0xFFF2B8B5),
    "onError" to Color(0xFF601410),
    "errorContainer" to Color(0xFF8C1D18),
    "onErrorContainer" to Color(0xFFF9DEDC),
    "outline" to Color(0xFF7287A0),
    "outlineVariant" to Color(0xFF334155),
    "scrim" to Color(0xFF000000),
    "surfaceBright" to Color(0xFF2A3747),
    "surfaceContainer" to Color(0xFF131F2E),
    "surfaceContainerHigh" to Color(0xFF1A2738),
    "surfaceContainerHighest" to Color(0xFF22303F),
    "surfaceContainerLow" to Color(0xFF101A27),
    "surfaceContainerLowest" to Color(0xFF08101A),
    "surfaceDim" to Color(0xFF0B131E)
)

val LightColorScheme: ColorScheme = lightSchemeFrom(LightRoles)
val DarkColorScheme: ColorScheme = darkSchemeFrom(DarkRoles)

/** A meaning-carrying colour family: [container] fills, [onContainer] is text on it, [accent] is icons, dots and text on surfaces. */
@Immutable
data class Tone(val container: Color, val onContainer: Color, val accent: Color)

@Immutable
data class SemanticColors(
    val positive: Tone,
    val caution: Tone,
    val negative: Tone,
    val closed: Tone,
    val neutral: Tone,
    val info: Tone
) {
    val all: Map<String, Tone> get() = mapOf(
        "positive" to positive, "caution" to caution, "negative" to negative,
        "closed" to closed, "neutral" to neutral, "info" to info
    )
}

val LightSemantic = SemanticColors(
    positive = Tone(Color(0xFFDDF3E4), Color(0xFF0E4F2E), Color(0xFF146A3A)),
    caution = Tone(Color(0xFFFCEFD2), Color(0xFF6B3A05), Color(0xFF8A4200)),
    negative = Tone(Color(0xFFFBE3E0), Color(0xFF7A1712), Color(0xFFB3261E)),
    closed = Tone(Color(0xFFE4E1DA), Color(0xFF3A3833), Color(0xFF57534E)),
    neutral = Tone(Color(0xFFECE8DF), Color(0xFF45423B), Color(0xFF625E55)),
    info = Tone(Color(0xFFDCEBF7), Color(0xFF0E3A5C), Color(0xFF1F5F95))
)

val DarkSemantic = SemanticColors(
    positive = Tone(Color(0xFF12372A), Color(0xFFA7F3D0), Color(0xFF4ADE80)),
    caution = Tone(Color(0xFF3D2A08), Color(0xFFFDE68A), Color(0xFFFBBF24)),
    negative = Tone(Color(0xFF43171A), Color(0xFFFECACA), Color(0xFFFC9A9A)),
    closed = Tone(Color(0xFF2A3240), Color(0xFFCBD5E1), Color(0xFFA0AEC0)),
    neutral = Tone(Color(0xFF1E293B), Color(0xFFCBD5E1), Color(0xFF9AA8BC)),
    info = Tone(Color(0xFF10304A), Color(0xFFBAE6FD), Color(0xFF60A5FA))
)

/** Colours drawn over the hero photo; the photo is dark in both themes, so these do not change with the theme. */
object HeroColors {
    val content = Color(0xFFFFFFFF)
    val accent = Mint
    val scrim = Color(0xFF000000)
    const val SCRIM_TOP_ALPHA = 0.35f
    const val SCRIM_BOTTOM_ALPHA = 0.75f
    const val SCRIM_CHIP_ALPHA = 0.4f
    const val SUBTITLE_ALPHA = 0.9f
    const val BUTTON_FILL_ALPHA = 0.2f
    const val CHIP_BORDER_ALPHA = 0.3f
}

/** Pins keep these fixed colours on both the light and dark map, so each is checked against both. ARGB ints for the bitmap generator. */
object MapColors {
    const val PIN_AVAILABLE: Int = 0xFF1B7A45.toInt()
    const val PIN_FILLING_FAST: Int = 0xFFB45309.toInt()
    const val PIN_FULL: Int = 0xFFC62828.toInt()
    const val PIN_CLOSED: Int = 0xFF334155.toInt()
    const val PIN_UNKNOWN: Int = 0xFF64748B.toInt()
    const val PIN_BORDER: Int = 0xFFFFFFFF.toInt()
    const val PIN_GLYPH: Int = PIN_BORDER
    /** Thin dark ring outside the white border, so a pin stands out on both land and forest tiles. */
    const val PIN_HALO: Int = 0xFF1F2937.toInt()
    const val PIN_SHADOW: Int = 0x50000000
    const val USER_LOCATION: Int = 0xFF2563EB.toInt()
    /** Base-map tints so the map sits in the app's own palette: warm paper and sage by day, night navy after dark. */
    val lightLand = Color(0xFFFAF7F0)
    val lightPark = Color(0xFFE2EBDC)
    val lightWater = Color(0xFFCADFE4)
    val darkLand = Color(0xFF0B131E)
    val darkPark = Color(0xFF13261F)
    val darkWater = Color(0xFF0E2233)
    /** What the pins sit on in each theme. */
    val tileBackgrounds = listOf(lightLand, lightPark, lightWater)
    val darkTileBackgrounds = listOf(darkLand, darkPark, darkWater)
}

/** Android 7 cannot draw dark navigation-bar icons, so a light theme there needs this scrim behind the white icons. */
object SystemBarColors {
    val lightIconsScrim = Color(0x801B1B1B)
}

/** Named opacity steps for tinted fills, dividers and de-emphasised text. */
object Alpha {
    const val FAINT = 0.15f
    const val SUBTLE = 0.2f
    const val LOW = 0.4f
    const val HALF = 0.5f
    const val MEDIUM = 0.7f
    const val HIGH = 0.8f
    const val HIGHER = 0.85f
    const val NEAR_OPAQUE = 0.9f
}
