package ca.bcparkspot.app.ui.theme

import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.PassRequirementStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusTonesTest {
    private val s = LightSemantic

    @Test
    fun `each lot status has its own tone and closed is never green`() {
        assertEquals(s.positive, statusTone(AvailabilityStatus.AVAILABLE, s))
        assertEquals(s.caution, statusTone(AvailabilityStatus.FILLING_FAST, s))
        assertEquals(s.negative, statusTone(AvailabilityStatus.FULL, s))
        assertEquals(s.closed, statusTone(AvailabilityStatus.CLOSED, s))
        assertEquals(s.neutral, statusTone(AvailabilityStatus.UNKNOWN, s))
    }

    @Test
    fun `only confirmed lots take a status colour`() {
        assertEquals(s.negative, stateTone(LotState.Confirmed(AvailabilityStatus.FULL, 2, 0), s))
        assertEquals(s.closed, stateTone(LotState.Confirmed(AvailabilityStatus.CLOSED, 2, 0), s))
        assertEquals(s.info, stateTone(LotState.Mixed(AvailabilityStatus.AVAILABLE, 2, 0), s))
        assertEquals(s.neutral, stateTone(LotState.Unconfirmed(AvailabilityStatus.AVAILABLE, 1, 0), s))
        assertEquals(s.neutral, stateTone(LotState.NoRecentReports, s))
    }

    @Test
    fun `pass states map to caution, positive and neutral`() {
        assertEquals(s.caution, passTone(PassRequirementStatus.REQUIRED, s))
        assertEquals(s.positive, passTone(PassRequirementStatus.NOT_REQUIRED, s))
        assertEquals(s.neutral, passTone(PassRequirementStatus.UNKNOWN, s))
    }

    @Test
    fun `every status tone is distinct in both themes`() {
        listOf(LightSemantic, DarkSemantic).forEach { theme ->
            val tones = AvailabilityStatus.entries.map { statusTone(it, theme) }
            assertEquals(tones.size, tones.toSet().size)
            assertEquals(tones.size, tones.map { it.accent }.toSet().size)
        }
    }
}
