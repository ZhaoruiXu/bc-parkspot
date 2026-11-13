package ca.bcparkspot.app

import ca.bcparkspot.app.ui.theme.Spacing
import ca.bcparkspot.app.ui.theme.Sizes
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.ui.res.stringResource
import ca.bcparkspot.app.ui.theme.resolveDarkTheme
import ca.bcparkspot.app.ui.theme.ThemePreferenceStore
import ca.bcparkspot.app.ui.theme.ThemePreference
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import android.graphics.drawable.ColorDrawable
import ca.bcparkspot.app.ui.theme.SystemBarColors
import ca.bcparkspot.app.ui.theme.windowBackgroundFor
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.SystemBarStyle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import ca.bcparkspot.app.model.ParkingLot
import ca.bcparkspot.app.ui.components.AccountPromptDialog
import ca.bcparkspot.app.ui.components.AccountSheet
import ca.bcparkspot.app.auth.clearGoogleCredentialState
import ca.bcparkspot.app.auth.requestGoogleIdToken
import androidx.compose.ui.platform.LocalContext
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

    override fun onStart() {
        super.onStart()
        AppGraph.from(this).cloud.setForeground(true)
    }

    override fun onStop() {
        AppGraph.from(this).cloud.setForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            org.maplibre.android.MapLibre.getInstance(this)
        } catch (e: Throwable) {
            // Fail gracefully if running under JVM test environment
        }

        val themeStore = ThemePreferenceStore(this)
        val initialTheme = themeStore.load()
        themeStore.applyToSystem(initialTheme)

        setContent {
            var themePreference by remember { mutableStateOf(initialTheme) }
            val dark = resolveDarkTheme(themePreference, isSystemInDarkTheme())
            DisposableEffect(dark) {
                val transparent = Color.Transparent.toArgb()
                val statusStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                val navStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, SystemBarColors.lightIconsScrim.toArgb())
                enableEdgeToEdge(statusBarStyle = statusStyle, navigationBarStyle = navStyle)
                window.setBackgroundDrawable(ColorDrawable(windowBackgroundFor(dark).toArgb()))
                onDispose {}
            }
            BCParkSpotTheme(preference = themePreference) {
                MainAppRoot(
                    viewModel = viewModel,
                    themePreference = themePreference,
                    onThemePreferenceChange = {
                        themeStore.save(it)
                        themePreference = it
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppRoot(
    viewModel: ParkingViewModel,
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    onThemePreferenceChange: (ThemePreference) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MainNavTab.EXPLORE) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredLots by viewModel.filteredLots.collectAsStateWithLifecycle()
    val allLots by viewModel.parkingLots.collectAsStateWithLifecycle()
    val savedLots by viewModel.savedLots.collectAsStateWithLifecycle()
    val parkedCar by viewModel.parkedCar.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val waitingReportIds by viewModel.waitingReportIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userLocation by viewModel.currentUserLocation.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle toast/notification
    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg, withDismissAction = true, duration = SnackbarDuration.Long)
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
                            waitingReportIds = waitingReportIds,
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
                            destinationNow = allLots.firstOrNull { it.id == activeRoute.destinationLot.id } ?: activeRoute.destinationLot,
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
                                imageVector = Icons.Default.Hiking,
                                contentDescription = null,
                                modifier = Modifier.size(Sizes.emptyStateIcon),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Text(
                                text = stringResource(R.string.route_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = stringResource(R.string.route_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(Spacing.xl))
                            Button(
                                onClick = {
                                    viewModel.findNearestOpenSpot()
                                },
                                modifier = Modifier.testTag("empty_route_find_nearest_button")
                            ) {
                                Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(stringResource(R.string.route_empty_button))
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

            if (uiState.isAuthDialogVisible) {
                AccountSheet(
                    state = account,
                    message = uiState.accountMessage,
                    nowMs = System.currentTimeMillis(),
                    themePreference = themePreference,
                    onThemePreferenceChange = onThemePreferenceChange,
                    onDismiss = { viewModel.showAuthDialog(false) },
                    onSignIn = { viewModel.signInWithGoogle { requestGoogleIdToken(context) } },
                    onSignOut = { viewModel.signOut { clearGoogleCredentialState(context) } },
                    onDeleteConfirmed = { viewModel.deleteMyData { requestGoogleIdToken(context) } },
                    onRetryNotSynced = { viewModel.retryNotSynced() },
                    onDiscardRejected = { viewModel.discardRejectedReports() },
                    onOpenPrivacyPolicy = { viewModel.showPrivacyDialog(true) }
                )
            }

            prompt?.let { pending ->
                AccountPromptDialog(prompt = pending, policy = viewModel.policy, onAnswer = viewModel::answerPrompt)
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
