package ca.bcparkspot.app.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.R
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.location.LocationHelper
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.model.UserLocation
import ca.bcparkspot.app.ui.components.FilterChipsRow
import ca.bcparkspot.app.ui.components.LocationPermissionHandler
import ca.bcparkspot.app.ui.components.ParkingLotCard
import ca.bcparkspot.app.ui.map.ExploreMapView
import ca.bcparkspot.app.ui.map.ParkingMapConstants
import ca.bcparkspot.app.ui.viewmodel.ParkingUiState
import ca.bcparkspot.app.ui.viewmodel.ParkingViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

enum class ExploreViewMode {
    LIST,
    MAP
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LotsListScreen(
    viewModel: ParkingViewModel,
    uiState: ParkingUiState,
    filteredLots: List<ParkingLot>,
    savedLotIds: Set<String>,
    onSelectLot: (ParkingLot) -> Unit,
    onNavigateToLot: (ParkingLot) -> Unit,
    onFindNearestOpen: () -> Unit,
    onOpenAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUserLocation by viewModel.currentUserLocation.collectAsState()
    val deviceGpsLocation by viewModel.deviceGpsLocation.collectAsState()
    val communityReports by viewModel.communityReports.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val focusManager = LocalFocusManager.current
    var showLocationMenu by remember { mutableStateOf(false) }

    // Reactive location permission state
    val locationPermissions = remember {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = locationPermissions
    )
    val hasLocationPermission = locationPermissionsState.allPermissionsGranted ||
        locationPermissionsState.permissions.any { it.status.isGranted }

    // Fetch real device GPS location when permission is granted; plot ONLY real device GPS fix
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationHelper.fetchCurrentCoordinates(
                context = context,
                onSuccess = { lat, lon ->
                    viewModel.updateDeviceGpsLocation(
                        UserLocation(
                            name = "Current GPS (${String.format("%.3f", lat)}, ${String.format("%.3f", lon)})",
                            latitude = lat,
                            longitude = lon
                        )
                    )
                },
                onError = {
                    viewModel.updateDeviceGpsLocation(null)
                }
            )
        } else {
            viewModel.updateDeviceGpsLocation(null)
        }
    }

    val viewMode = uiState.exploreViewMode
    val selectedMapLot = uiState.selectedMapLot

    if (viewMode == ExploreViewMode.MAP) {
        // MAP MODE: Collapsed hero header so map fills the screen; search & filter controls preserved
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("lots_list_screen")
        ) {
            // Collapsed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "BC ParkSpot",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "PARKING MAP",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onOpenAuth,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .size(34.dp)
                            .testTag("account_button")
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.AccountCircle else Icons.Default.PersonOutline,
                            contentDescription = "User Account",
                            tint = if (currentUser != null) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshLiveAvailability() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .size(34.dp)
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Live Status",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar in Map mode
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search parks, lakes, or trails...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .testTag("search_text_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            // Region & Category Chips
            FilterChipsRow(
                selectedRegion = uiState.selectedRegion,
                onSelectRegion = { viewModel.selectRegion(it) },
                selectedType = uiState.selectedRecreationType,
                onSelectType = { viewModel.selectRecreationType(it) },
                filterAvailableOnly = uiState.filterAvailableOnly,
                onToggleAvailableOnly = { viewModel.toggleFilterAvailableOnly() },
                filterNoPassNeeded = uiState.filterNoPassNeeded,
                onToggleNoPassNeeded = { viewModel.toggleFilterNoPassNeeded() },
                filterFreeParking = uiState.filterFreeParkingOnly,
                onToggleFreeParking = { viewModel.toggleFilterFreeParking() },
                filterEvCharging = uiState.filterEvCharging,
                onToggleEvCharging = { viewModel.toggleFilterEvCharging() },
                filterDogFriendly = uiState.filterDogFriendly,
                onToggleDogFriendly = { viewModel.toggleFilterDogFriendly() }
            )

            // Lot-Count Row with List/Map Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${filteredLots.size} BC Recreational Parking Lots",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (communityReports.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "Live Sync",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                ListMapToggle(
                    viewMode = viewMode,
                    onModeChanged = { viewModel.setExploreViewMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Map fills the remaining space
            ExploreMapView(
                filteredLots = filteredLots,
                selectedLot = selectedMapLot,
                onPinTapped = { lot ->
                    // Pin tap only selects lot to display bottom card without opening details
                    viewModel.selectMapLot(lot)
                },
                onViewTrailheadDetails = { lot ->
                    // Trailhead details button explicitly navigates to details
                    onSelectLot(lot)
                },
                onNavigateToLot = { lot -> onNavigateToLot(lot) },
                onDismissCard = { viewModel.selectMapLot(null) },
                onSwitchToListMode = { viewModel.setExploreViewMode(ExploreViewMode.LIST) },
                userLocation = if (hasLocationPermission) deviceGpsLocation else null,
                hasLocationPermission = hasLocationPermission,
                onRequestLocationPermission = {
                    locationPermissionsState.launchMultiplePermissionRequest()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    } else {
        // LIST MODE: Full header with scenic photo, nearest open banner, search, chips, and lot cards list
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("lots_list_screen"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Hero Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ParkingMapConstants.HERO_BANNER_HEIGHT_DP)
                ) {
                    // Background scenic image
                    Image(
                        painter = painterResource(id = R.drawable.img_bc_parks_hero),
                        contentDescription = "BC Parks scenery",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Dark Gradient overlay for high text legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top App Header & Refresh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = "BC ParkSpot",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Text(
                                    text = "PARKING LOCATOR",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = onOpenAuth,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .size(36.dp)
                                        .testTag("account_button")
                                ) {
                                    Icon(
                                        imageVector = if (currentUser != null) Icons.Default.AccountCircle else Icons.Default.PersonOutline,
                                        contentDescription = "User Account",
                                        tint = if (currentUser != null) Color(0xFF34D399) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.refreshLiveAvailability() },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .size(36.dp)
                                        .testTag("refresh_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Refresh Live Status",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Hero Title & Departure Hub Selector
                        Column {
                            Text(
                                text = "Crowd-reported trailhead parking across British Columbia",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Departure Hub Picker
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Black.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { showLocationMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Departing from: ${currentUserLocation.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showLocationMenu,
                                    onDismissRequest = { showLocationMenu = false }
                                ) {
                                    Text(
                                        text = "Select Starting Point:",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LocationPermissionHandler(
                                        onLocationDetected = { loc ->
                                            viewModel.setUserLocation(loc)
                                            showLocationMenu = false
                                        },
                                        onError = {
                                            showLocationMenu = false
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    BCParksDataSource.sampleUserLocations.forEach { loc ->
                                        DropdownMenuItem(
                                            text = { Text(loc.name) },
                                            onClick = {
                                                viewModel.setUserLocation(loc)
                                                showLocationMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Smart CUJ Banner: "Navigate to Nearest Open Spot"
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable { onFindNearestOpen() }
                        .testTag("hero_nearest_open_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Find Nearest Open Spot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Nearest lot with confirmed crowd reports",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Go",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search parks, lakes, or trails...", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_text_field"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Region & Category Chips
            item {
                FilterChipsRow(
                    selectedRegion = uiState.selectedRegion,
                    onSelectRegion = { viewModel.selectRegion(it) },
                    selectedType = uiState.selectedRecreationType,
                    onSelectType = { viewModel.selectRecreationType(it) },
                    filterAvailableOnly = uiState.filterAvailableOnly,
                    onToggleAvailableOnly = { viewModel.toggleFilterAvailableOnly() },
                    filterNoPassNeeded = uiState.filterNoPassNeeded,
                    onToggleNoPassNeeded = { viewModel.toggleFilterNoPassNeeded() },
                    filterFreeParking = uiState.filterFreeParkingOnly,
                    onToggleFreeParking = { viewModel.toggleFilterFreeParking() },
                    filterEvCharging = uiState.filterEvCharging,
                    onToggleEvCharging = { viewModel.toggleFilterEvCharging() },
                    filterDogFriendly = uiState.filterDogFriendly,
                    onToggleDogFriendly = { viewModel.toggleFilterDogFriendly() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Status Header & Count with List/Map Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${filteredLots.size} BC Recreational Parking Lots",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else if (communityReports.isNotEmpty()) {
                            // Show Live Sync ONLY when community reports are actually loaded
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "Live Sync",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // List/Map Toggle
                    ListMapToggle(
                        viewMode = viewMode,
                        onModeChanged = { viewModel.setExploreViewMode(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Parking Lot Items
            if (filteredLots.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No matching parking lots found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try clearing active filters or searching for another BC recreational area.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                viewModel.updateSearchQuery("")
                                viewModel.selectRegion(null)
                                if (uiState.filterAvailableOnly) viewModel.toggleFilterAvailableOnly()
                                if (uiState.filterNoPassNeeded) viewModel.toggleFilterNoPassNeeded()
                            },
                            modifier = Modifier.testTag("reset_filters_button")
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            } else {
                items(filteredLots, key = { it.id }) { lot ->
                    val dist = BCParksDataSource.calculateDistanceKm(
                        currentUserLocation.latitude,
                        currentUserLocation.longitude,
                        lot.latitude,
                        lot.longitude
                    )
                    val driveMin = BCParksDataSource.calculateDrivingMinutes(
                        dist,
                        lot.region == ca.bcparkspot.app.model.ParkRegion.SEA_TO_SKY
                    )
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ParkingLotCard(
                            lot = lot,
                            distanceKm = dist,
                            drivingMinutes = driveMin,
                            isSaved = savedLotIds.contains(lot.id),
                            onSelect = { onSelectLot(lot) },
                            onNavigate = { onNavigateToLot(lot) },
                            onToggleSave = { viewModel.toggleSaveLot(lot) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean, accessible Segmented Toggle between List and Map modes on the lot-count row.
 */
@Composable
fun ListMapToggle(
    viewMode: ExploreViewMode,
    onModeChanged: (ExploreViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (viewMode == ExploreViewMode.LIST) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .clickable { onModeChanged(ExploreViewMode.LIST) }
                    .testTag("explore_toggle_list")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "List Mode",
                        modifier = Modifier.size(14.dp),
                        tint = if (viewMode == ExploreViewMode.LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "List",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewMode == ExploreViewMode.LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (viewMode == ExploreViewMode.MAP) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .clickable { onModeChanged(ExploreViewMode.MAP) }
                    .testTag("explore_toggle_map")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map Mode",
                        modifier = Modifier.size(14.dp),
                        tint = if (viewMode == ExploreViewMode.MAP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Map",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewMode == ExploreViewMode.MAP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
