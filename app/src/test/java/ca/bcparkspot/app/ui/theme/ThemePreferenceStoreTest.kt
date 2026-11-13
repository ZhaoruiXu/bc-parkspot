package ca.bcparkspot.app.ui.theme

import android.app.UiModeManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ThemePreferenceStoreTest.SDK_WITH_APP_NIGHT_MODE])
class ThemePreferenceStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uiModeManager = context.getSystemService(UiModeManager::class.java)
    private fun appliedNightMode() = shadowOf(uiModeManager).applicationNightMode

    @Test
    fun `choosing Dark then System hands night mode back to the system`() {
        val store = ThemePreferenceStore(context)
        store.save(ThemePreference.DARK)
        assertEquals(UiModeManager.MODE_NIGHT_YES, appliedNightMode())
        store.save(ThemePreference.SYSTEM)
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, appliedNightMode())
        assertEquals(ThemePreference.SYSTEM, ThemePreferenceStore(context).load())
    }

    @Test
    fun `choosing Light forces day mode and survives a restart`() {
        ThemePreferenceStore(context).save(ThemePreference.LIGHT)
        assertEquals(UiModeManager.MODE_NIGHT_NO, appliedNightMode())
        assertEquals(ThemePreference.LIGHT, ThemePreferenceStore(context).load())
    }

    companion object {
        const val SDK_WITH_APP_NIGHT_MODE = 34
    }
}
