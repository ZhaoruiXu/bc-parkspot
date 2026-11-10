package ca.bcparkspot.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.AvailabilityStatus

@Composable
fun AvailabilityBadge(
    status: AvailabilityStatus,
    totalSpots: Int?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    labelOverride: String? = null
) {
    val (bgColor, contentColor, icon) = when (status) {
        AvailabilityStatus.AVAILABLE -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF047857),
            Icons.Default.CheckCircle
        )
        AvailabilityStatus.FILLING_FAST -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFB45309),
            Icons.Default.Warning
        )
        AvailabilityStatus.FULL -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Dangerous
        )
        AvailabilityStatus.CLOSED -> Triple(
            Color(0xFFE2E8F0),
            Color(0xFF475569),
            Icons.Default.Lock
        )
        AvailabilityStatus.UNKNOWN -> Triple(
            Color(0xFFF1F5F9),
            Color(0xFF475569),
            Icons.Default.HelpOutline
        )
    }

    val animatedBg by animateColorAsState(targetValue = bgColor, label = "badge_bg")

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = animatedBg,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 14.dp else 18.dp)
            )

            val stallsText = if (totalSpots != null && totalSpots > 0) " ($totalSpots Stalls)" else ""
            Text(
                text = labelOverride ?: when (status) {
                    AvailabilityStatus.AVAILABLE -> "Available$stallsText"
                    AvailabilityStatus.FILLING_FAST -> "Filling Fast$stallsText"
                    AvailabilityStatus.FULL -> if (compact) "Lot Full" else "Lot Full (0 open)"
                    AvailabilityStatus.CLOSED -> "Gate Closed"
                    AvailabilityStatus.UNKNOWN -> if (compact) "Baseline$stallsText" else "Unknown / Baseline$stallsText"
                },
                color = contentColor,
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
