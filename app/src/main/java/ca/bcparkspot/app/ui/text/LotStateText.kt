package ca.bcparkspot.app.ui.text

import android.content.res.Resources
import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.domain.ReportPolicy
import java.util.concurrent.TimeUnit

fun formatAge(resources: Resources, ageMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ageMs).toInt()
    val hours = TimeUnit.MILLISECONDS.toHours(ageMs).toInt()
    return when {
        minutes < 1 -> resources.getString(R.string.age_just_now)
        hours < 1 -> resources.getQuantityString(R.plurals.age_minutes, minutes, minutes)
        else -> resources.getQuantityString(R.plurals.age_hours, hours, hours)
    }
}

fun formatWindow(resources: Resources, windowMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(windowMs).toInt()
    return resources.getQuantityString(R.plurals.hours_count, hours, hours)
}

fun formatPeople(resources: Resources, count: Int): String =
    resources.getQuantityString(R.plurals.people_count, count, count)

fun stateHeadline(resources: Resources, state: LotState): String = when (state) {
    is LotState.Confirmed -> resources.getString(R.string.state_confirmed, state.status.label)
    is LotState.Mixed -> resources.getString(R.string.state_mixed, state.newestStatus.label)
    is LotState.Unconfirmed -> resources.getQuantityString(
        R.plurals.state_unconfirmed, state.reporterCount, state.reporterCount, state.newestStatus.label
    )
    LotState.NoRecentReports -> resources.getString(R.string.state_no_recent)
}

fun stateDetail(resources: Resources, state: LotState, policy: ReportPolicy): String {
    val age = state.newestAgeMs
        ?: return resources.getString(R.string.state_no_recent_detail, formatWindow(resources, policy.validityWindowMs))
    return resources.getQuantityString(
        R.plurals.reporters_summary, state.reporterCount, state.reporterCount, formatAge(resources, age)
    )
}

fun stateSummary(resources: Resources, state: LotState, policy: ReportPolicy): String =
    if (state == LotState.NoRecentReports) stateDetail(resources, state, policy)
    else "${stateHeadline(resources, state)} · ${stateDetail(resources, state, policy)}"

fun noConfirmedMessage(resources: Resources, policy: ReportPolicy): String =
    resources.getString(
        R.string.nearest_none_confirmed,
        formatPeople(resources, policy.minQuorumUsers),
        formatWindow(resources, policy.validityWindowMs)
    )
