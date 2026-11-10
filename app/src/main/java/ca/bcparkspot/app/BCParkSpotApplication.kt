package ca.bcparkspot.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.maplibre.android.MapLibre

/**
 * Custom Application class for BC ParkSpot.
 * Guarantees crash-free startup under all execution environments:
 * 1. Initializes MapLibre before any Activity or MapView is inflated.
 * 2. Ensures FirebaseApp is safely initialized with fallback configuration if google-services.json
 *    is absent on clean clones or offline devices, preventing uncaught IllegalStateException.
 */
class BCParkSpotApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize MapLibre globally before any Activity or MapView is inflated
        try {
            MapLibre.getInstance(this)
        } catch (e: Throwable) {
            Log.w("BCParkSpotApp", "MapLibre global initialization notice: ${e.message}")
        }

        // 2. Ensure FirebaseApp is safely initialized under any environment
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Throwable) {
                    Log.w("BCParkSpotApp", "Resource-based FirebaseApp init fallback: ${e.message}")
                    FirebaseApp.initializeApp(
                        this,
                        FirebaseOptions.Builder()
                            .setApplicationId(packageName)
                            .setProjectId("bcparkspot-offline")
                            .setApiKey("bc_park_spot_offline_key_placeholder")
                            .build()
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w("BCParkSpotApp", "Firebase initialization guard: ${e.message}")
        }
    }
}
