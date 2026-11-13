package ca.bcparkspot.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    private val failures = mutableListOf<String>()

    private fun check(label: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrastRatio(fg, bg)
        if (ratio < min) failures += "$label: %.2f < $min".format(ratio)
    }

    private val surfaceRoles = listOf(
        "surface", "background", "surfaceBright", "surfaceDim", "surfaceContainerLowest", "surfaceContainerLow",
        "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest"
    )

    private fun checkScheme(name: String, roles: Map<String, Color>, semantic: SemanticColors) {
        fun r(k: String) = roles.getValue(k)
        listOf("primary", "secondary", "tertiary", "error").forEach { base ->
            val on = "on" + base.replaceFirstChar { it.uppercase() }
            check("$name $on/$base", r(on), r(base), ContrastTargets.TEXT)
            check("$name ${on}Container/${base}Container", r("${on}Container"), r("${base}Container"), ContrastTargets.TEXT)
        }
        check("$name onBackground/background", r("onBackground"), r("background"), ContrastTargets.TEXT)
        check("$name onSurfaceVariant/surfaceVariant", r("onSurfaceVariant"), r("surfaceVariant"), ContrastTargets.TEXT)
        check("$name inverseOnSurface/inverseSurface", r("inverseOnSurface"), r("inverseSurface"), ContrastTargets.TEXT)
        surfaceRoles.forEach { bg ->
            listOf("onSurface", "onSurfaceVariant", "primary", "error").forEach { fg -> check("$name $fg/$bg", r(fg), r(bg), ContrastTargets.TEXT) }
            check("$name outline/$bg", r("outline"), r(bg), ContrastTargets.NON_TEXT)
            semantic.all.forEach { (tone, t) -> check("$name $tone.accent/$bg", t.accent, r(bg), ContrastTargets.TEXT) }
        }
        semantic.all.forEach { (tone, t) ->
            check("$name $tone.onContainer/container", t.onContainer, t.container, ContrastTargets.TEXT)
            check("$name $tone.accent/container", t.accent, t.container, ContrastTargets.NON_TEXT)
        }
    }

    @Test
    fun `light theme meets WCAG AA`() {
        checkScheme("light", LightRoles, LightSemantic)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `dark theme meets WCAG AA`() {
        checkScheme("dark", DarkRoles, DarkSemantic)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `map pins stand out from their outline, glyph and every map tile`() {
        listOf(MapColors.PIN_AVAILABLE, MapColors.PIN_FILLING_FAST, MapColors.PIN_FULL, MapColors.PIN_CLOSED, MapColors.PIN_UNKNOWN, MapColors.USER_LOCATION)
            .forEach { pin ->
                check("pin %08X/border".format(pin), Color(pin), Color(MapColors.PIN_BORDER), ContrastTargets.NON_TEXT)
                check("pin %08X/glyph".format(pin), Color(MapColors.PIN_GLYPH), Color(pin), ContrastTargets.NON_TEXT)
            }
        MapColors.tileBackgrounds.forEach { tile ->
            check("pin halo/tile %s".format(tile), Color(MapColors.PIN_HALO), tile, ContrastTargets.NON_TEXT)
        }
        MapColors.darkTileBackgrounds.forEach { tile ->
            check("pin border/dark tile %s".format(tile), Color(MapColors.PIN_BORDER), tile, ContrastTargets.NON_TEXT)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `contrast of black on white is the WCAG maximum`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.0)
    }
}
