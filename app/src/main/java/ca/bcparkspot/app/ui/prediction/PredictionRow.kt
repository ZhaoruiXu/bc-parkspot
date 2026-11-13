package ca.bcparkspot.app.ui.prediction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.ConfigurationCompat
import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.Outlook
import ca.bcparkspot.app.domain.Prediction
import ca.bcparkspot.app.ui.theme.BCParkSpotColors
import ca.bcparkspot.app.ui.theme.Sizes
import ca.bcparkspot.app.ui.theme.Spacing
import java.util.Locale

fun outlookIcon(outlook: Outlook): ImageVector = when (outlook) {
    Outlook.PARK_CLOSED -> Icons.Outlined.DoNotDisturbOn
    Outlook.GATE_CLOSED -> Icons.Outlined.Lock
    Outlook.GATE_MAY_BE_CLOSED -> Icons.Outlined.LockClock
    Outlook.GATE_CLOSING_SOON -> Icons.Outlined.Timer
    Outlook.ARRIVE_BY -> Icons.Outlined.Schedule
    Outlook.PASS_DAY -> Icons.Outlined.ConfirmationNumber
    Outlook.SUMMER_BUSY -> Icons.Outlined.Groups
}

fun outlookTone(outlook: Outlook): FactTone = when (outlook) {
    Outlook.PARK_CLOSED, Outlook.GATE_CLOSED -> FactTone.SHUT
    Outlook.GATE_MAY_BE_CLOSED, Outlook.GATE_CLOSING_SOON, Outlook.ARRIVE_BY, Outlook.PASS_DAY, Outlook.SUMMER_BUSY ->
        FactTone.HEADS_UP
}

/** Gate outlooks whose times the gate tile beside them already shows, so their detail line would only repeat it. */
fun detailRepeatsGateTile(outlook: Outlook): Boolean =
    outlook == Outlook.GATE_CLOSED || outlook == Outlook.GATE_MAY_BE_CLOSED || outlook == Outlook.GATE_CLOSING_SOON

/**
 * What public sources say about a lot nobody has reported on: a coloured badge, a short label and one detail line,
 * tagged as public info so it never looks like a hiker report. With [showSource], the row ends with a link to its
 * source.
 */
@Composable
fun PredictionRow(
    prediction: Prediction,
    modifier: Modifier = Modifier,
    showSource: Boolean = false,
    /** False when the row sits inside a block that already carries the public-info tag. */
    showTag: Boolean = true,
    showDetail: Boolean = true
) {
    val resources = LocalContext.current.resources
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val tone = BCParkSpotColors.semantic.toneFor(outlookTone(prediction.outlook))
    Surface(
        shape = RoundedCornerShape(Sizes.messageCorner),
        color = tone.container,
        modifier = modifier
            .fillMaxWidth()
            .testTag("lot_prediction")
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                IconBadge(outlookIcon(prediction.outlook), tone)
                Text(
                    text = outlookLabel(resources, prediction.outlook),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone.onContainer,
                    modifier = Modifier.weight(1f)
                )
                if (showTag) PublicInfoTag()
            }
            if (showDetail) {
                Text(
                    text = patternDetail(resources, prediction, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = tone.onContainer
                )
            }
            val sourceUrl = prediction.sourceUrl
            if (showSource && sourceUrl != null) {
                val uriHandler = LocalUriHandler.current
                TextButton(
                    onClick = { runCatching { uriHandler.openUri(sourceUrl) } },
                    modifier = Modifier.testTag("lot_prediction_source")
                ) {
                    Text(
                        text = stringResource(R.string.prediction_source, sourceHost(sourceUrl)),
                        style = MaterialTheme.typography.labelMedium,
                        color = tone.onContainer
                    )
                }
            }
        }
    }
}
