package ca.bcparkspot.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.model.AvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDataTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BC ParkSpot", appName)
    }

    @Test
    fun `verify initial lots and distance calculation`() {
        val lots = BCParksDataSource.getInitialParkingLots()
        assertTrue(lots.isNotEmpty())

        val vancouver = BCParksDataSource.sampleUserLocations[0]
        val seymour = lots.first { it.id == "seymour_lower_p1" }

        val distance = BCParksDataSource.calculateDistanceKm(
            vancouver.latitude, vancouver.longitude,
            seymour.latitude, seymour.longitude
        )
        assertTrue("Distance should be positive and realistic", distance > 5.0 && distance < 60.0)

        val route = BCParksDataSource.generateRoute(vancouver, seymour, lots)
        assertNotNull(route)
        assertTrue(route.roadConditions.isNotBlank())
        assertTrue(route.estimatedMinutes > 0)
    }

    @Test
    fun `verify lot physical stalls`() {
        val lots = BCParksDataSource.getInitialParkingLots()
        val lotWithStalls = lots.first { it.totalSpots != null }
        assertTrue((lotWithStalls.totalSpots ?: 0) > 0)
        assertEquals(AvailabilityStatus.UNKNOWN, lotWithStalls.effectiveStatus)
    }

    @Test
    fun `launch MainActivity with Robolectric`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}

