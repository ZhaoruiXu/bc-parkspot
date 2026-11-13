package ca.bcparkspot.app.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeRuntimeTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun roles(scheme: ColorScheme) = listOf(
        scheme.primary, scheme.secondary, scheme.tertiary, scheme.background, scheme.surface,
        scheme.surfaceContainer, scheme.error, scheme.outline
    )

    private fun schemeFor(preference: ThemePreference): ColorScheme {
        lateinit var captured: ColorScheme
        compose.setContent { BCParkSpotTheme(preference) { captured = MaterialTheme.colorScheme } }
        compose.waitForIdle()
        return captured
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `light window background matches the light scheme`() {
        assertEquals(LightColorScheme.background.toArgb(), ContextCompat.getColor(context, R.color.window_background))
        assertEquals(LightColorScheme.background, windowBackgroundFor(dark = false))
    }

    @Test
    @Config(qualifiers = "night")
    fun `night window background matches the dark scheme`() {
        assertEquals(DarkColorScheme.background.toArgb(), ContextCompat.getColor(context, R.color.window_background))
        assertEquals(DarkColorScheme.background, windowBackgroundFor(dark = true))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `explicit dark is the brand dark scheme even when the system is light`() {
        assertEquals(roles(DarkColorScheme), roles(schemeFor(ThemePreference.DARK)))
    }

    @Test
    @Config(qualifiers = "night")
    fun `explicit light is the brand light scheme even when the system is dark`() {
        assertEquals(roles(LightColorScheme), roles(schemeFor(ThemePreference.LIGHT)))
    }

    @Test
    @Config(qualifiers = "night")
    fun `system follows a dark device with the brand scheme, never a dynamic one`() {
        assertEquals(roles(DarkColorScheme), roles(schemeFor(ThemePreference.SYSTEM)))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `system follows a light device with the brand scheme`() {
        assertEquals(roles(LightColorScheme), roles(schemeFor(ThemePreference.SYSTEM)))
    }
}
