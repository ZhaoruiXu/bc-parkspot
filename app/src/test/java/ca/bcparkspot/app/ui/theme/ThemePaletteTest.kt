package ca.bcparkspot.app.ui.theme

import android.app.UiModeManager
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePaletteTest {

    // ColorScheme exposes each role as a mangled getter such as getPrimary-0d7_KjU returning the packed colour.
    private fun roles(scheme: ColorScheme): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter { it.name.startsWith("get") && it.name.contains('-') && it.parameterCount == 0 && it.returnType == Long::class.javaPrimitiveType }
            .associate { m ->
                val name = m.name.removePrefix("get").substringBefore('-').replaceFirstChar { it.lowercase() }
                name to Color((m.invoke(scheme) as Long).toULong())
            }

    @Test
    fun `light palette names every colour role of the scheme`() {
        assertEquals(roles(LightColorScheme).keys.sorted(), LightRoles.keys.sorted())
    }

    @Test
    fun `dark palette names every colour role of the scheme`() {
        assertEquals(roles(DarkColorScheme).keys.sorted(), DarkRoles.keys.sorted())
    }

    @Test
    fun `every light role comes from the palette`() {
        roles(LightColorScheme).forEach { (name, value) -> assertEquals(name, LightRoles.getValue(name), value) }
    }

    @Test
    fun `every dark role comes from the palette`() {
        roles(DarkColorScheme).forEach { (name, value) -> assertEquals(name, DarkRoles.getValue(name), value) }
    }

    @Test
    fun `system preference follows the device and explicit choices override it`() {
        assertEquals(true, resolveDarkTheme(ThemePreference.SYSTEM, systemIsDark = true))
        assertEquals(false, resolveDarkTheme(ThemePreference.SYSTEM, systemIsDark = false))
        assertEquals(false, resolveDarkTheme(ThemePreference.LIGHT, systemIsDark = true))
        assertEquals(true, resolveDarkTheme(ThemePreference.DARK, systemIsDark = false))
    }

    @Test
    fun `stored theme preference round-trips and falls back to system`() {
        ThemePreference.entries.forEach { assertEquals(it, parseThemePreference(it.name)) }
        assertEquals(ThemePreference.SYSTEM, parseThemePreference(null))
        assertEquals(ThemePreference.SYSTEM, parseThemePreference("SEPIA"))
    }

    @Test
    fun `each preference maps to its system night mode`() {
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, uiModeNightFor(ThemePreference.SYSTEM))
        assertEquals(UiModeManager.MODE_NIGHT_NO, uiModeNightFor(ThemePreference.LIGHT))
        assertEquals(UiModeManager.MODE_NIGHT_YES, uiModeNightFor(ThemePreference.DARK))
    }
}
