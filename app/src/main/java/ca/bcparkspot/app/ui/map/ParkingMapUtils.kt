package ca.bcparkspot.app.ui.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object ParkingMapUtils {
    /**
     * Checks if the device has an active internet connection for downloading map tiles.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}
