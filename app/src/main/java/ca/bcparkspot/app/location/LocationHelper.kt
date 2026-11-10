package ca.bcparkspot.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

object LocationHelper {

    @SuppressLint("MissingPermission")
    fun fetchFreshLocation(
        context: Context,
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onSuccess(location)
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            onSuccess(lastLoc)
                        } else {
                            onError("No GPS fix available. Using lot pin as fallback.")
                        }
                    }.addOnFailureListener {
                        onError("Unable to retrieve GPS fix: ${it.localizedMessage}")
                    }
                }
            }.addOnFailureListener { exception ->
                onError("Failed to get fresh GPS location: ${exception.localizedMessage}")
            }
        } catch (e: SecurityException) {
            onError("Location permission not granted.")
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Location request failed.")
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentCoordinates(
        context: Context,
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchFreshLocation(
            context,
            onSuccess = { loc -> onSuccess(loc.latitude, loc.longitude) },
            onError = onError
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(
        fusedClient: FusedLocationProviderClient,
        onSuccess: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    onSuccess(loc.latitude, loc.longitude)
                } else {
                    onError("Location fix unavailable. Try selecting a BC departure hub.")
                }
                fusedClient.removeLocationUpdates(this)
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            onError("Location updates failed: ${e.localizedMessage}")
        }
    }
}
