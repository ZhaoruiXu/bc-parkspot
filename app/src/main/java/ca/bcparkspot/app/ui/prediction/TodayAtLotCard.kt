package ca.bcparkspot.app.ui.prediction

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.ConfigurationCompat
import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.GateToday
import ca.bcparkspot.app.domain.Prediction
import ca.bcparkspot.app.domain.TodayAtLot
import ca.bcparkspot.app.model.PassRequirementStatus
import ca.bcparkspot.app.ui.theme.BCParkSpotColors
import ca.bcparkspot.app.ui.theme.SemanticColors
import ca.bcparkspot.app.ui.theme.Sizes
import ca.bcparkspot.app.ui.theme.Spacing
import ca.bcparkspot.app.ui.theme.Tone
import java.util.Locale

/** Which status colour a fact tile takes, so the meaning shows before the words are read. */
enum class FactTone { GOOD, HEADS_UP, SHUT, UNKNOWN }

fun SemanticColors.toneFor(fact: FactTone): Tone = when (fact) {
    FactTone.GOOD -> positive
    FactTone.HEADS_UP -> caution
    FactTone.SHUT -> closed
    FactTone.UNKNOWN -> neutral
}

/** A tile's main value and an optional short note beneath it. */
data class FactText(val value: String, val note: String? = null)

/** Green only while the gate is open right now; a gate shut at this moment looks shut whatever its hours. */
fun gateTone(gate: GateToday, openNow: Boolean): FactTone = when {
    gate == GateToday.NoHoursPublished -> FactTone.UNKNOWN
    !openNow -> FactTone.SHUT
    gate is GateToday.ClosesAsEarlyAs -> FactTone.HEADS_UP
    else -> FactTone.GOOD
}

fun gateText(res: Resources, gate: GateToday, locale: Locale): FactText = when (gate) {
    GateToday.NoHoursPublished -> FactText(res.getString(R.string.gate_value_not_published))
    GateToday.OpenAllDay -> FactText(res.getString(R.string.gate_value_open_all_day))
    GateToday.ClosedAllDay -> FactText(res.getString(R.string.gate_value_closed_all_day))
    is GateToday.Hours ->
        FactText(res.getString(R.string.gate_value_hours, formatCompactClockTime(gate.opens, locale), formatCompactClockTime(gate.closes, locale)))
    is GateToday.ClosesAsEarlyAs -> FactText(
        res.getString(R.string.gate_value_from, formatCompactClockTime(gate.opens, locale)),
        res.getString(R.string.gate_note_may_close, formatCompactClockTime(gate.earliestClose, locale))
    )
}

fun passTone(pass: PassRequirementStatus): FactTone = when (pass) {
    PassRequirementStatus.NOT_REQUIRED -> FactTone.GOOD
    PassRequirementStatus.REQUIRED -> FactTone.HEADS_UP
    PassRequirementStatus.UNKNOWN -> FactTone.UNKNOWN
}

fun passText(res: Resources, pass: PassRequirementStatus): FactText = FactText(
    res.getString(
        when (pass) {
            PassRequirementStatus.REQUIRED -> R.string.pass_value_required
            PassRequirementStatus.NOT_REQUIRED -> R.string.pass_value_not_required
            PassRequirementStatus.UNKNOWN -> R.string.pass_value_unknown
        }
    )
)

/**
 * What a first visitor can rely on at a lot nobody has reported on, under one public-info tag: the sourced heads-up
 * for this moment if there is one, today's published gate hours and the pass rule right now as colour-coded tiles,
 * and a button to make the first report.
 */
@Composable
fun TodayAtLotCard(today: TodayAtLot, prediction: Prediction?, onReport: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val semantic = BCParkSpotColors.semantic
    Column(
        modifier = modifier.fillMaxWidth().testTag("today_at_lot"),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.today_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            PublicInfoTag()
        }
        prediction?.let { PredictionRow(it, showTag = false, showDetail = !detailRepeatsGateTile(it.outlook)) }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FactTile(
                icon = Icons.Outlined.LockClock,
                label = stringResource(R.string.fact_gate),
                text = gateText(resources, today.gate, locale),
                tone = semantic.toneFor(gateTone(today.gate, today.gateOpenNow)),
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("fact_gate")
            )
            FactTile(
                icon = Icons.Outlined.ConfirmationNumber,
                label = stringResource(R.string.fact_pass),
                text = passText(resources, today.pass),
                tone = semantic.toneFor(passTone(today.pass)),
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("fact_pass")
            )
        }
        OutlinedButton(
            onClick = onReport,
            shape = RoundedCornerShape(Sizes.messageCorner),
            border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().testTag("first_report_button")
        ) {
            Icon(imageVector = Icons.Outlined.Campaign, contentDescription = null, modifier = Modifier.size(Sizes.inlineIcon))
            Text(
                text = stringResource(R.string.first_report_cta),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
    }
}

/** A small neutral tag that marks a block as public information, never a live report. */
@Composable
fun PublicInfoTag(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.inlineIcon)
        )
        Text(
            text = stringResource(R.string.public_info_tag),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FactTile(icon: ImageVector, label: String, text: FactText, tone: Tone, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(Sizes.messageCorner),
        color = tone.container,
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            IconBadge(icon, tone)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = tone.onContainer)
                Text(
                    text = text.value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone.onContainer
                )
                text.note?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = tone.onContainer)
                }
            }
        }
    }
}

/** An icon on a filled circle in the tone's accent colour. */
@Composable
fun IconBadge(icon: ImageVector, tone: Tone, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(Sizes.iconBadge).background(tone.accent, CircleShape)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tone.container, modifier = Modifier.size(Sizes.inlineIcon))
    }
}
