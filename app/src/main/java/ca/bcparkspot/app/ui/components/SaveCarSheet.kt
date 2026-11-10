package ca.bcparkspot.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.ui.map.ParkingMapConstants

@Composable
fun SaveCarSheet(
    lot: ParkingLot,
    onDismiss: () -> Unit,
    onSave: (stall: String, notes: String, deviceLocation: Location?) -> Unit
) {
    val context = LocalContext.current
    var stallNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var detectedGpsLocation by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locationError = null
            ca.bcparkspot.app.location.LocationHelper.fetchFreshLocation(
                context,
                onSuccess = { loc -> detectedGpsLocation = loc },
                onError = { err -> locationError = err }
            )
        } else {
            locationError = "Location permission not granted. Falling back to lot pin."
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            ca.bcparkspot.app.location.LocationHelper.fetchFreshLocation(
                context,
                onSuccess = { loc -> detectedGpsLocation = loc },
                onError = { err -> locationError = err }
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                    text = "Save Parked Vehicle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = lot.parkName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = lot.lotName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val loc = detectedGpsLocation
                        if (loc != null) {
                            val accuracyM = loc.accuracy
                            val isAccurate = accuracyM <= 20f
                            val ageSeconds = ((System.currentTimeMillis() - loc.time) / 1000).coerceAtLeast(0)
                            val gpsLabel = if (isAccurate) {
                                "Exact GPS Fix (±${accuracyM.toInt()}m, ${ageSeconds}s ago)"
                            } else {
                                "Approximate GPS Fix (±${accuracyM.toInt()}m, ${ageSeconds}s ago)"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isAccurate) Color(0xFF047857) else Color(0xFFB45309)
                                )
                                Text(
                                    text = "$gpsLabel: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}",
                                    fontSize = ParkingMapConstants.SAVE_CAR_CAPTION_FONT_SIZE,
                                    color = if (isAccurate) Color(0xFF047857) else Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "GPS Unavailable: Fallback to Lot pin, not your exact car location",
                                    fontSize = ParkingMapConstants.SAVE_CAR_CAPTION_FONT_SIZE,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(ParkingMapConstants.SAVE_CAR_ENABLE_GPS_BTN_HEIGHT).testTag("save_car_enable_gps_button")
                            ) {
                                Text("Enable Location Permission", fontSize = ParkingMapConstants.SAVE_CAR_CAPTION_FONT_SIZE, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Text(
                    text = "Saved offline on your device so you can easily locate your car after your hike even without cell reception.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = stallNumber,
                    onValueChange = { stallNumber = it },
                    label = { Text("Row / Bay / Stall #") },
                    placeholder = { Text("e.g. Row C, Stall 22 or Lower Gravel Loop") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("car_stall_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Landmark & Notes") },
                    placeholder = { Text("e.g. Parked by the bear-proof garbage bins & trail map") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("car_notes_input"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(stallNumber, notes, detectedGpsLocation) },
                modifier = Modifier.testTag("save_car_confirm_button")
            ) {
                Text("Save My Spot")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("save_car_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
