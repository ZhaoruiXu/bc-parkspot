package ca.bcparkspot.app

import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.ui.map.ParkingMapConstants
import ca.bcparkspot.app.ui.map.mapStatusToPinVisual
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure function unit tests for status-to-pin mapping.
 * Verifies color assignments, neutral grey for UNKNOWN, hex formatting, and idempotency.
 */
class StatusPinMappingTest {

    @Test
    fun unknownStatus_mapsToNeutralGrey() {
        val visual = mapStatusToPinVisual(AvailabilityStatus.UNKNOWN)

        assertEquals("UNKNOWN status must have neutral grey color", ParkingMapConstants.COLOR_PIN_UNKNOWN, visual.colorArgb)
        assertEquals("UNKNOWN status hex code must match #94A3B8", "#94A3B8", visual.hexColor)
        assertTrue("UNKNOWN status must be marked as neutral grey", visual.isNeutralGrey)
        assertEquals(AvailabilityStatus.UNKNOWN, visual.status)
    }

    @Test
    fun availableStatus_mapsToGreen() {
        val visual = mapStatusToPinVisual(AvailabilityStatus.AVAILABLE)

        assertEquals("AVAILABLE status must map to green", ParkingMapConstants.COLOR_PIN_AVAILABLE, visual.colorArgb)
        assertEquals("#059669", visual.hexColor)
        assertFalse("AVAILABLE status must not be neutral grey", visual.isNeutralGrey)
        assertEquals("Available", visual.label)
    }

    @Test
    fun fillingFastStatus_mapsToAmber() {
        val visual = mapStatusToPinVisual(AvailabilityStatus.FILLING_FAST)

        assertEquals("FILLING_FAST status must map to amber", ParkingMapConstants.COLOR_PIN_FILLING_FAST, visual.colorArgb)
        assertEquals("#D97706", visual.hexColor)
        assertFalse("FILLING_FAST must not be neutral grey", visual.isNeutralGrey)
        assertEquals("Filling Fast", visual.label)
    }

    @Test
    fun fullStatus_mapsToRed() {
        val visual = mapStatusToPinVisual(AvailabilityStatus.FULL)

        assertEquals("FULL status must map to red", ParkingMapConstants.COLOR_PIN_FULL, visual.colorArgb)
        assertEquals("#DC2626", visual.hexColor)
        assertFalse("FULL must not be neutral grey", visual.isNeutralGrey)
        assertEquals("Full", visual.label)
    }

    @Test
    fun closedStatus_mapsToDarkSlate() {
        val visual = mapStatusToPinVisual(AvailabilityStatus.CLOSED)

        assertEquals("CLOSED status must map to dark slate", ParkingMapConstants.COLOR_PIN_CLOSED, visual.colorArgb)
        assertEquals("#334155", visual.hexColor)
        assertFalse("CLOSED must not be neutral grey", visual.isNeutralGrey)
        assertEquals("Gate Closed", visual.label)
    }

    @Test
    fun allAvailabilityStatuses_areCoveredAndPure() {
        AvailabilityStatus.values().forEach { status ->
            val visual1 = mapStatusToPinVisual(status)
            val visual2 = mapStatusToPinVisual(status)

            // Idempotency: pure function must return identical results
            assertEquals("Mapping must be idempotent for $status", visual1, visual2)
            assertNotNull(visual1.hexColor)
            assertTrue("Hex code must start with # for $status", visual1.hexColor.startsWith("#"))
            assertEquals(7, visual1.hexColor.length)
            assertTrue("Label must not be empty for $status", visual1.label.isNotEmpty())

            if (status == AvailabilityStatus.UNKNOWN) {
                assertTrue("Only UNKNOWN should be neutral grey", visual1.isNeutralGrey)
            } else {
                assertFalse("$status should not be neutral grey", visual1.isNeutralGrey)
            }
        }
    }
}
