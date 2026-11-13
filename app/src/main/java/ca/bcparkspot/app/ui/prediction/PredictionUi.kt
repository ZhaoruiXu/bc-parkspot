package ca.bcparkspot.app.ui.prediction

import android.content.res.Resources
import androidx.annotation.StringRes
import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.LotCandidate
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.domain.Outlook
import ca.bcparkspot.app.domain.Prediction
import ca.bcparkspot.app.ui.text.stateHeadline
import java.net.URI
import java.time.LocalTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

@StringRes
fun outlookLabelRes(outlook: Outlook): Int = when (outlook) {
    Outlook.PARK_CLOSED -> R.string.outlook_park_closed
    Outlook.GATE_CLOSED -> R.string.outlook_gate_closed
    Outlook.GATE_MAY_BE_CLOSED -> R.string.outlook_gate_may_be_closed
    Outlook.GATE_CLOSING_SOON -> R.string.outlook_gate_closing_soon
    Outlook.ARRIVE_BY -> R.string.outlook_arrive_by
    Outlook.PASS_DAY -> R.string.outlook_pass_day
    Outlook.SUMMER_BUSY -> R.string.outlook_summer_busy
}

fun outlookLabel(res: Resources, outlook: Outlook): String = res.getString(outlookLabelRes(outlook))

fun formatClockTime(time: LocalTime, locale: Locale): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

/** Like [formatClockTime], but a whole hour on a 12-hour clock drops its minutes ("8 AM"), so ranges fit a tile. */
fun formatCompactClockTime(time: LocalTime, locale: Locale): String {
    val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale)
    val twelveHour = pattern.contains(AM_PM_FIELD)
    if (!twelveHour || time.minute != 0) return formatClockTime(time, locale)
    return time.format(DateTimeFormatter.ofPattern(pattern.replace(MINUTES_FIELD, ""), locale))
}

private const val AM_PM_FIELD = 'a'
private const val MINUTES_FIELD = ":mm"

/** The detail line's string and the clock time it is formatted with, if any. */
data class DetailText(@StringRes val res: Int, val time: LocalTime? = null)

fun patternDetailText(prediction: Prediction): DetailText = when (prediction.outlook) {
    Outlook.PARK_CLOSED -> DetailText(R.string.pattern_park_closed)
    Outlook.GATE_CLOSED -> prediction.gateOpens?.let { DetailText(R.string.pattern_gate_opens, it) }
        ?: DetailText(R.string.pattern_gate_closed_all_day)
    Outlook.GATE_MAY_BE_CLOSED -> DetailText(R.string.pattern_gate_may_be_closed, prediction.gateClosesAsEarlyAs)
    Outlook.GATE_CLOSING_SOON -> DetailText(R.string.pattern_gate_closing_soon, prediction.gateCloses)
    Outlook.ARRIVE_BY -> DetailText(R.string.pattern_arrive_by, prediction.arriveBy)
    Outlook.PASS_DAY -> DetailText(R.string.pattern_pass_day)
    Outlook.SUMMER_BUSY -> DetailText(R.string.pattern_summer_busy)
}

fun patternDetail(res: Resources, prediction: Prediction, locale: Locale): String {
    val text = patternDetailText(prediction)
    return text.time?.let { res.getString(text.res, formatClockTime(it, locale)) } ?: res.getString(text.res)
}

/** A source link shown by its site name, e.g. "vancouvertrails.com". */
fun sourceHost(url: String): String =
    runCatching { URI(url).host }.getOrNull()?.removePrefix(WWW_PREFIX) ?: url

private const val WWW_PREFIX = "www."

/**
 * A fallback pick's status. A lot nobody reported on also shows what sources say about it, e.g.
 * "No recent reports; Day-use pass day"; a lot with reports shows only what hikers said.
 */
fun fallbackStatus(res: Resources, candidate: LotCandidate): String {
    val pattern = candidate.outlook?.takeIf { candidate.state == LotState.NoRecentReports }
    return listOfNotNull(stateHeadline(res, candidate.state), pattern?.let { outlookLabel(res, it) })
        .joinToString(FALLBACK_STATUS_SEPARATOR)
}

const val FALLBACK_STATUS_SEPARATOR = "; "
