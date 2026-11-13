package ca.bcparkspot.app.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.auth.AuthGateway
import ca.bcparkspot.app.auth.AuthUser
import ca.bcparkspot.app.auth.LinkOutcome
import ca.bcparkspot.app.data.local.AppDatabase
import ca.bcparkspot.app.data.local.DevicePrefs
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.sync.FlushResult
import ca.bcparkspot.app.sync.RemoteErrorKind
import ca.bcparkspot.app.sync.RemoteStore
import ca.bcparkspot.app.sync.RemoteWriteException
import ca.bcparkspot.app.sync.SyncControl
import ca.bcparkspot.app.sync.UpsertResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

fun inMemoryDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext<Context>(),
    AppDatabase::class.java
).allowMainThreadQueries().build()

/** A cloud that behaves like Firestore plus the rules: last-writer-wins records and create-only reports. */
class FakeRemoteStore : RemoteStore {
    val savedLots = mutableMapOf<Pair<String, String>, SavedLotEntity>()
    val cars = mutableMapOf<String, ParkedCarEntity>()
    val reports = mutableMapOf<String, Pair<String, OwnReportEntity>>()

    var savedLotError: RemoteErrorKind? = null
    var carError: RemoteErrorKind? = null
    var reportError: RemoteErrorKind? = null
    var deleteFails = false
    var flushResult = FlushResult.FLUSHED
    var createCalls = 0
    val deletedUids = mutableListOf<String>()

    /** Runs inside an upload, before it is acknowledged, to simulate a change made meanwhile. */
    var duringUpload: suspend () -> Unit = {}

    override suspend fun upsertSavedLot(uid: String, row: SavedLotEntity): UpsertResult<SavedLotEntity> {
        savedLotError?.let { throw RemoteWriteException(it, it.name) }
        duringUpload()
        val cloud = savedLots[uid to row.lotId]
        if (cloud != null && cloud.updatedAt > row.updatedAt) return UpsertResult.CloudNewer(cloud)
        savedLots[uid to row.lotId] = row.copy(ownerUid = uid)
        return UpsertResult.Written
    }

    override suspend fun upsertParkedCar(uid: String, row: ParkedCarEntity): UpsertResult<ParkedCarEntity> {
        carError?.let { throw RemoteWriteException(it, it.name) }
        duringUpload()
        val cloud = cars[uid]
        if (cloud != null && cloud.updatedAt > row.updatedAt) return UpsertResult.CloudNewer(cloud)
        cars[uid] = row.copy(ownerUid = uid)
        return UpsertResult.Written
    }

    override suspend fun createReport(uid: String, row: OwnReportEntity) {
        createCalls++
        reportError?.let { throw RemoteWriteException(it, it.name) }
        if (row.id in reports) throw RemoteWriteException(RemoteErrorKind.PERMISSION_DENIED, "update denied")
        reports[row.id] = uid to row
    }

    override suspend fun reportAuthor(reportId: String): String? = reports[reportId]?.first

    override suspend fun deleteCloudDataOf(uid: String) {
        if (deleteFails) throw RemoteWriteException(RemoteErrorKind.RETRYABLE, "offline")
        savedLots.keys.removeAll { it.first == uid }
        cars.remove(uid)
        reports.values.removeAll { it.first == uid }
        deletedUids += uid
    }

    override suspend fun flush(timeoutMs: Long): FlushResult = flushResult
}

class FakeSyncControl : SyncControl {
    var requests = 0
    var cancels = 0
    var uploads = 0
    var onUploadNow: suspend (String) -> Unit = {}

    override fun request() {
        requests++
    }

    override suspend fun cancelAndAwait() {
        cancels++
    }

    override suspend fun uploadNow(uid: String, timeoutMs: Long) {
        uploads++
        onUploadNow(uid)
    }
}

class FakePrefs : DevicePrefs {
    override var deletionPendingUid: String? = null
    override var lastServerSnapshotAt: Long? = null
    override var lastSuccessfulSyncAt: Long? = null
}

/** Firebase Auth as the account flows see it; records every user it ever reported as current. */
class FakeAuthGateway(initial: AuthUser?) : AuthGateway {
    private val current = MutableStateFlow(initial)
    val history = mutableListOf(initial)
    private var nextGuest = 0

    var linkOutcome = LinkOutcome.COLLISION
    var googleUser = AuthUser("google-uid", isAnonymous = false, email = "hiker@example.com")
    var signInFails = false
    var reauthFails = false
    var deleteFails = false
    var deletedUsers = 0

    private fun set(user: AuthUser?) {
        current.value = user
        history += user
    }

    override fun currentUser(): AuthUser? = current.value
    override fun users(): Flow<AuthUser?> = current

    override suspend fun signInAnonymously() {
        set(AuthUser("guest-${nextGuest++}", isAnonymous = true, email = null))
    }

    override suspend fun linkGoogle(idToken: String): LinkOutcome {
        if (linkOutcome == LinkOutcome.LINKED) set(current.value!!.copy(isAnonymous = false, email = googleUser.email))
        return linkOutcome
    }

    override suspend fun signInGoogle(idToken: String) {
        if (signInFails) error("sign-in failed")
        set(googleUser)
    }

    override suspend fun reauthenticateGoogle(idToken: String) {
        if (reauthFails) error("re-auth failed")
    }

    override suspend fun deleteCurrentUser() {
        if (deleteFails) error("delete failed")
        deletedUsers++
        set(null)
    }

    override fun signOut() = set(null)
}
