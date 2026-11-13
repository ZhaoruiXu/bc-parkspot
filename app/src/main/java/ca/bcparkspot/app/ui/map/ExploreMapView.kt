package ca.bcparkspot.app.ui.map

import ca.bcparkspot.app.ui.theme.BCParkSpotColors
import ca.bcparkspot.app.ui.theme.MapColors
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.model.PassRequirementStatus
import ca.bcparkspot.app.model.UserLocation
import ca.bcparkspot.app.ui.components.AvailabilityBadge
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import androidx.compose.ui.graphics.luminance
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.ui.prediction.PredictionRow

@Composable
fun ExploreMapView(
    filteredLots: List<ParkingLot>,
    selectedLot: ParkingLot?,
    onPinTapped: (ParkingLot) -> Unit,
    onViewTrailheadDetails: (ParkingLot) -> Unit,
    onNavigateToLot: (ParkingLot) -> Unit,
    onDismissCard: () -> Unit,
    onSwitchToListMode: () -> Unit,
    userLocation: UserLocation?,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize MapLibre
    LaunchedEffect(Unit) {
        try {
            MapLibre.getInstance(context)
        } catch (e: Throwable) {
            // Handled gracefully by fallback
        }
    }

    // Dynamic network connectivity listener: detects losing connection while map is open (e.g. airplane mode)
    var isOffline by remember { mutableStateOf(!ParkingMapUtils.isNetworkAvailable(context)) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline = false
            }

            override fun onLost(network: Network) {
                // Re-check active network status (prevents false offline when Wi-Fi drops while mobile data is active)
                isOffline = !ParkingMapUtils.isNetworkAvailable(context)
            }
        }

        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Fallback to initial check if callback registration fails
        }

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {}
        }
    }

    var maplibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    val darkMap = MaterialTheme.colorScheme.background.luminance() < MapTheme.DARK_LUMINANCE_THRESHOLD
    var styledDark by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(darkMap, maplibreMapInstance) {
        val map = maplibreMapInstance ?: return@LaunchedEffect
        if (styledDark != null && styledDark != darkMap) {
            styledDark = darkMap
            // A loaded style applies its own default view, so the user's view is kept across the switch.
            val keptCamera = map.cameraPosition
            map.setStyle(Style.Builder().fromUri(mapStyleUrl(darkMap))) { style ->
                map.cameraPosition = keptCamera
                tintMapStyle(style, darkMap)
            }
        }
    }
    var activeMarkerMap by remember { mutableStateOf<Map<Marker, ParkingLot>>(emptyMap()) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }

    // Lifecycle-aware MapView with TextureView rendering (avoids EGL/SurfaceView compositor crashes in emulators)
    val mapView = remember {
        try {
            val options = org.maplibre.android.maps.MapLibreMapOptions.createFromAttributes(context)
                .textureMode(true)
            MapView(context, options).apply {
                onCreate(null)
            }
        } catch (e: Throwable) {
            null
        }
    }

    if (mapView != null) {
        DisposableEffect(lifecycleOwner, mapView) {
            var isDestroyed = false
            fun safeDestroy() {
                if (!isDestroyed) {
                    isDestroyed = true
                    try {
                        mapView.onDestroy()
                    } catch (e: Throwable) {}
                }
            }

            val observer = LifecycleEventObserver { _, event ->
                try {
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        Lifecycle.Event.ON_DESTROY -> safeDestroy()
                        else -> {}
                    }
                } catch (e: Throwable) {
                    // Fail gracefully
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                try {
                    (mapView.parent as? android.view.ViewGroup)?.removeView(mapView)
                    mapView.onPause()
                    mapView.onStop()
                    safeDestroy()
                } catch (e: Throwable) {}
            }
        }
    }


    // Function to update all pins on the MapLibre instance
    fun updateMapPins(map: MapLibreMap) {
        try {
            map.clear()
            val newMarkerMap = mutableMapOf<Marker, ParkingLot>()

            // 1. Add parking lot pins
            filteredLots.forEach { lot ->
                val icon = ParkingPinBitmapGenerator.getPinIcon(context, lot.effectiveStatus)
                val markerOptions = MarkerOptions()
                    .position(LatLng(lot.latitude, lot.longitude))
                    .title(lot.lotName)
                    .snippet(lot.id)
                    .icon(icon)

                val marker = map.addMarker(markerOptions)
                newMarkerMap[marker] = lot
            }
            activeMarkerMap = newMarkerMap

            // 2. Add user location pin ONLY if permission is granted AND real device GPS fix exists (no fake default location!)
            if (hasLocationPermission && userLocation != null) {
                val userIcon = ParkingPinBitmapGenerator.getUserLocationIcon(context)
                val userMarkerOptions = MarkerOptions()
                    .position(LatLng(userLocation.latitude, userLocation.longitude))
                    .title("Your GPS Location")
                    .snippet("user_real_gps_location")
                    .icon(userIcon)
                userLocationMarker = map.addMarker(userMarkerOptions)
            } else {
                userLocationMarker = null
            }

            // 3. Adjust camera to encompass all filtered lots
            if (filteredLots.isNotEmpty()) {
                if (filteredLots.size == 1) {
                    val single = filteredLots.first()
                    map.easeCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(single.latitude, single.longitude),
                            ParkingMapConstants.SINGLE_LOT_ZOOM
                        ),
                        ParkingMapConstants.CAMERA_ANIMATION_DURATION_MS
                    )
                } else {
                    val boundsBuilder = LatLngBounds.Builder()
                    filteredLots.forEach {
                        boundsBuilder.include(LatLng(it.latitude, it.longitude))
                    }
                    if (hasLocationPermission && userLocation != null) {
                        boundsBuilder.include(LatLng(userLocation.latitude, userLocation.longitude))
                    }
                    try {
                        map.easeCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                boundsBuilder.build(),
                                ParkingMapConstants.MAP_BOUNDS_PADDING_SIDE_PX,
                                ParkingMapConstants.MAP_BOUNDS_PADDING_TOP_PX,
                                ParkingMapConstants.MAP_BOUNDS_PADDING_SIDE_PX,
                                ParkingMapConstants.MAP_BOUNDS_PADDING_BOTTOM_PX
                            ),
                            ParkingMapConstants.CAMERA_ANIMATION_DURATION_MS
                        )
                    } catch (e: Exception) {
                        // In case dimensions are not yet laid out
                    }
                }
            }
        } catch (e: Throwable) {
            // Protect against native engine state mismatch
        }
    }

    // Reactively update pins whenever filteredLots, user GPS location, or location permission changes
    LaunchedEffect(filteredLots, hasLocationPermission, userLocation, maplibreMapInstance) {
        maplibreMapInstance?.let { map ->
            updateMapPins(map)
        }
    }

    val isMapUnavailable = mapView == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_map_container")
    ) {
        if (isMapUnavailable) {
            // Map Unavailable Offline Fallback Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ParkingMapConstants.MAP_OFFLINE_CARD_PADDING)
                    .align(Alignment.Center)
                    .testTag("map_offline_fallback_card"),
                shape = RoundedCornerShape(ParkingMapConstants.MAP_OFFLINE_CARD_CORNER_RADIUS),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    ParkingMapConstants.MAP_OFFLINE_BORDER_STROKE_WIDTH,
                    MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = ParkingMapConstants.MAP_OFFLINE_ELEVATION)
            ) {
                Column(
                    modifier = Modifier.padding(ParkingMapConstants.MAP_OFFLINE_CARD_PADDING),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_OFFLINE_CONTENT_SPACING)
                ) {
                    Box(
                        modifier = Modifier
                            .size(ParkingMapConstants.MAP_OFFLINE_ICON_BG_SIZE)
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(ParkingMapConstants.MAP_OFFLINE_ICON_BG_RADIUS)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(ParkingMapConstants.MAP_OFFLINE_ICON_SIZE)
                        )
                    }

                    Text(
                        text = ParkingMapConstants.MAP_UNAVAILABLE_OFFLINE_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = ParkingMapConstants.MAP_UNAVAILABLE_OFFLINE_DESC,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = ParkingMapConstants.MAP_OFFLINE_DESC_FONT_SIZE
                    )

                    Spacer(modifier = Modifier.height(ParkingMapConstants.MAP_OFFLINE_SPACER_HEIGHT))

                    Button(
                        onClick = onSwitchToListMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_offline_switch_to_list_button"),
                        shape = RoundedCornerShape(ParkingMapConstants.MAP_OFFLINE_BTN_CORNER_RADIUS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(ParkingMapConstants.MAP_OFFLINE_BTN_ICON_SIZE)
                        )
                        Spacer(modifier = Modifier.width(ParkingMapConstants.MAP_OFFLINE_BTN_SPACING))
                        Text("Switch to List View")
                    }
                }
            }
        } else {
            // MapLibre AndroidView
            AndroidView(
                factory = {
                    (mapView.parent as? android.view.ViewGroup)?.removeView(mapView)
                    mapView.apply {
                        try {
                            getMapAsync { map ->
                                maplibreMapInstance = map
                                try {
                                    // Camera and pin taps do not depend on the remote style, so they work even if it fails to load.
                                    val initialCamera = CameraPosition.Builder()
                                        .target(LatLng(ParkingMapConstants.INITIAL_CAMERA_LATITUDE, ParkingMapConstants.INITIAL_CAMERA_LONGITUDE))
                                        .zoom(ParkingMapConstants.INITIAL_CAMERA_ZOOM)
                                        .build()
                                    map.cameraPosition = initialCamera
                                    map.setOnMarkerClickListener { marker ->
                                        activeMarkerMap[marker]?.let(onPinTapped)
                                        true
                                    }
                                    styledDark = darkMap
                                    map.setStyle(
                                        Style.Builder().fromUri(mapStyleUrl(darkMap))
                                    ) { style ->
                                        // A loaded style applies its own default view, so the start view is set again.
                                        map.cameraPosition = initialCamera
                                        tintMapStyle(style, darkMap)
                                        updateMapPins(map)
                                    }
                                } catch (e: Throwable) {
                                    // Fallback gracefully
                                }
                            }
                        } catch (e: Throwable) {
                            // Fallback gracefully
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("maplibre_view")
            )

            // Unified Top-Right Map Controls Stack (All elements equally spaced!)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = ParkingMapConstants.MAP_CONTROLS_PADDING_TOP,
                        end = ParkingMapConstants.MAP_CONTROLS_PADDING_END
                    ),
                verticalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_CONTROLS_SPACING),
                horizontalAlignment = Alignment.End
            ) {
                // Lot Count Pill placed at top of right-hand control column above + button
                Surface(
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_LOT_PILL_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = ParkingMapConstants.MAP_SURFACE_ALPHA),
                    border = BorderStroke(
                        ParkingMapConstants.MAP_LOT_PILL_BORDER_WIDTH,
                        MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
                    ),
                    shadowElevation = ParkingMapConstants.MAP_LOT_PILL_SHADOW_ELEVATION
                ) {
                    Text(
                        text = "${filteredLots.size} lots showing",
                        modifier = Modifier.padding(
                            horizontal = ParkingMapConstants.MAP_LOT_PILL_PADDING_HORIZONTAL,
                            vertical = ParkingMapConstants.MAP_LOT_PILL_PADDING_VERTICAL
                        ),
                        fontSize = ParkingMapConstants.MAP_LOT_PILL_FONT_SIZE,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 1. Zoom In (+) Button
                Surface(
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_CONTROL_BUTTON_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = ParkingMapConstants.MAP_SURFACE_ALPHA),
                    border = BorderStroke(
                        ParkingMapConstants.MAP_CONTROL_BUTTON_BORDER_WIDTH,
                        MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
                    ),
                    shadowElevation = ParkingMapConstants.MAP_CONTROL_BUTTON_SHADOW_ELEVATION,
                    modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_BUTTON_SIZE_DP)
                ) {
                    IconButton(
                        onClick = {
                            maplibreMapInstance?.easeCamera(
                                CameraUpdateFactory.zoomIn(),
                                ParkingMapConstants.STEP_ZOOM_ANIMATION_DURATION_MS
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 2. Zoom Out (−) Button
                Surface(
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_CONTROL_BUTTON_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = ParkingMapConstants.MAP_SURFACE_ALPHA),
                    border = BorderStroke(
                        ParkingMapConstants.MAP_CONTROL_BUTTON_BORDER_WIDTH,
                        MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
                    ),
                    shadowElevation = ParkingMapConstants.MAP_CONTROL_BUTTON_SHADOW_ELEVATION,
                    modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_BUTTON_SIZE_DP)
                ) {
                    IconButton(
                        onClick = {
                            maplibreMapInstance?.easeCamera(
                                CameraUpdateFactory.zoomOut(),
                                ParkingMapConstants.STEP_ZOOM_ANIMATION_DURATION_MS
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 3. Re-center / Fit all lots button
                Surface(
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_CONTROL_BUTTON_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = ParkingMapConstants.MAP_SURFACE_ALPHA),
                    border = BorderStroke(
                        ParkingMapConstants.MAP_CONTROL_BUTTON_BORDER_WIDTH,
                        MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
                    ),
                    shadowElevation = ParkingMapConstants.MAP_CONTROL_BUTTON_SHADOW_ELEVATION,
                    modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_BUTTON_SIZE_DP)
                ) {
                    IconButton(
                        onClick = {
                            maplibreMapInstance?.let { updateMapPins(it) }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("map_recenter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Fit All Lots",
                            modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 4. User Location button - always visible so user can request permission or center location
                Surface(
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_CONTROL_BUTTON_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = ParkingMapConstants.MAP_SURFACE_ALPHA),
                    border = BorderStroke(
                        ParkingMapConstants.MAP_CONTROL_BUTTON_BORDER_WIDTH,
                        MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
                    ),
                    shadowElevation = ParkingMapConstants.MAP_CONTROL_BUTTON_SHADOW_ELEVATION,
                    modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_BUTTON_SIZE_DP)
                ) {
                    IconButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                onRequestLocationPermission()
                            } else if (userLocation != null) {
                                maplibreMapInstance?.easeCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(userLocation.latitude, userLocation.longitude),
                                        ParkingMapConstants.SINGLE_LOT_ZOOM
                                    ),
                                    ParkingMapConstants.CAMERA_ANIMATION_DURATION_MS
                                )
                            } else {
                                onRequestLocationPermission()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("map_my_location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Show My Location",
                            modifier = Modifier.size(ParkingMapConstants.MAP_CONTROL_ICON_SIZE),
                            tint = if (hasLocationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Visible OSM attribution directly on the map surface (top-left, no bottom offset)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = ParkingMapConstants.MAP_ATTRIBUTION_PADDING_START,
                        top = ParkingMapConstants.MAP_ATTRIBUTION_PADDING_TOP
                    )
                    .widthIn(max = ParkingMapConstants.MAP_ATTRIBUTION_MAX_WIDTH),
                shape = RoundedCornerShape(ParkingMapConstants.MAP_ATTRIBUTION_CORNER_RADIUS),
                color = MaterialTheme.colorScheme.surface.copy(alpha = ParkingMapConstants.MAP_ATTRIBUTION_ALPHA),
                border = BorderStroke(ParkingMapConstants.MAP_ATTRIBUTION_BORDER_WIDTH, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = ParkingMapConstants.OSM_ATTRIBUTION_TEXT,
                    fontSize = ParkingMapConstants.MAP_ATTRIBUTION_FONT_SIZE,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = ParkingMapConstants.MAP_ATTRIBUTION_PADDING_HORIZONTAL,
                        vertical = ParkingMapConstants.MAP_ATTRIBUTION_PADDING_VERTICAL
                    )
                )
            }

            // Bottom Card when a pin is tapped
            AnimatedVisibility(
                visible = selectedLot != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        horizontal = ParkingMapConstants.MAP_BOTTOM_CARD_PADDING_HORIZONTAL,
                        vertical = ParkingMapConstants.MAP_BOTTOM_CARD_PADDING_VERTICAL
                    )
            ) {
                if (selectedLot != null) {
                    MapSelectedLotCard(
                        lot = selectedLot,
                        onViewDetails = { onViewTrailheadDetails(selectedLot) },
                        onNavigate = { onNavigateToLot(selectedLot) },
                        onDismiss = onDismissCard
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapSelectedLotCard(
    lot: ParkingLot,
    onViewDetails: () -> Unit,
    onNavigate: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_bottom_card"),
        shape = RoundedCornerShape(ParkingMapConstants.MAP_BOTTOM_CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ParkingMapConstants.MAP_BOTTOM_CARD_ELEVATION),
        border = BorderStroke(
            ParkingMapConstants.MAP_BOTTOM_CARD_BORDER_WIDTH,
            MaterialTheme.colorScheme.outline.copy(alpha = ParkingMapConstants.MAP_OUTLINE_SUBTLE_ALPHA)
        )
    ) {
        Column(
            modifier = Modifier.padding(ParkingMapConstants.MAP_BOTTOM_CARD_CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_BOTTOM_CARD_SPACING)
        ) {
            // Header Row: Park Name + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lot.parkName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${lot.lotName} • ${lot.region.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(ParkingMapConstants.MAP_BOTTOM_CARD_CLOSE_BTN_SIZE)
                        .testTag("map_card_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(ParkingMapConstants.MAP_BOTTOM_CARD_CLOSE_ICON_SIZE),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status and Pass badges in FlowRow with clean wrapping and no awkward word breaks
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_BOTTOM_CARD_BADGE_SPACING_HORIZONTAL),
                verticalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_BOTTOM_CARD_BADGE_SPACING_VERTICAL)
            ) {
                AvailabilityBadge(
                    status = lot.effectiveStatus,
                    totalSpots = lot.totalSpots,
                    compact = true,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                val passState = lot.getPassState()
                val passLabel = when (passState) {
                    PassRequirementStatus.REQUIRED -> "Pass Required"
                    PassRequirementStatus.UNKNOWN -> "Pass TBA"
                    PassRequirementStatus.NOT_REQUIRED -> {
                        if (lot.isDayUsePassRequired) "Off-Season (No Pass)" else "No Pass Needed"
                    }
                }
                val passChipTone = if (passState == PassRequirementStatus.REQUIRED) BCParkSpotColors.semantic.caution else BCParkSpotColors.semantic.neutral
                Surface(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_PASS_BADGE_CORNER_RADIUS),
                    color = passChipTone.container,
                    border = BorderStroke(
                        ParkingMapConstants.MAP_PASS_BADGE_BORDER_WIDTH,
                        if (passState == PassRequirementStatus.REQUIRED) passChipTone.accent else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = ParkingMapConstants.MAP_PASS_BADGE_PADDING_HORIZONTAL,
                            vertical = ParkingMapConstants.MAP_PASS_BADGE_PADDING_VERTICAL
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_PASS_BADGE_INNER_SPACING)
                    ) {
                        Icon(
                            imageVector = when (passState) {
                                PassRequirementStatus.REQUIRED -> Icons.Default.ConfirmationNumber
                                PassRequirementStatus.UNKNOWN -> Icons.Default.Info
                                PassRequirementStatus.NOT_REQUIRED -> Icons.Default.Check
                            },
                            contentDescription = null,
                            modifier = Modifier.size(ParkingMapConstants.MAP_PASS_BADGE_ICON_SIZE),
                            tint = passChipTone.onContainer
                        )
                        Text(
                            text = passLabel,
                            fontSize = ParkingMapConstants.MAP_PASS_BADGE_FONT_SIZE,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            color = passChipTone.onContainer
                        )
                    }
                }
            }
            val prediction = lot.prediction
            if (prediction != null && lot.crowd.state == LotState.NoRecentReports) {
                PredictionRow(prediction)
            }


            // Explanatory detail when lot has seasonal pass rules
            if (lot.isDayUsePassRequired && lot.getPassState() == PassRequirementStatus.NOT_REQUIRED) {
                Text(
                    text = "BC Parks day-use pass not active today (off-season free access).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = ParkingMapConstants.MAP_PASS_EXPLANATION_FONT_SIZE
                )
            }

            // Actions row: Details and Navigate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ParkingMapConstants.MAP_ACTION_BUTTON_SPACING)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_card_detail_button"),
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_ACTION_BUTTON_CORNER_RADIUS)
                ) {
                    Text("Trailhead Details", fontSize = ParkingMapConstants.MAP_ACTION_BUTTON_FONT_SIZE)
                }

                Button(
                    onClick = onNavigate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_card_navigate_button"),
                    shape = RoundedCornerShape(ParkingMapConstants.MAP_ACTION_BUTTON_CORNER_RADIUS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(ParkingMapConstants.MAP_NAVIGATE_BUTTON_ICON_SIZE)
                    )
                    Spacer(modifier = Modifier.width(ParkingMapConstants.MAP_NAVIGATE_BUTTON_SPACING))
                    Text("Directions", fontSize = ParkingMapConstants.MAP_ACTION_BUTTON_FONT_SIZE)
                }
            }
        }
    }
}
