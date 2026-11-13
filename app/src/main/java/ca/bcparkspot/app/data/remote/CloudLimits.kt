package ca.bcparkspot.app.data.remote

import ca.bcparkspot.app.model.AvailabilityStatus

/**
 * Field limits the server enforces (4.17). `firestore.rules` repeats each value in a function named after it,
 * and a unit test fails if the two ever differ.
 */
object CloudLimits {
    const val MAX_ID_LENGTH = 100
    const val MAX_NAME_LENGTH = 200
    const val MAX_REGION_LENGTH = 100
    const val MAX_NOTES_LENGTH = 500
    const val MAX_STALL_LENGTH = 100
    const val MAX_REPORTER_NAME_LENGTH = 100
    const val MIN_COMMENT_LENGTH = 3
    const val MAX_COMMENT_LENGTH = 500
    const val MAX_SPOTS_ESTIMATE = 5000

    /** Statuses a person can report; UNKNOWN is the app's own "no reports" state, never sent. */
    val REPORTABLE_STATUSES: List<AvailabilityStatus> = AvailabilityStatus.entries - AvailabilityStatus.UNKNOWN

    /** Report ids are lower-case UUIDs, the format `UUID.randomUUID()` produces. */
    const val REPORT_ID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
}

enum class CarPinProblem { STALL_TOO_LONG, NOTES_TOO_LONG }

/** 4.11: the car pin is checked on the device against the server's limits before it is saved. */
fun carPinProblem(stall: String, notes: String): CarPinProblem? = when {
    stall.length > CloudLimits.MAX_STALL_LENGTH -> CarPinProblem.STALL_TOO_LONG
    notes.length > CloudLimits.MAX_NOTES_LENGTH -> CarPinProblem.NOTES_TOO_LONG
    else -> null
}

/** The comment rule the server applies, on the trimmed text. */
fun isValidReportComment(comment: String): Boolean =
    comment.trim().length in CloudLimits.MIN_COMMENT_LENGTH..CloudLimits.MAX_COMMENT_LENGTH
