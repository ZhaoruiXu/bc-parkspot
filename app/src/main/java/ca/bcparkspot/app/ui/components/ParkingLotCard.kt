package ca.bcparkspot.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.*

@Composable
fun ParkingLotCard(
    lot: ParkingLot,
    distanceKm: Double,
    drivingMinutes: Int,
    isSaved: Boolean,
    onSelect: () -> Unit,
    onNavigate: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consensus = lot.crowdConsensus

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("lot_card_${lot.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (lot.effectiveStatus == AvailabilityStatus.FULL) Color(0xFFFCA5A5)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Park Name + Authority + Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = lot.parkName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = lot.authority.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("save_button_${lot.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                        contentDescription = if (isSaved) "Remove Saved" else "Save Lot",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Lot Title
            Text(
                text = lot.lotName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Distance, ETA & Region row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$distanceKm km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(text = "•", color = MaterialTheme.colorScheme.outline)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "~$drivingMinutes min drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(text = "•", color = MaterialTheme.colorScheme.outline)

                Text(
                    text = if (lot.totalSpots != null) "${lot.totalSpots} stalls" else "Trailhead (Quota per person)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Honest Crowd Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    consensus?.freshness == CrowdFreshness.ACTIVE && consensus.status == AvailabilityStatus.FULL -> Color(0xFFFEE2E2)
                    consensus?.freshness == CrowdFreshness.ACTIVE && consensus.status == AvailabilityStatus.FILLING_FAST -> Color(0xFFFEF3C7)
                    consensus?.freshness == CrowdFreshness.ACTIVE -> Color(0xFFDCFCE7)
                    consensus?.freshness == CrowdFreshness.STALE -> Color(0xFFFFFBEB)
                    else -> Color(0xFFF1F5F9)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotColor = when {
                        consensus?.freshness == CrowdFreshness.ACTIVE && consensus.status == AvailabilityStatus.FULL -> Color(0xFFDC2626)
                        consensus?.freshness == CrowdFreshness.ACTIVE && consensus.status == AvailabilityStatus.FILLING_FAST -> Color(0xFFD97706)
                        consensus?.freshness == CrowdFreshness.ACTIVE -> Color(0xFF16A34A)
                        consensus?.freshness == CrowdFreshness.STALE -> Color(0xFFB45309)
                        else -> Color(0xFF64748B)
                    }
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = dotColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = consensus?.displaySummary ?: "Guidebook Baseline • No recent crowd reports (3h)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = dotColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Policy & Feature Chips Row
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Seasonal Pass Tag
                val passState = lot.getPassState()
                val passLabel = when (passState) {
                    PassRequirementStatus.REQUIRED -> "Day-Pass Active"
                    PassRequirementStatus.UNKNOWN -> "Pass TBA"
                    PassRequirementStatus.NOT_REQUIRED -> {
                        if (lot.isDayUsePassRequired) "Off-Season (No Pass)" else "No Pass Req."
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (passState) {
                        PassRequirementStatus.REQUIRED -> Color(0xFFFEF3C7)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = passLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        color = when (passState) {
                            PassRequirementStatus.REQUIRED -> Color(0xFF92400E)
                            else -> Color(0xFF475569)
                        }
                    )
                }

                // Fee tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = lot.feeDescription.split("(")[0].trim(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        color = Color(0xFF334155)
                    )
                }

                if (!lot.isDogFriendly) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = "No Dogs",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // Overflow recommendation if full
            if (lot.effectiveStatus == AvailabilityStatus.FULL && lot.overflowLotName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AltRoute,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Overflow: ${lot.overflowLotName} (${lot.overflowDistanceKm} km)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Detail and Navigate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("details_button_${lot.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Trailhead Info", fontSize = 13.sp)
                }

                Button(
                    onClick = onNavigate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("navigate_button_${lot.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions", fontSize = 13.sp)
                }
            }
        }
    }
}
