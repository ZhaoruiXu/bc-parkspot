package ca.bcparkspot.app.ui.viewmodel

import ca.bcparkspot.app.ui.prediction.fallbackStatus
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.bcparkspot.app.AppGraph
import ca.bcparkspot.app.auth.AccountBusy
import ca.bcparkspot.app.auth.AccountOutcome
import ca.bcparkspot.app.auth.GoogleTokenResult
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.data.remote.CarPinProblem
import ca.bcparkspot.app.data.remote.CloudLimits
import ca.bcparkspot.app.data.remote.carPinProblem
import ca.bcparkspot.app.data.remote.isValidReportComment
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.data.repository.ParkingRepository
import ca.bcparkspot.app.sync.FeedStatus
import ca.bcparkspot.app.ui.account.AccountPrompt
import ca.bcparkspot.app.ui.account.AccountUiState
import ca.bcparkspot.app.ui.account.busyMessage
import ca.bcparkspot.app.ui.account.feedMessage
import ca.bcparkspot.app.ui.account.outcomeMessage
import ca.bcparkspot.app.ui.account.tokenMessage
import ca.bcparkspot.app.ui.account.toCommunityReport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID
import java.util.concurrent.TimeUnit
import ca.bcparkspot.app.model.*
import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.NearestResult
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.ui.text.noConfirmedMessage
import ca.bcparkspot.app.ui.text.stateHeadline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ca.bcparkspot.app.ui.screens.ExploreViewMode

data class ParkingUiState(
    val searchQuery: String = "",
    val selectedRegion: ParkRegion? = null,
    val selectedRecreationType: RecreationType? = null,
    val filterAvailableOnly: Boolean = false,
    val filterNoPassNeeded: Boolean = false,
    val filterFreeParkingOnly: Boolean = false,
    val filterEvCharging: Boolean = false,
    val filterDogFriendly: Boolean = false,
    val selectedLot: ParkingLot? = null,
    val activeNavigationRoute: NavigationRoute? = null,
    val isReportingDialogVisible: Boolean = false,
    val isSaveCarSheetVisible: Boolean = false,
    val isAuthDialogVisible: Boolean = false,
    val accountMessage: String? = null,
    val isPrivacyDialogVisible: Boolean = false,
    val isDisclaimerVisible: Boolean = false,
    val isRefreshing: Boolean = false,
    val userNotification: String? = null,
    val exploreViewMode: ExploreViewMode = ExploreViewMode.LIST,
    val selectedMapLot: ParkingLot? = null
)

private const val LOG_TAG = "BCParkSpotAccount"

object ViewModelTiming {
    const val STATUS_REFRESH_SECONDS = 60L
    val statusRefreshMs: Long = TimeUnit.SECONDS.toMillis(STATUS_REFRESH_SECONDS)
    const val SUBSCRIPTION_KEEPALIVE_MS = 5_000L
}

