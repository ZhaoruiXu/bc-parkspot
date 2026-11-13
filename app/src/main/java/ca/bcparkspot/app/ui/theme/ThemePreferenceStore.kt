package ca.bcparkspot.app.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build

private const val PREFS_NAME = "appearance"
private const val KEY_THEME = "theme_preference"

fun parseThemePreference(stored: String?): ThemePreference =
    ThemePreference.entries.firstOrNull { it.name == stored } ?: ThemePreference.SYSTEM

fun uiModeNightFor(preference: ThemePreference): Int = when (preference) {
    ThemePreference.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
    ThemePreference.LIGHT -> UiModeManager.MODE_NIGHT_NO
    ThemePreference.DARK -> UiModeManager.MODE_NIGHT_YES
}

class ThemePreferenceStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ThemePreference = parseThemePreference(prefs.getString(KEY_THEME, null))

    fun save(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
        applyToSystem(preference)
    }

    // On Android 12+ this makes the system splash and resources follow the in-app choice.
    fun applyToSystem(preference: ThemePreference) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(uiModeNightFor(preference))
        }
    }
}
