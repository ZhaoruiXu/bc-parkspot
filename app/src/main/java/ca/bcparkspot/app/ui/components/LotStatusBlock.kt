package ca.bcparkspot.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.ui.prediction.PredictionRow
import ca.bcparkspot.app.ui.text.stateSummary
import ca.bcparkspot.app.ui.theme.BCParkSpotColors
import ca.bcparkspot.app.ui.theme.Sizes
import ca.bcparkspot.app.ui.theme.Spacing
import ca.bcparkspot.app.ui.theme.stateTone

/** A lot's crowd-report status, followed by its labelled prediction while nobody has reported on it. */
@Composable
fun LotStatusBlock(lot: ParkingLot, modifier: Modifier = Modifier, showPrediction: Boolean = true) {
    val resources = LocalContext.current.resources
    val tone = stateTone(lot.crowd.state, BCParkSpotColors.semantic)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Surface(
            shape = RoundedCornerShape(Sizes.badgeCorner),
            color = tone.container,
            modifier = Modifier.fillMaxWidth().testTag("lot_crowd_status")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = tone.onContainer,
                    modifier = Modifier.size(Sizes.statusDot)
                )
                Text(
                    text = stateSummary(resources, lot.crowd.state, ReportPolicies.DEFAULT),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tone.onContainer
                )
            }
        }
        val prediction = lot.prediction
        if (showPrediction && prediction != null && lot.crowd.state == LotState.NoRecentReports) {
            PredictionRow(prediction)
        }
    }
}