@OptIn(ExperimentalCoroutinesApi::class)
class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = AppGraph.from(application)
    private val local = graph.local
    private val accounts = graph.accounts
    private val res get() = getApplication<Application>().resources
    val policy = graph.policy
    internal val localRepository = ParkingRepository(graph.policy, graph.clock)

    fun updateCommunityReportsForTesting(reports: List<CommunityReport>) {
        testReports.value = reports
    }

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    val parkingLots: StateFlow<List<ParkingLot>> = localRepository.parkingLots
    val currentUserLocation: StateFlow<UserLocation> = localRepository.currentUserLocation
    private val _deviceGpsLocation = MutableStateFlow<UserLocation?>(null)
    val deviceGpsLocation: StateFlow<UserLocation?> = _deviceGpsLocation.asStateFlow()

    private val keepAlive = SharingStarted.WhileSubscribed(ViewModelTiming.SUBSCRIPTION_KEEPALIVE_MS)

    val savedLots: StateFlow<List<SavedLotEntity>> = accounts.ownerUid.flatMapLatest { local.savedLots(it) }
        .stateIn(viewModelScope, keepAlive, emptyList())
    val parkedCar: StateFlow<ParkedCarEntity?> = accounts.ownerUid.flatMapLatest { local.parkedCar(it) }
        .stateIn(viewModelScope, keepAlive, null)
    private val ownReports: StateFlow<List<OwnReportEntity>> = accounts.ownerUid.flatMapLatest { local.ownReports(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Reports injected by JVM tests; when set they replace the cloud feed. */
    private val testReports = MutableStateFlow<List<CommunityReport>?>(null)
    private val serverReports = combine(graph.cloud.reports, testReports) { cloud, injected -> injected ?: cloud }

    /** The shared feed plus this device's own reports still waiting to sync, which are shown but never counted (4.14). */
    val communityReports: StateFlow<List<CommunityReport>> = combine(localRepository.communityReports, ownReports) { shared, own ->
        val sharedIds = shared.mapTo(HashSet()) { it.id }
        shared + own.filter { it.syncState == ReportSyncState.PENDING && it.id !in sharedIds }.mapNotNull { it.toCommunityReport() }
    }.stateIn(viewModelScope, keepAlive, emptyList())

    val waitingReportIds: StateFlow<Set<String>> = ownReports
        .map { rows -> rows.filter { it.syncState == ReportSyncState.PENDING }.mapTo(HashSet()) { it.id } }
        .stateIn(viewModelScope, keepAlive, emptySet())

    val feed: StateFlow<FeedStatus> = graph.cloud.feed

    val account: StateFlow<AccountUiState> = combine(
        combine(accounts.user, accounts.busy, accounts.deletionPending, graph.network.online, graph.lastSyncAt) { user, busy, deleting, online, lastSync ->
            AccountUiState(
                isGuest = user == null || user.isAnonymous,
                email = user?.email,
                lastSyncAt = lastSync,
                online = online,
                busy = busy,
                deletionPending = deleting != null
            )
        },
        accounts.ownerUid.flatMapLatest { local.pendingChangeCount(it) },
        accounts.ownerUid.flatMapLatest { local.syncErrorCount(it) },
        ownReports.map { rows -> rows.count { it.syncState == ReportSyncState.REJECTED } }
    ) { base, pending, errors, rejected ->
        base.copy(pendingChanges = pending, notSyncedItems = errors, rejectedReports = rejected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountUiState())

    private val _prompt = MutableStateFlow<AccountPrompt?>(null)
    val prompt: StateFlow<AccountPrompt?> = _prompt.asStateFlow()
    private var promptAnswer: CompletableDeferred<Boolean>? = null

    init {
        viewModelScope.launch {
            combine(serverReports, local.pendingReportIds()) { reports, pending -> reports to pending.toSet() }
                .collect { (reports, pending) -> localRepository.updateCommunityReports(reports, pending) }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(ViewModelTiming.statusRefreshMs)
                localRepository.recalculateLotConsensus()
                refreshSelectedLot()
            }
        }
        viewModelScope.launch {
            parkingLots.collect { refreshSelectedLot() }
        }
    }

    private fun refreshSelectedLot() {
        _uiState.update { current ->
            current.copy(selectedLot = current.selectedLot?.let { sel -> parkingLots.value.firstOrNull { it.id == sel.id } ?: sel })
        }
    }

    /** Results of account flows show inside the account sheet when it is open, since a snackbar would sit behind it. */
    private fun notifyAccount(message: String?) {
        if (message == null) return
        _uiState.update { if (it.isAuthDialogVisible) it.copy(accountMessage = message) else it.copy(userNotification = message) }
    }

    private fun notify(message: String?) {
        if (message != null) _uiState.update { it.copy(userNotification = message) }
    }

    /** 3.6: while an account flow runs, the user's own writes wait. */
    private fun writesBlocked(): Boolean {
        val busy = accounts.busy.value ?: AccountBusy.DELETING.takeIf { accounts.deletionPending.value != null }
        busy?.let { notify(busyMessage(res, it)) }
        return busy != null
    }

    // Derived filtered lots
    val filteredLots: StateFlow<List<ParkingLot>> = combine(
        localRepository.parkingLots,
        _uiState,
        currentUserLocation
    ) { lots, state, userLoc ->
        lots.filter { lot ->
            val matchesQuery = state.searchQuery.isBlank() ||
                    lot.parkName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.lotName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.region.displayName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.authority.displayName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.proTips.contains(state.searchQuery, ignoreCase = true)

            val matchesRegion = state.selectedRegion == null || lot.region == state.selectedRegion
            val matchesType = state.selectedRecreationType == null || lot.type == state.selectedRecreationType
            val matchesAvailable = !state.filterAvailableOnly || (lot.effectiveStatus == AvailabilityStatus.AVAILABLE || lot.effectiveStatus == AvailabilityStatus.FILLING_FAST)
            val matchesPass = !state.filterNoPassNeeded || lot.getPassState() == PassRequirementStatus.NOT_REQUIRED
            val matchesFree = !state.filterFreeParkingOnly || lot.feeType == ParkingFeeType.FREE || lot.feeType == ParkingFeeType.FREE_HYDRO_PASS
            val matchesEv = !state.filterEvCharging || lot.evChargingSpots > 0
            val matchesDog = !state.filterDogFriendly || lot.isDogFriendly

            matchesQuery && matchesRegion && matchesType && matchesAvailable &&
                    matchesPass && matchesFree && matchesEv && matchesDog
        }.sortedBy { lot ->
            BCParksDataSource.calculateDistanceKm(
                userLoc.latitude, userLoc.longitude, lot.latitude, lot.longitude
            )
        }
    }.stateIn(viewModelScope, keepAlive, emptyList())

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectRegion(region: ParkRegion?) {
        _uiState.update { it.copy(selectedRegion = region) }
    }

    fun selectRecreationType(type: RecreationType?) {
        _uiState.update { it.copy(selectedRecreationType = type) }
    }

    fun toggleFilterAvailableOnly() {
        _uiState.update { it.copy(filterAvailableOnly = !it.filterAvailableOnly) }
    }

    fun toggleFilterNoPassNeeded() {
        _uiState.update { it.copy(filterNoPassNeeded = !it.filterNoPassNeeded) }
    }

    fun toggleFilterFreeParking() {
        _uiState.update { it.copy(filterFreeParkingOnly = !it.filterFreeParkingOnly) }
    }

    fun toggleFilterEvCharging() {
        _uiState.update { it.copy(filterEvCharging = !it.filterEvCharging) }
    }

    fun toggleFilterDogFriendly() {
        _uiState.update { it.copy(filterDogFriendly = !it.filterDogFriendly) }
    }

    fun selectLot(lot: ParkingLot?) {
        _uiState.update { it.copy(selectedLot = lot) }
    }

    fun setExploreViewMode(mode: ExploreViewMode) {
        _uiState.update { it.copy(exploreViewMode = mode) }
    }

    fun selectMapLot(lot: ParkingLot?) {
        _uiState.update { it.copy(selectedMapLot = lot) }
    }

    fun updateDeviceGpsLocation(location: UserLocation?) {
        _deviceGpsLocation.value = location
    }

    fun startNavigation(lot: ParkingLot) {
        val route = localRepository.getRouteToLot(currentUserLocation.value, lot)
        _uiState.update {
            it.copy(
                activeNavigationRoute = route,
                selectedLot = lot,
                userNotification = "Viewing directions to ${lot.lotName}"
            )
        }
    }

    fun findNearestOpenSpot(targetParkId: String? = null) {
        val origin = currentUserLocation.value
        val (result, route) = localRepository.findNearestOpenSpot(origin, targetParkId)
        val res = getApplication<Application>().resources
        val lot = route?.destinationLot
        val message = when (result) {
            is NearestResult.Confirmed ->
                res.getString(R.string.nearest_found, lot?.lotName.orEmpty(), origin.name)
            is NearestResult.Fallback ->
                noConfirmedMessage(res, ReportPolicies.DEFAULT) + " " + res.getString(
                    R.string.nearest_fallback, lot?.lotName.orEmpty(), fallbackStatus(res, result.candidate), origin.name
                )
            NearestResult.NothingQualifies ->
                noConfirmedMessage(res, ReportPolicies.DEFAULT) + " " + res.getString(R.string.nearest_nothing)
        }
        _uiState.update {
            if (lot == null) it.copy(userNotification = message)
            else it.copy(selectedLot = lot, activeNavigationRoute = route, userNotification = message)
        }
    }

    fun clearActiveRoute() {
        _uiState.update { it.copy(activeNavigationRoute = null) }
    }

    fun refreshLiveAvailability() {
        localRepository.recalculateLotConsensus()
        refreshSelectedLot()
        graph.syncControl.request()
        notify(feedMessage(res, feed.value, graph.clock()))
    }

    fun setUserLocation(location: UserLocation) {
        localRepository.setUserLocation(location)
        _uiState.value.selectedLot?.let { lot ->
            if (_uiState.value.activeNavigationRoute != null) {
                val updatedRoute = localRepository.getRouteToLot(location, lot)
                _uiState.update { it.copy(activeNavigationRoute = updatedRoute) }
            }
        }
    }

    fun toggleSaveLot(lot: ParkingLot) {
        if (writesBlocked()) return
        val owner = accounts.ownerUid.value
        val isSaved = savedLots.value.any { it.lotId == lot.id }
        viewModelScope.launch {
            if (isSaved) {
                local.unsaveLot(owner, lot.id, graph.clock())
            } else {
                local.saveLot(owner, lot.id, lot.parkName, lot.lotName, lot.region.displayName, "", graph.clock())
            }
            graph.syncControl.request()
            notify(res.getString(if (isSaved) R.string.lot_unsaved else R.string.lot_saved))
        }
    }

    fun showCommunityReportDialog(visible: Boolean) {
        _uiState.update { it.copy(isReportingDialogVisible = visible) }
    }

    fun showSaveCarSheet(visible: Boolean) {
        _uiState.update { it.copy(isSaveCarSheetVisible = visible) }
    }

    fun showAuthDialog(visible: Boolean) {
        _uiState.update { it.copy(isAuthDialogVisible = visible, accountMessage = null) }
    }

    fun showDisclaimer(visible: Boolean) {
        _uiState.update { it.copy(isDisclaimerVisible = visible) }
    }

    fun showPrivacyDialog(visible: Boolean) {
        _uiState.update { it.copy(isPrivacyDialogVisible = visible) }
    }

    fun submitCommunityReport(
        lotId: String,
        status: AvailabilityStatus,
        comment: String,
        spots: Int?
    ) {
        val trimmedComment = comment.trim()
        if (!isValidReportComment(trimmedComment)) {
            notify(res.getString(R.string.report_comment_length, CloudLimits.MIN_COMMENT_LENGTH, CloudLimits.MAX_COMMENT_LENGTH))
            return
        }
        if (writesBlocked()) return
        val owner = accounts.ownerUid.value
        val lot = parkingLots.value.firstOrNull { it.id == lotId } ?: return
        viewModelScope.launch {
            local.addReport(
                OwnReportEntity(
                    id = UUID.randomUUID().toString(),
                    ownerUid = owner,
                    lotId = lotId,
                    parkName = lot.parkName,
                    reporterName = res.getString(R.string.default_reporter_name),
                    status = status.name,
                    comment = trimmedComment,
                    spotsEstimate = spots,
                    observedAt = graph.clock()
                )
            )
            graph.syncControl.request()
            _uiState.update { it.copy(isReportingDialogVisible = false, userNotification = res.getString(R.string.report_saved)) }
        }
    }

    fun saveParkedCar(
        stall: String,
        notes: String,
        deviceLocation: android.location.Location? = null
    ) {
        carPinProblem(stall, notes)?.let { problem ->
            notify(
                when (problem) {
                    CarPinProblem.STALL_TOO_LONG -> res.getString(R.string.car_stall_too_long, CloudLimits.MAX_STALL_LENGTH)
                    CarPinProblem.NOTES_TOO_LONG -> res.getString(R.string.car_notes_too_long, CloudLimits.MAX_NOTES_LENGTH)
                }
            )
            return
        }
        if (writesBlocked()) return
        val currentLot = _uiState.value.selectedLot ?: parkingLots.value.first()
        val owner = accounts.ownerUid.value
        val isGpsFix = deviceLocation != null
        val now = graph.clock()
        val car = ParkedCarEntity(
            ownerUid = owner,
            parkName = currentLot.parkName,
            lotName = currentLot.lotName,
            latitude = deviceLocation?.latitude ?: currentLot.latitude,
            longitude = deviceLocation?.longitude ?: currentLot.longitude,
            stallNumber = stall,
            notes = notes,
            timestamp = now,
            isExactGpsLocation = isGpsFix,
            updatedAt = now
        )
        viewModelScope.launch {
            local.pinCar(owner, car)
            graph.syncControl.request()
            _uiState.update {
                it.copy(
                    isSaveCarSheetVisible = false,
                    userNotification = res.getString(if (isGpsFix) R.string.car_saved_gps else R.string.car_saved_lot_pin)
                )
            }
        }
    }

    fun clearParkedCar() {
        if (writesBlocked()) return
        val owner = accounts.ownerUid.value
        viewModelScope.launch {
            local.clearCar(owner, graph.clock())
            graph.syncControl.request()
            notify(res.getString(R.string.car_cleared))
        }
    }

    fun discardRejectedReports() {
        viewModelScope.launch {
            ownReports.value.filter { it.syncState == ReportSyncState.REJECTED }.forEach { local.discardReport(it.id) }
        }
    }

    fun retryNotSynced() {
        val owner = accounts.ownerUid.value
        viewModelScope.launch {
            local.clearSyncErrors(owner)
            graph.syncControl.request()
        }
    }

    private suspend fun ask(prompt: AccountPrompt): Boolean {
        val answer = CompletableDeferred<Boolean>()
        promptAnswer = answer
        _prompt.value = prompt
        return try {
            answer.await()
        } finally {
            _prompt.value = null
            promptAnswer = null
        }
    }

    fun answerPrompt(accepted: Boolean) {
        promptAnswer?.complete(accepted)
    }

    /** 4.15 and 4.16. [requestToken] shows the Google account picker; it runs only from a tap. */
    fun signInWithGoogle(requestToken: suspend () -> GoogleTokenResult) {
        if (!account.value.online || account.value.busy != null) return
        viewModelScope.launch {
            when (val token = requestToken()) {
                is GoogleTokenResult.Token -> {
                    val outcome = accounts.signInWithGoogle(token.idToken) { items -> ask(AccountPrompt.ConfirmMerge(items)) }
                    Log.i(LOG_TAG, "Google sign-in outcome: $outcome")
                    notifyAccount(outcomeMessage(res, outcome))
                }
                else -> {
                    Log.i(LOG_TAG, "Google token request ended without a token: $token")
                    notifyAccount(tokenMessage(res, token))
                }
            }
        }
    }

    /** 4.18. */
    fun signOut(forgetGoogleChoice: suspend () -> Unit) {
        if (!account.value.online || account.value.busy != null) return
        viewModelScope.launch {
            val outcome = accounts.signOut { pending -> ask(AccountPrompt.ConfirmSignOut(pending)) }
            if (outcome is AccountOutcome.SignedOut) forgetGoogleChoice()
            notifyAccount(outcomeMessage(res, outcome))
        }
    }

    /** 4.19. The sheet has already asked the user to confirm. A Google account re-authenticates through [requestToken]. */
    fun deleteMyData(requestToken: suspend () -> GoogleTokenResult) {
        if (!account.value.online || account.value.busy != null) return
        viewModelScope.launch {
            val retry = account.value.deletionPending
            val token = if (account.value.isGuest) null else when (val result = requestToken()) {
                is GoogleTokenResult.Token -> result.idToken
                else -> {
                    notifyAccount(tokenMessage(res, result))
                    return@launch
                }
            }
            val outcome = if (retry) accounts.retryDeletion(token) else accounts.deleteMyData(token)
            Log.i(LOG_TAG, "Delete my data outcome: $outcome")
            if (outcome == AccountOutcome.Deleted) _uiState.update { it.copy(isAuthDialogVisible = false, accountMessage = null) }
            notifyAccount(outcomeMessage(res, outcome))
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }
}
