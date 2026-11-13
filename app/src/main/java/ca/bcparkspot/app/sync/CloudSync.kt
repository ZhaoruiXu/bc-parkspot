package ca.bcparkspot.app.sync

import ca.bcparkspot.app.data.local.DevicePrefs
import ca.bcparkspot.app.data.local.LocalStore
import ca.bcparkspot.app.data.local.entity.CAR_PIN_DOCUMENT_ID
import ca.bcparkspot.app.data.remote.CloudFields
import ca.bcparkspot.app.data.remote.parkedCarFromCloud
import ca.bcparkspot.app.data.remote.reportFromCloud
import ca.bcparkspot.app.data.remote.savedLotFromCloud
import ca.bcparkspot.app.domain.ReportPolicy
import ca.bcparkspot.app.model.CommunityReport
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.concurrent.TimeUnit

object FeedTicker {
    const val TICK_SECONDS = 1L
}

/** Cloud-to-device listeners (4.13) and the shared reports feed (1.5, 1.6). */
class CloudSync(
    private val firestore: FirebaseFirestore,
    private val local: LocalStore,
    private val activeUid: StateFlow<String?>,
    private val prefs: DevicePrefs,
    private val policy: ReportPolicy,
    private val clock: () -> Long,
    private val scope: CoroutineScope
) {
    private val foreground = MutableStateFlow(false)
    private val _reports = MutableStateFlow<List<CommunityReport>>(emptyList())
    private val signals = MutableStateFlow(FeedSignals(false, false, clock(), prefs.lastServerSnapshotAt))

    val reports: StateFlow<List<CommunityReport>> = _reports.asStateFlow()

    private val ticks = flow {
        while (true) {
            emit(clock())
            delay(TimeUnit.SECONDS.toMillis(FeedTicker.TICK_SECONDS))
        }
    }

    val feed: StateFlow<FeedStatus> = combine(signals, ticks) { s, now -> feedStatus(s, now) }
        .stateIn(scope, SharingStarted.Eagerly, FeedStatus.Loading)

    fun setForeground(isForeground: Boolean) {
        foreground.value = isForeground
    }

    fun start() {
        scope.launch {
            activeUid.collectLatest { uid -> if (uid != null) listenToOwnData(uid) }
        }
        scope.launch {
            combine(foreground, activeUid) { fg, uid -> fg && uid != null }.collectLatest { listening ->
                if (listening) listenToReports()
            }
        }
    }

    private suspend fun listenToOwnData(uid: String) = coroutineScope {
        val user = firestore.collection(CloudFields.USERS).document(uid)
        launch {
            callbackFlow {
                val reg = user.collection(CloudFields.SAVED_LOTS).addSnapshotListener(MetadataChanges.INCLUDE) { snap, _ ->
                    if (snap != null) trySend(snap)
                }
                awaitClose { reg.remove() }
            }.collect { snap ->
                val fromCache = snap.metadata.isFromCache
                snap.documents.forEach { doc ->
                    doc.data?.let { savedLotFromCloud(uid, it) }?.let { local.applyCloudSavedLot(uid, it, fromCache) }
                }
            }
        }
        launch {
            callbackFlow {
                val reg = user.collection(CloudFields.PARKED_VEHICLES).document(CAR_PIN_DOCUMENT_ID)
                    .addSnapshotListener(MetadataChanges.INCLUDE) { snap, _ -> if (snap != null) trySend(snap) }
                awaitClose { reg.remove() }
            }.collect { snap ->
                snap.data?.let { parkedCarFromCloud(uid, it) }?.let { local.applyCloudCar(uid, it, snap.metadata.isFromCache) }
            }
        }
    }

    /** Re-subscribes once per refresh bucket, so every subscription within a bucket uses the identical query. */
    private suspend fun listenToReports() {
        signals.value = signals.value.copy(listenerStartedAt = clock(), lastSnapshotFromServer = false)
        while (true) {
            val now = clock()
            val lowerBound = reportQueryLowerBound(now, policy)
            val untilNextBucket = SyncPolicy.reportQueryRefreshMs - Math.floorMod(now, SyncPolicy.reportQueryRefreshMs)
            withTimeoutOrNull(untilNextBucket) { listenOnce(lowerBound) }
        }
    }

    private suspend fun listenOnce(lowerBound: Long) {
        callbackFlow {
            val reg = firestore.collection(CloudFields.REPORTS)
                .whereGreaterThanOrEqualTo(CloudFields.OBSERVED_AT, Timestamp(Date(lowerBound)))
                .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                    when {
                        error?.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                            signals.value = signals.value.copy(quotaExhausted = true)
                        snap != null -> trySend(snap)
                    }
                }
            awaitClose { reg.remove() }
        }.collect { snap ->
            val fromServer = !snap.metadata.isFromCache
            if (fromServer) prefs.lastServerSnapshotAt = clock()
            signals.value = signals.value.copy(
                quotaExhausted = false,
                lastSnapshotFromServer = fromServer,
                lastServerSnapshotAt = prefs.lastServerSnapshotAt
            )
            _reports.value = snap.documents
                .filterNot { it.metadata.hasPendingWrites() }
                .mapNotNull { doc -> doc.data?.let { reportFromCloud(doc.id, it) } }
        }
    }
}
