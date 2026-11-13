package ca.bcparkspot.app.model

import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.domain.BC_TIME_ZONE
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class PassStateZoneTest {
    private val original = TimeZone.getDefault()

    @After
    fun restore() = TimeZone.setDefault(original)

    @Test
    fun `pass state defaults to BC time whatever zone the phone is in`() {
        val lot = BCParksDataSource.getInitialParkingLots().first { it.isDayUsePassRequired && it.passRulePeriods.isNotEmpty() }
        val expected = lot.getPassState(LocalDate.now(BC_TIME_ZONE), LocalTime.now(BC_TIME_ZONE))
        listOf("Pacific/Kiritimati", "Pacific/Pago_Pago").forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            assertEquals(zone, expected, lot.getPassState())
        }
    }
}
