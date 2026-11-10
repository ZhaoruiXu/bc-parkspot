package ca.bcparkspot.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLaunchTest {

    // Robolectric skips FirebaseInitProvider, so call what it calls on a device.
    @Test
    fun `app launches on a fresh clone with no Firebase configuration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(FirebaseApp.initializeApp(context))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertNotNull(it) }
        }
    }
}
