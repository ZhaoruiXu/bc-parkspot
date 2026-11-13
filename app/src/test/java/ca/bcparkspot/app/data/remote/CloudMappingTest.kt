package ca.bcparkspot.app.data.remote

import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.model.AvailabilityStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class CloudMappingTest {
    private val savedAt = 1_700_000_000_000L
    private val updatedAt = savedAt + 1_000L

    @Test
    fun `saved lot round-trips, tombstone included, under the reader's uid`() {
        val row = SavedLotEntity("me", "lot-1", "Park", "Lot", "Region", "notes", savedAt, updatedAt, deleted = true, version = 3, syncedVersion = 2)
        val back = savedLotFromCloud("me", row.toCloud())
        assertEquals(row.copy(version = 1, syncedVersion = 0), back)
    }

    @Test
    fun `saved lot without id or updatedAt is dropped`() {
        val cloud = SavedLotEntity("me", "lot-1", "Park", "Lot", "Region", "", savedAt, updatedAt).toCloud()
        assertNull(savedLotFromCloud("me", cloud - CloudFields.LOT_ID))
        assertNull(savedLotFromCloud("me", cloud - CloudFields.UPDATED_AT))
    }

    @Test
    fun `parked car round-trips with its GPS flag`() {
        val row = ParkedCarEntity("me", "Park", "Lot", 49.1, -123.2, "B4", "near gate", savedAt, isExactGpsLocation = true, updatedAt = updatedAt)
        assertEquals(row, parkedCarFromCloud("me", row.toCloud()))
    }

    @Test
    fun `parked car without coordinates is dropped`() {
        val cloud = ParkedCarEntity("me", "Park", "Lot", 49.1, -123.2, timestamp = savedAt, updatedAt = updatedAt).toCloud()
        assertNull(parkedCarFromCloud("me", cloud - CloudFields.LATITUDE))
    }

    @Test
    fun `report is written under the uploading uid with its observed time`() {
        val row = OwnReportEntity("id-1", "previous", "lot-1", "Park", "BC Hiker", "FULL", "packed", null, savedAt)
        val cloud = row.toCloud("me")
        assertEquals("me", cloud[CloudFields.USER_ID])
        assertEquals(Timestamp(Date(savedAt)), cloud[CloudFields.OBSERVED_AT])
        assertFalse(CloudFields.SPOTS_ESTIMATE in cloud)

        val parsed = reportFromCloud("id-1", cloud)!!
        assertEquals(AvailabilityStatus.FULL, parsed.statusReported)
        assertEquals(savedAt, parsed.timestampMs)
        assertEquals("me", parsed.userId)
    }

    @Test
    fun `report with an unknown status or no observed time is dropped`() {
        val cloud = OwnReportEntity("id-1", "me", "lot-1", "Park", "BC Hiker", "FULL", "packed", 3, savedAt).toCloud("me")
        assertNull(reportFromCloud("id-1", cloud + (CloudFields.STATUS to "MAYBE")))
        assertNull(reportFromCloud("id-1", cloud - CloudFields.OBSERVED_AT))
    }
}
