package ca.bcparkspot.app.ui.prediction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
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

/**
 * What public sources say about a lot nobody has reported on. Always tagged as not a live report and
 * drawn in the informational tone, so it never looks like a hiker report. With [showSource], the row
 * ends with a link to its source.
 */
@Composable
fun PredictionRow(prediction: Prediction, modifier: Modifier = Modifier, showSource: Boolean = false) {
    val resources = LocalContext.current.resources
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val tone = BCParkSpotColors.semantic.info
    Surface(
        shape = RoundedCornerShape(Sizes.messageCorner),
        color = tone.container,
        modifier = modifier
            .fillMaxWidth()
            .testTag("lot_prediction")
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = tone.onContainer,
                    modifier = Modifier.size(Sizes.inlineIcon)
                )
                Text(
                    text = stringResource(R.string.prediction_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tone.onContainer
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = outlookIcon(prediction.outlook),
                    contentDescription = null,
                    tint = tone.onContainer,
                    modifier = Modifier.size(Sizes.inlineIcon)
                )
                Text(
                    text = outlookLabel(resources, prediction.outlook),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tone.onContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = patternDetail(resources, prediction, locale),
                style = MaterialTheme.typography.bodySmall,
                color = tone.onContainer
            )
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
