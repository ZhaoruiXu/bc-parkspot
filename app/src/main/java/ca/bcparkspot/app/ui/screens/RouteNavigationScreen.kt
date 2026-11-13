package ca.bcparkspot.app.ui.screens

import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.ui.prediction.TodayAtLotCard
import ca.bcparkspot.app.domain.isConfirmedOpen
import androidx.compose.ui.res.stringResource
import ca.bcparkspot.app.R
import ca.bcparkspot.app.ui.components.LotStatusBlock
import ca.bcparkspot.app.ui.theme.Spacing
import ca.bcparkspot.app.ui.theme.Alpha
import ca.bcparkspot.app.ui.theme.BCParkSpotColors
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.CellSignal
import ca.bcparkspot.app.model.NavigationRoute
import ca.bcparkspot.app.model.ParkingLot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteNavigationScreen(
    route: NavigationRoute,
    /** The destination as it stands now, so its report status and prediction stay current on this screen. */
    destinationNow: ParkingLot,
    onBack: () -> Unit,
    onSelectAlternativeLot: (ParkingLot) -> Unit,
    onOpenSaveCarSheet: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val destLot = route.destinationLot

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Trailhead Directions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From: ${route.originName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nav_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .testTag("route_navigation_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Destination Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Alpha.HALF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = destLot.lotName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${destLot.parkName} • ${destLot.authority.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                        if (!destinationNow.crowd.state.isConfirmedOpen()) {
                            Text(
                                text = stringResource(R.string.route_not_confirmed),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = BCParkSpotColors.semantic.caution.accent,
                                modifier = Modifier.testTag("route_not_confirmed")
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                        val today = destinationNow.today
                        val unreported = destinationNow.crowd.state == LotState.NoRecentReports
                        LotStatusBlock(destinationNow, showPrediction = today == null || !unreported)
                        if (today != null && unreported) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            TodayAtLotCard(today, destinationNow.prediction, onReport = onReport)
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Distance & Travel Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox(label = "DISTANCE", value = "${route.totalDistanceKm} km")
                            StatBox(label = "EST. DRIVE", value = "~${route.estimatedMinutes} min")
                            StatBox(label = "TOTAL STALLS", value = destLot.totalSpots?.let { "$it" } ?: "Not published")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Verified GPS Coordinates
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "GPS: ${destLot.latitude}, ${destLot.longitude} (Verified Trailhead Pin)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Primary Google Maps Turn-by-Turn GPS Button
            item {
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${destLot.latitude},${destLot.longitude}&mode=d")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val fallbackUri = Uri.parse("geo:${destLot.latitude},${destLot.longitude}?q=${destLot.latitude},${destLot.longitude}(${Uri.encode(destLot.lotName)})")
                            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("launch_external_maps_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start GPS Navigation in Google Maps",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Access Road & Trailhead Conditions
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.SUBTLE)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Road & Highway Access",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = destLot.highwayAccess,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Alpha.MEDIUM)
                        ) {
                            Text(
                                text = route.roadConditions,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cell Service & Gate Warning
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (destLot.cellSignal == CellSignal.NO_SERVICE || destLot.cellSignal == CellSignal.WEAK_CELL) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BCParkSpotColors.semantic.caution.container,
                            border = BorderStroke(1.dp, BCParkSpotColors.semantic.caution.accent)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                                    contentDescription = "No Signal",
                                    tint = BCParkSpotColors.semantic.caution.onContainer
                                )
                                Column {
                                    Text(
                                        text = "Trailhead Cell Reception: ${destLot.cellSignal.label}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BCParkSpotColors.semantic.caution.onContainer
                                    )
                                    Text(
                                        text = "Cell coverage ends before reaching this lot. Download offline maps and pass tickets now.",
                                        fontSize = 11.sp,
                                        color = BCParkSpotColors.semantic.caution.onContainer
                                    )
                                }
                            }
                        }
                    }

                    // Gate Hours Notice
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Gate Hours: ${destLot.gateHours}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Pin: Save Car Location
            item {
                OutlinedButton(
                    onClick = onOpenSaveCarSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("route_save_car_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PinDrop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pin My Car Location Here (Offline)")
                }
            }

            // Overflow & Alternative Lots
            if (route.alternativeLots.isNotEmpty()) {
                item {
                    Text(
                        text = "Backup & Overflow Parking If Full",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(route.alternativeLots) { altLot ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.SUBTLE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAlternativeLot(altLot) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = altLot.lotName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = altLot.totalSpots?.let { "${altLot.parkName} • $it stalls" } ?: altLot.parkName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
