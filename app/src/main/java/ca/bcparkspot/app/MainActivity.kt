package ca.bcparkspot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import ca.bcparkspot.app.auth.AuthManager
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.ui.components.AuthDialog
import ca.bcparkspot.app.ui.components.CommunityReportDialog
import ca.bcparkspot.app.ui.components.PrivacyPolicyDialog
import ca.bcparkspot.app.ui.components.SaveCarSheet
import ca.bcparkspot.app.ui.screens.*
import ca.bcparkspot.app.ui.theme.BCParkSpotTheme
import ca.bcparkspot.app.ui.viewmodel.ParkingViewModel
import kotlinx.coroutines.launch

enum class MainNavTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    EXPLORE("Explore", Icons.Filled.LocalParking, Icons.Outlined.LocalParking),
    ROUTE("Route", Icons.Filled.Navigation, Icons.Outlined.Navigation),
    SAVED("Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    PASS_GUIDE("Passes", Icons.Filled.AssignmentLate, Icons.Outlined.AssignmentLate)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ParkingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            org.maplibre.android.MapLibre.getInstance(this)
        } catch (e: Throwable) {
            // Fail gracefully if running under JVM test environment
        }

        // Silent auto-sign-in on app start
        try {
            val credentialManager = CredentialManager.create(this)
            AuthManager.attemptAutoSignIn(
                context = this,
                credentialManager = credentialManager,
                onAuthSuccess = { /* Automatically picked up by FirebaseAuth listener */ },
                onUnauthenticated = { /* Retain local offline state */ },
                scope = lifecycleScope
            )
        } catch (e: Throwable) {
            // Fail gracefully if credential services are unavailable on device
        }

        setContent {
            BCParkSpotTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppRoot(viewModel: ParkingViewModel) {
    var selectedTab by remember { mutableStateOf(MainNavTab.EXPLORE) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredLots by viewModel.filteredLots.collectAsStateWithLifecycle()
    val allLots by viewModel.parkingLots.collectAsStateWithLifecycle()
    val savedLots by viewModel.savedLots.collectAsStateWithLifecycle()
    val parkedCar by viewModel.parkedCar.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userLocation by viewModel.currentUserLocation.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle toast/notification
    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    // Auto-switch to ROUTE tab if an active route is initiated
    LaunchedEffect(uiState.activeNavigationRoute) {
        if (uiState.activeNavigationRoute != null) {
            selectedTab = MainNavTab.ROUTE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                tonalElevation = 6.dp
            ) {
                MainNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            if (tab == MainNavTab.EXPLORE) {
                                viewModel.selectLot(null)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        alwaysShowLabel = true,
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            when (selectedTab) {
                MainNavTab.EXPLORE -> {
                    val selectedLot = uiState.selectedLot
                    if (selectedLot != null) {
                        LotDetailScreen(
                            lot = selectedLot,
                            isSaved = savedLots.any { it.lotId == selectedLot.id },
                            communityReports = communityReports,
                            onBack = { viewModel.selectLot(null) },
                            onNavigate = { viewModel.startNavigation(selectedLot) },
                            onFindNearestOpen = { viewModel.findNearestOpenSpot(selectedLot.parkId) },
                            onToggleSave = { viewModel.toggleSaveLot(selectedLot) },
                            onOpenReportDialog = { viewModel.showCommunityReportDialog(true) },
                            onOpenSaveCarSheet = { viewModel.showSaveCarSheet(true) },
                            onSelectOverflowLot = { overflowId ->
                                val target = allLots.find { it.id == overflowId }
                                if (target != null) viewModel.selectLot(target)
                            }
                        )
                    } else {
                        LotsListScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            filteredLots = filteredLots,
                            savedLotIds = savedLots.map { it.lotId }.toSet(),
                            onSelectLot = { lot -> viewModel.selectLot(lot) },
                            onNavigateToLot = { lot -> viewModel.startNavigation(lot) },
                            onFindNearestOpen = { viewModel.findNearestOpenSpot() },
                            onOpenAuth = { viewModel.showAuthDialog(true) }
                        )
                    }
                }

                MainNavTab.ROUTE -> {
                    val activeRoute = uiState.activeNavigationRoute
                    if (activeRoute != null) {
                        RouteNavigationScreen(
                            route = activeRoute,
                            onBack = {
                                viewModel.clearActiveRoute()
                                selectedTab = MainNavTab.EXPLORE
                            },
                            onSelectAlternativeLot = { altLot ->
                                viewModel.startNavigation(altLot)
                            },
                            onOpenSaveCarSheet = {
                                viewModel.showSaveCarSheet(true)
                            }
                        )
                    } else {
                        // Empty Route placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Active Parking Route",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap 'Navigate to Nearest Open Spot' or select any BC recreation lot to calculate driving routes and live capacity.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    viewModel.findNearestOpenSpot()
                                },
                                modifier = Modifier.testTag("empty_route_find_nearest_button")
                            ) {
                                Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Find Nearest Open Spot Now")
                            }
                        }
                    }
                }

                MainNavTab.SAVED -> {
                    SavedLotsScreen(
                        parkedCar = parkedCar,
                        savedLots = savedLots,
                        allLots = allLots,
                        onSelectLot = { lot ->
                            viewModel.selectLot(lot)
                            selectedTab = MainNavTab.EXPLORE
                        },
                        onNavigateToLot = { lot ->
                            viewModel.startNavigation(lot)
                        },
                        onDeleteSavedLot = { lot ->
                            viewModel.toggleSaveLot(lot)
                        },
                        onClearParkedCar = {
                            viewModel.clearParkedCar()
                        },
                        userLocation = userLocation
                    )
                }

                MainNavTab.PASS_GUIDE -> {
                    BCPassInfoScreen()
                }
            }

            // Google Auth Dialog
            if (uiState.isAuthDialogVisible) {
                AuthDialog(
                    currentUser = currentUser,
                    onDismiss = { viewModel.showAuthDialog(false) },
                    onAuthSuccess = {
                        viewModel.showAuthDialog(false)
                    },
                    onSignOutSuccess = {
                        viewModel.showAuthDialog(false)
                    },
                    onOpenPrivacyPolicy = {
                        viewModel.showPrivacyDialog(true)
                    },
                    onDeleteAccount = {
                        viewModel.deleteUserAccountAndData(
                            onSuccess = {
                                viewModel.showAuthDialog(false)
                            },
                            onError = { /* notification handled in viewModel */ }
                        )
                    }
                )
            }

            // In-App Privacy Policy Dialog
            if (uiState.isPrivacyDialogVisible) {
                PrivacyPolicyDialog(
                    onDismiss = { viewModel.showPrivacyDialog(false) }
                )
            }

            // Reporting Dialog
            if (uiState.isReportingDialogVisible) {
                val currentLot = uiState.selectedLot ?: allLots.first()
                CommunityReportDialog(
                    lot = currentLot,
                    onDismiss = { viewModel.showCommunityReportDialog(false) },
                    onSubmit = { status, comment, spots ->
                        viewModel.submitCommunityReport(
                            lotId = currentLot.id,
                            reporterName = currentUser?.displayName ?: "BC Hiker",
                            status = status,
                            comment = comment,
                            spots = spots
                        )
                    }
                )
            }

            // Save Parked Car Dialog
            if (uiState.isSaveCarSheetVisible) {
                val currentLot = uiState.selectedLot ?: allLots.first()
                SaveCarSheet(
                    lot = currentLot,
                    onDismiss = { viewModel.showSaveCarSheet(false) },
                    onSave = { stall, notes, deviceLocation ->
                        viewModel.saveParkedCar(stall, notes, deviceLocation)
                    }
                )
            }
        }
    }
}
