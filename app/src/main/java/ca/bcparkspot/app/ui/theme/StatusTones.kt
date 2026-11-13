package ca.bcparkspot.app.ui.theme

import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.PassRequirementStatus

fun statusTone(status: AvailabilityStatus, semantic: SemanticColors): Tone = when (status) {
    AvailabilityStatus.AVAILABLE -> semantic.positive
    AvailabilityStatus.FILLING_FAST -> semantic.caution
    AvailabilityStatus.FULL -> semantic.negative
    AvailabilityStatus.CLOSED -> semantic.closed
    AvailabilityStatus.UNKNOWN -> semantic.neutral
}

/** Only a confirmed lot takes its status colour; unconfirmed, mixed and unreported lots never look settled. */
fun stateTone(state: LotState, semantic: SemanticColors): Tone = when (state) {
    is LotState.Confirmed -> statusTone(state.status, semantic)
    is LotState.Mixed -> semantic.info
    is LotState.Unconfirmed, LotState.NoRecentReports -> semantic.neutral
}

fun passTone(pass: PassRequirementStatus, semantic: SemanticColors): Tone = when (pass) {
    PassRequirementStatus.REQUIRED -> semantic.caution
    PassRequirementStatus.NOT_REQUIRED -> semantic.positive
    PassRequirementStatus.UNKNOWN -> semantic.neutral
}
