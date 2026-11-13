package ca.bcparkspot.app.ui.screens

import ca.bcparkspot.app.ui.theme.Alpha
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import androidx.compose.runtime.remember
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.model.UserLocation
import ca.bcparkspot.app.ui.components.AvailabilityBadge

@Composable
fun SavedLotsScreen(
    parkedCar: ParkedCarEntity?,
    savedLots: List<SavedLotEntity>,
    allLots: List<ParkingLot>,
    onSelectLot: (ParkingLot) -> Unit,
    onNavigateToLot: (ParkingLot) -> Unit,
    onDeleteSavedLot: (ParkingLot) -> Unit,
    onClearParkedCar: () -> Unit,
    userLocation: UserLocation? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val compassData = remember(parkedCar, userLocation) {
        if (parkedCar != null && userLocation != null) {
            val dist = BCParksDataSource.calculateDistanceKm(
                userLocation.latitude, userLocation.longitude,
                parkedCar.latitude, parkedCar.longitude
            )
            val dLon = Math.toRadians(parkedCar.longitude - userLocation.longitude)
            val lat1 = Math.toRadians(userLocation.latitude)
            val lat2 = Math.toRadians(parkedCar.latitude)
            val y = Math.sin(dLon) * Math.cos(lat2)
            val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
            val bearing = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360
            val cardinals = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
            val dir = cardinals[((bearing + 22.5) / 45.0).toInt() % 8]
            Triple(bearing.toInt(), dir, dist)
        } else {
            Triple(null, null, null)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("saved_lots_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Where I Parked
        item {
            Text(
                text = "Where I Parked (Offline Locator)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Retain your vehicle's trailhead location even without cell reception.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            if (parkedCar != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("parked_car_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Vehicle Pinned",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            TextButton(
                                onClick = onClearParkedCar,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Clear Pin")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = parkedCar.lotName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = parkedCar.parkName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Alpha.HIGH)
                        )

                        if (parkedCar.stallNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = Alpha.HIGH)
                            ) {
                                Text(
                                    text = "Stall: ${parkedCar.stallNumber}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (parkedCar.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: ${parkedCar.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Alpha.NEAR_OPAQUE)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val locationLabel = if (parkedCar.isExactGpsLocation) {
                            "Device GPS Fix: ${String.format("%.4f", parkedCar.latitude)}, ${String.format("%.4f", parkedCar.longitude)}"
                        } else {
                            "Lot pin, not your exact car location (${String.format("%.4f", parkedCar.latitude)}, ${String.format("%.4f", parkedCar.longitude)})"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (parkedCar.isExactGpsLocation) Icons.Default.MyLocation else Icons.Default.PinDrop,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = locationLabel,
                                fontSize = 11.sp,
                                fontWeight = if (parkedCar.isExactGpsLocation) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Alpha.HIGHER)
                            )
                        }

                        // Compass Bearing & Distance Indicator
                        val (bearing, dir, dist) = compassData
                        if (bearing != null && dir != null && dist != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val originLabel = if (userLocation?.name?.contains("GPS", ignoreCase = true) == true) {
                                "Phone GPS Fix"
                            } else {
                                "Sample Hub: ${userLocation?.name?.split("(")?.get(0)?.trim() ?: "Departure Hub"}"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = Alpha.NEAR_OPAQUE)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Explore,
                                            contentDescription = "Compass",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "True-North Bearing: $bearing° $dir • $dist km",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Origin: $originLabel (Note: Mathematical True-North direction, not magnetic device heading)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:${parkedCar.latitude},${parkedCar.longitude}?q=${parkedCar.latitude},${parkedCar.longitude}(My Parked Car)")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("return_to_car_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NearMe, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guide Me Back to Vehicle")
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Alpha.HALF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.SUBTLE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalParking,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "No vehicle currently pinned",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "When you arrive at a trailhead lot, tap 'Parked Here' to bookmark your stall offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Saved Lots
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved BC Recreation Lots (${savedLots.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedLots.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No saved recreation lots yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap the bookmark icon on any parking lot to save it for quick offline trip planning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(savedLots, key = { it.lotId }) { saved ->
                val fullLot = allLots.find { it.id == saved.lotId }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fullLot?.let { onSelectLot(it) } },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Alpha.SUBTLE))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = saved.lotName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = saved.parkName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            fullLot?.let {
                                AvailabilityBadge(
                                    status = it.effectiveStatus,
                                    totalSpots = it.totalSpots,
                                    compact = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fullLot?.let { lot ->
                                Button(
                                    onClick = { onNavigateToLot(lot) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Navigate", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onDeleteSavedLot(lot) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Remove", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
