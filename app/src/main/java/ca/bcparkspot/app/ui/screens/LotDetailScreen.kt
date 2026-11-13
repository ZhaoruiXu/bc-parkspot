package ca.bcparkspot.app.ui.screens

import ca.bcparkspot.app.ui.theme.Spacing
import ca.bcparkspot.app.ui.prediction.PredictionRow
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.ui.theme.Alpha
import ca.bcparkspot.app.ui.theme.BCParkSpotColors
import ca.bcparkspot.app.ui.theme.stateTone
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.ui.text.stateDetail
import ca.bcparkspot.app.ui.text.stateHeadline
import ca.bcparkspot.app.ui.theme.passTone
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ca.bcparkspot.app.R
import ca.bcparkspot.app.ui.text.formatAge
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotDetailScreen(
    lot: ParkingLot,
    isSaved: Boolean,
    communityReports: List<CommunityReport>,
    waitingReportIds: Set<String> = emptySet(),
    onBack: () -> Unit,
    onNavigate: () -> Unit,
    onFindNearestOpen: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenReportDialog: () -> Unit,
    onOpenSaveCarSheet: () -> Unit,
    onSelectOverflowLot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val crowd = lot.crowd
    val resources = LocalContext.current.resources
    val semantic = BCParkSpotColors.semantic
    val lotReports = communityReports.filter { it.lotId == lot.id }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = lot.parkName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSave, modifier = Modifier.testTag("detail_save_button")) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                            contentDescription = if (isSaved) "Remove Bookmark" else "Bookmark",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("lot_detail_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Title & Authority Banner
            item {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = lot.region.displayName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = lot.authority.displayName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = lot.lotName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = lot.highwayAccess,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Crowd Status & Guidebook Panel
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.SUBTLE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRAILHEAD STATUS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (lot.verifiedAt != null) "Verified ${lot.verifiedAt}" else "Guidebook Baseline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Consensus badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = stateTone(crowd.state, semantic).container,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dotColor = stateTone(crowd.state, semantic).onContainer
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = dotColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Column {
                                    Text(
                                        text = stateHeadline(resources, crowd.state),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dotColor
                                    )
                                    Text(
                                        text = stateDetail(resources, crowd.state, ReportPolicies.DEFAULT),
                                        fontSize = 12.sp,
                                        color = dotColor
                                    )
                                    val capacityPhrase = if (lot.totalSpots != null) "Official capacity: ${lot.totalSpots} stalls • " else ""
                                    Text(
                                        text = "${capacityPhrase}Status comes from hiker reports, not sensors",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val prediction = lot.prediction
                        if (prediction != null && crowd.state == LotState.NoRecentReports) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            PredictionRow(prediction, showSource = true)
                            Text(
                                text = stringResource(R.string.prediction_explainer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.xs)
                            )
                        }
                        if (crowd.latestComment != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Latest Note: \"${crowd.latestComment}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // Primary Navigation Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_navigate_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Directions", fontWeight = FontWeight.Bold)
                    }

                    if (lot.effectiveStatus == AvailabilityStatus.FULL || lot.effectiveStatus == AvailabilityStatus.FILLING_FAST) {
                        FilledTonalButton(
                            onClick = onFindNearestOpen,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_find_nearest_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.AltRoute, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Find Open Spot", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Seasonal Day-Use Pass Policy Card
            item {
                val passState = lot.getPassState()
                val passRequired = passState == PassRequirementStatus.REQUIRED
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (passRequired) semantic.caution.container else MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (passRequired) semantic.caution.accent else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (passState) {
                                    PassRequirementStatus.REQUIRED -> Icons.Default.AssignmentLate
                                    PassRequirementStatus.UNKNOWN -> Icons.Default.Info
                                    PassRequirementStatus.NOT_REQUIRED -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = if (passRequired) semantic.caution.onContainer else passTone(passState, semantic).accent
                            )
                            Text(
                                text = when (passState) {
                                    PassRequirementStatus.REQUIRED -> "Day-Use Pass Active Today"
                                    PassRequirementStatus.UNKNOWN -> "Pass dates TBA - check bcparks.ca"
                                    PassRequirementStatus.NOT_REQUIRED -> {
                                        if (lot.isDayUsePassRequired) "Day-Use Pass Off-Season" else "No Day-Pass Required"
                                    }
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (passRequired) semantic.caution.onContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = lot.passInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (passRequired) semantic.caution.onContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Key Trailhead Amenities Grid
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.SUBTLE))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Trailhead Specifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        DetailSpecRow(icon = Icons.Default.LocalParking, label = "Fee Structure", value = lot.feeDescription)
                        DetailSpecRow(icon = Icons.Default.LockClock, label = "Gate Hours", value = lot.gateHours)
                        DetailSpecRow(icon = Icons.Default.EvStation, label = "EV Charging", value = if (lot.evChargingSpots > 0) "${lot.evChargingSpots} Level 2 Ports" else "None")
                        DetailSpecRow(icon = Icons.Default.Pets, label = "Dog Regulations", value = if (lot.isDogFriendly) "Allowed (On-leash)" else "Strictly Prohibited")
                        DetailSpecRow(icon = Icons.Default.Wc, label = "Washrooms", value = if (lot.hasWashrooms) "Available at Trailhead" else "No Facilities")
                        DetailSpecRow(icon = Icons.Default.SignalCellularAlt, label = "Cell Service", value = lot.cellSignal.label)
                    }
                }
            }

            // Official Park Source Link
            item {
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(lot.sourceUrl))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("official_website_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Visit Official ${lot.authority.displayName} Page")
                }
            }

            // Community Reports Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hiker Reports (${lotReports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = onOpenReportDialog,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("post_report_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Report Status",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (lotReports.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Alpha.LOW),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No reports submitted in the last 3 hours. Arriving now? Tap 'Report Status' to help fellow BC hikers!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(lotReports) { report ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.FAINT)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = report.reporterName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (report.id in waitingReportIds) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = semantic.caution.container
                                        ) {
                                            Text(
                                                text = stringResource(R.string.report_waiting_to_sync),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = semantic.caution.onContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatAge(LocalContext.current.resources, System.currentTimeMillis() - report.timestampMs),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status: ${report.statusReported.label}" + if (report.spotsEstimate != null) " (~${report.spotsEstimate} spots left)" else "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (report.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = report.comment,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSpecRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
