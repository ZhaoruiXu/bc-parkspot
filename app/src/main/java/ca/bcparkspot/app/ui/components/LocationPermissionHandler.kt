package ca.bcparkspot.app.ui.components

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.location.LocationHelper
import ca.bcparkspot.app.model.UserLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onLocationDetected: (UserLocation) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLocating by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val hasPermission = locationPermissionsState.allPermissionsGranted ||
        locationPermissionsState.permissions.any { it.status.isGranted }

    fun fetchLocation() {
        isLocating = true
        LocationHelper.fetchCurrentCoordinates(
            context = context,
            onSuccess = { lat, lon ->
                isLocating = false
                val detected = UserLocation(
                    name = "Current GPS (${String.format("%.3f", lat)}, ${String.format("%.3f", lon)})",
                    latitude = lat,
                    longitude = lon
                )
                onLocationDetected(detected)
            },
            onError = { err ->
                isLocating = false
                onError(err)
            }
        )
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && isLocating) {
            fetchLocation()
        }
    }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Detect My Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = if (isLocating) "Acquiring GPS fix…" else "Use My Current GPS Location",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        onClick = {
            if (hasPermission) {
                fetchLocation()
            } else if (locationPermissionsState.shouldShowRationale) {
                showRationaleDialog = true
            } else {
                isLocating = true
                locationPermissionsState.launchMultiplePermissionRequest()
            }
        },
        modifier = modifier.testTag("use_gps_location_item")
    )

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Location Permission Needed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "BC ParkSpot needs location access to calculate exact driving distances, travel times, and guide you to the nearest open parking spot from where you are right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        isLocating = true
                        locationPermissionsState.launchMultiplePermissionRequest()
                    },
                    modifier = Modifier.testTag("grant_location_permission_button")
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
