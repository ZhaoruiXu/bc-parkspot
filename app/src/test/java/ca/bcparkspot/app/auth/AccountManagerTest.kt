package ca.bcparkspot.app.auth

import ca.bcparkspot.app.data.local.AppDatabase
import ca.bcparkspot.app.data.local.LocalStore
import ca.bcparkspot.app.data.local.entity.LOCAL_DEVICE_OWNER
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.data.local.entity.needsUpload
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.sync.FlushResult
import ca.bcparkspot.app.sync.SyncEngine
import ca.bcparkspot.app.testutil.FakeAuthGateway
import ca.bcparkspot.app.testutil.FakePrefs
import ca.bcparkspot.app.testutil.FakeRemoteStore
import ca.bcparkspot.app.testutil.FakeSyncControl
import ca.bcparkspot.app.testutil.inMemoryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountManagerTest {
    private val guest = AuthUser("guest-uid", isAnonymous = true, email = null)
    private val policy = ReportPolicies.DEFAULT
    private val step = 1_000L
    private val now = policy.validityWindowMs * 100
    private val token = "id-token"

    private lateinit var db: AppDatabase
    private lateinit var local: LocalStore
    private lateinit var remote: FakeRemoteStore
    private lateinit var sync: FakeSyncControl
    private lateinit var prefs: FakePrefs
    private lateinit var auth: FakeAuthGateway
    private lateinit var scope: CoroutineScope
    private lateinit var accounts: AccountManager
    private val online = MutableStateFlow(true)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        local = LocalStore(db)
        remote = FakeRemoteStore()
        sync = FakeSyncControl()
        prefs = FakePrefs()
        auth = FakeAuthGateway(guest)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        accounts = AccountManager(auth, local, remote, sync, prefs, online, policy, { now }, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun guestHasItems() {
        local.saveLot(guest.uid, "lot", "Park", "Lot", "Region", "", now)
        local.pinCar(guest.uid, ParkedCarEntity(guest.uid, "Park", "Lot", 1.0, 1.0, timestamp = now, updatedAt = now))
        local.addReport(report("pending", now))
        local.addReport(report("recent", now - step))
        local.dao.setReportState("recent", ReportSyncState.SYNCED, null)
        remote.reports["recent"] = guest.uid to report("recent", now - step)
        local.addReport(report("old", now - policy.validityWindowMs - step))
        local.dao.setReportState("old", ReportSyncState.SYNCED, null)
        remote.savedLots[guest.uid to "lot"] = SavedLotEntity(guest.uid, "lot", "Park", "Lot", "Region", "", now, now)
    }

    private fun report(id: String, observedAt: Long) =
        OwnReportEntity(id, guest.uid, "lot", "Park", "BC Hiker", "AVAILABLE", "room", null, observedAt)

    private val movableItems = 4

    @Test
    fun `linking a guest keeps its uid and moves nothing`() = runBlocking {
        auth.linkOutcome = LinkOutcome.LINKED
        guestHasItems()
        assertEquals(AccountOutcome.Linked, accounts.signInWithGoogle(token) { error("no prompt expected") })
        assertEquals(guest.uid, auth.currentUser()!!.uid)
        assertEquals(1, local.dao.savedLotRows(guest.uid).size)
    }

    @Test
    fun `collision with nothing to move signs in without asking and never leaves the device signed out`() = runBlocking {
        assertEquals(AccountOutcome.Linked, accounts.signInWithGoogle(token) { error("no prompt expected") })
        assertEquals(auth.googleUser, auth.currentUser())
        assertFalse(auth.history.contains(null))
    }

    @Test
    fun `collision moves the guest's items into the Google account`() = runBlocking {
        guestHasItems()
        var asked = 0
        val outcome = accounts.signInWithGoogle(token) { asked = it; true }
        assertEquals(movableItems, asked)
        assertEquals(AccountOutcome.Merged(movableItems), outcome)
        assertFalse(auth.history.contains(null))

        val google = auth.googleUser.uid
        assertTrue(local.dao.savedLot(google, "lot")!!.needsUpload)
        assertTrue(local.dao.parkedCarRow(google)!!.needsUpload)
        val reports = local.dao.ownReportRows(google).associateBy { it.id }
        assertEquals(setOf("pending", "recent"), reports.keys)
        assertTrue(reports.values.all { it.syncState == ReportSyncState.PENDING && it.previousOwnerUid == guest.uid })
        assertEquals(ReportSyncState.SYNCED, local.dao.ownReport("old")!!.syncState)
        assertEquals(guest.uid, local.dao.ownReport("old")!!.ownerUid)
        assertTrue(guest.uid in remote.deletedUids)
    }

    @Test
    fun `moved reports are re-created under the Google uid with the same id`() = runBlocking {
        guestHasItems()
        accounts.signInWithGoogle(token) { true }
        SyncEngine(local, remote, policy, { now }).syncOnce(auth.googleUser.uid)
        assertEquals(auth.googleUser.uid, remote.reports["recent"]!!.first)
        assertEquals(auth.googleUser.uid, remote.reports["pending"]!!.first)
        assertEquals(auth.googleUser.uid, remote.savedLots[auth.googleUser.uid to "lot"]!!.ownerUid)
    }

    @Test
    fun `cancelling the merge prompt changes nothing`() = runBlocking {
        guestHasItems()
        assertEquals(AccountOutcome.Cancelled, accounts.signInWithGoogle(token) { false })
        assertEquals(guest, auth.currentUser())
        assertEquals(1, local.dao.savedLotRows(guest.uid).size)
        assertTrue(remote.deletedUids.isEmpty())
    }

    @Test
    fun `a flush that times out stops the move with nothing changed`() = runBlocking {
        guestHasItems()
        remote.flushResult = FlushResult.TIMED_OUT
        assertEquals(AccountOutcome.ServerUnreachable, accounts.signInWithGoogle(token) { true })
        assertEquals(guest, auth.currentUser())
        assertEquals(1, local.dao.savedLotRows(guest.uid).size)
        assertTrue(remote.deletedUids.isEmpty())
    }

    @Test
    fun `a failed cloud delete gives every item back to the guest, still needing upload`() = runBlocking {
        guestHasItems()
        remote.deleteFails = true
        assertEquals(AccountOutcome.MoveFailed, accounts.signInWithGoogle(token) { true })
        assertEquals(guest, auth.currentUser())
        assertTrue(local.dao.savedLot(guest.uid, "lot")!!.needsUpload)
        assertTrue(local.dao.savedLotRows(LOCAL_DEVICE_OWNER).isEmpty())
        assertEquals(2, local.dao.ownReportRows(guest.uid).count { it.syncState == ReportSyncState.PENDING })
    }

    @Test
    fun `a failed Google sign-in gives every item back and the worker re-uploads them`() = runBlocking {
        guestHasItems()
        auth.signInFails = true
        assertEquals(AccountOutcome.SignInFailedItemsKept, accounts.signInWithGoogle(token) { true })
        assertEquals(guest, auth.currentUser())
        SyncEngine(local, remote, policy, { now }).syncOnce(guest.uid)
        assertTrue((guest.uid to "lot") in remote.savedLots)
        assertEquals(guest.uid, remote.reports["recent"]!!.first)
    }

    @Test
    fun `a kill after parking the rows loses nothing, the next account adopts them`() = runBlocking {
        guestHasItems()
        local.parkGuestRowsForMove(guest.uid, now, policy.validityWindowMs)
        local.adoptDeviceRows("new-guest")
        assertTrue(local.dao.savedLot("new-guest", "lot")!!.needsUpload)
        assertTrue(local.dao.parkedCarRow("new-guest")!!.needsUpload)
        assertEquals(2, local.dao.ownReportRows("new-guest").size)
    }

    @Test
    fun `sign-out with unsynced changes asks, then keeps only those rows`() = runBlocking {
        val google = auth.googleUser
        val signedIn = FakeAuthGateway(google)
        val manager = AccountManager(signedIn, local, remote, sync, prefs, online, policy, { now }, scope)
        local.saveLot(google.uid, "synced", "Park", "Lot", "Region", "", now)
        local.dao.markSavedLotSynced(google.uid, "synced", 1)
        local.saveLot(google.uid, "pending", "Park", "Lot", "Region", "", now)

        assertEquals(AccountOutcome.Cancelled, manager.signOut { false })
        assertEquals(google, signedIn.currentUser())

        var asked = 0
        assertEquals(AccountOutcome.SignedOut(1), manager.signOut { asked = it; true })
        assertEquals(1, asked)
        assertEquals(listOf("pending"), local.dao.savedLotRows(google.uid).map { it.lotId })
        assertEquals("each attempt syncs first, the cancelled one too", 2, sync.uploads)
    }

    @Test
    fun `sign-out goes ahead when the flush times out`() = runBlocking {
        val signedIn = FakeAuthGateway(auth.googleUser)
        val manager = AccountManager(signedIn, local, remote, sync, prefs, online, policy, { now }, scope)
        remote.flushResult = FlushResult.TIMED_OUT
        assertEquals(AccountOutcome.SignedOut(0), manager.signOut { error("nothing pending") })
    }

    @Test
    fun `a guest cannot sign out`() = runBlocking {
        assertEquals(AccountOutcome.NotAllowed, accounts.signOut { true })
    }

    @Test
    fun `a guest deletes everything locally and in the cloud`() = runBlocking {
        guestHasItems()
        assertEquals(AccountOutcome.Deleted, accounts.deleteMyData(reauthToken = null))
        assertTrue(local.dao.savedLotRows(guest.uid).isEmpty())
        assertTrue(local.dao.ownReportRows(guest.uid).isEmpty())
        assertTrue(remote.savedLots.isEmpty() && remote.reports.isEmpty())
        assertEquals(1, auth.deletedUsers)
        assertNull(prefs.deletionPendingUid)
    }

    @Test
    fun `a Google account without a fresh credential deletes nothing`() = runBlocking {
        val signedIn = FakeAuthGateway(auth.googleUser)
        val manager = AccountManager(signedIn, local, remote, sync, prefs, online, policy, { now }, scope)
        local.saveLot(auth.googleUser.uid, "lot", "Park", "Lot", "Region", "", now)
        assertEquals(AccountOutcome.Cancelled, manager.deleteMyData(reauthToken = null))
        signedIn.reauthFails = true
        assertTrue(manager.deleteMyData(token) is AccountOutcome.Failed)
        assertEquals(1, local.dao.savedLotRows(auth.googleUser.uid).size)
    }

    @Test
    fun `delete stops on a flush timeout with nothing deleted`() = runBlocking {
        guestHasItems()
        remote.flushResult = FlushResult.TIMED_OUT
        assertEquals(AccountOutcome.ServerUnreachable, accounts.deleteMyData(null))
        assertEquals(1, local.dao.savedLotRows(guest.uid).size)
        assertNull(prefs.deletionPendingUid)
    }

    @Test
    fun `an unfinished delete keeps the flag, blocks the account, and retry finishes it`() = runBlocking {
        guestHasItems()
        remote.deleteFails = true
        assertEquals(AccountOutcome.DeleteIncomplete, accounts.deleteMyData(null))
        assertEquals(guest.uid, prefs.deletionPendingUid)
        assertTrue(local.dao.savedLotRows(guest.uid).isEmpty())
        assertNull(accounts.activeUid.value)

        remote.deleteFails = false
        auth.deleteFails = true
        assertEquals(AccountOutcome.AccountRemovalFailed, accounts.retryDeletion(null))
        assertEquals(guest.uid, auth.currentUser()!!.uid)
        assertEquals(guest.uid, prefs.deletionPendingUid)

        auth.deleteFails = false
        assertEquals(AccountOutcome.Deleted, accounts.retryDeletion(null))
        assertNull(prefs.deletionPendingUid)
        assertNull(accounts.deletionPending.value)
    }
}
