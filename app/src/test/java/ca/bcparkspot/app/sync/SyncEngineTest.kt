package ca.bcparkspot.app.sync

import ca.bcparkspot.app.data.local.AppDatabase
import ca.bcparkspot.app.data.local.LocalStore
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.testutil.FakeRemoteStore
import ca.bcparkspot.app.testutil.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncEngineTest {
    private val uid = "me"
    private val policy = ReportPolicies.DEFAULT
    private val step = 1_000L
    private var now = policy.validityWindowMs * 100

    private lateinit var db: AppDatabase
    private lateinit var local: LocalStore
    private lateinit var remote: FakeRemoteStore
    private lateinit var engine: SyncEngine
    private val completed = mutableListOf<Long>()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        local = LocalStore(db)
        remote = FakeRemoteStore()
        engine = SyncEngine(local, remote, policy, { now }, onCompleted = { completed += it })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun save(lotId: String = "lot") = local.saveLot(uid, lotId, "Park", "Lot", "Region", "", now)
    private suspend fun lot(lotId: String = "lot", owner: String = uid) = local.dao.savedLot(owner, lotId)
    private fun report(id: String = "r1", observedAt: Long = now, owner: String = uid, previous: String? = null) =
        OwnReportEntity(id, owner, "lot", "Park", "BC Hiker", "AVAILABLE", "lots of room", null, observedAt, previousOwnerUid = previous)

    @Test
    fun `a saved lot uploads and is marked synced`() = runBlocking {
        save()
        assertEquals(SyncRunResult.DONE, engine.syncOnce(uid))
        val row = lot()!!
        assertEquals(row.version, row.syncedVersion)
        assertTrue((uid to "lot") in remote.savedLots)
        assertEquals(listOf(now), completed)
    }

    @Test
    fun `a change made during the upload stays pending and goes next run`() = runBlocking {
        save()
        remote.duringUpload = { now += step; save(); remote.duringUpload = {} }
        engine.syncOnce(uid)
        val row = lot()!!
        assertTrue(row.version > row.syncedVersion)
        engine.syncOnce(uid)
        assertEquals(lot()!!.version, lot()!!.syncedVersion)
        assertEquals(now, remote.savedLots[uid to "lot"]!!.updatedAt)
    }

    @Test
    fun `offline unsave wins over an older cloud save`() = runBlocking {
        save()
        engine.syncOnce(uid)
        now += step
        local.unsaveLot(uid, "lot", now)
        engine.syncOnce(uid)
        assertTrue(remote.savedLots[uid to "lot"]!!.deleted)
        assertTrue(local.savedLots(uid).first().isEmpty())
    }

    @Test
    fun `a long-offline older save does not revive a lot unsaved elsewhere`() = runBlocking {
        val oldSave = now
        local.saveLot(uid, "lot", "Park", "Lot", "Region", "", oldSave)
        remote.savedLots[uid to "lot"] = SavedLotEntity(uid, "lot", "Park", "Lot", "Region", "", oldSave, oldSave + step, deleted = true)
        engine.syncOnce(uid)
        val row = lot()!!
        assertTrue(row.deleted)
        assertEquals(row.version, row.syncedVersion)
        assertTrue(local.savedLots(uid).first().isEmpty())
    }

    @Test
    fun `a newer cloud pin beats an older pending pin, and the reverse`() = runBlocking {
        val older = ParkedCarEntity(uid, "Park", "Old", 1.0, 1.0, timestamp = now, updatedAt = now)
        val newer = older.copy(lotName = "New", updatedAt = now + step)

        local.pinCar(uid, older)
        remote.cars[uid] = newer
        engine.syncOnce(uid)
        assertEquals("New", local.dao.parkedCarRow(uid)!!.lotName)

        local.pinCar(uid, newer.copy(lotName = "Newest", updatedAt = now + step * 2))
        engine.syncOnce(uid)
        assertEquals("Newest", remote.cars[uid]!!.lotName)
    }

    @Test
    fun `a retryable failure keeps the row pending and asks for a retry`() = runBlocking {
        save()
        remote.savedLotError = RemoteErrorKind.RETRYABLE
        assertEquals(SyncRunResult.RETRY, engine.syncOnce(uid))
        assertEquals(0L, lot()!!.syncedVersion)
        assertTrue(completed.isEmpty())
    }

    @Test
    fun `a permanent refusal marks the row not synced and stops retrying it`() = runBlocking {
        save()
        remote.savedLotError = RemoteErrorKind.INVALID
        assertEquals(SyncRunResult.DONE, engine.syncOnce(uid))
        assertEquals(RemoteErrorKind.INVALID.name, lot()!!.syncError)
        assertEquals(1, local.syncErrorCount(uid).first())
        assertEquals(0, local.pendingChangeCount(uid).first())

        remote.savedLotError = null
        engine.syncOnce(uid)
        assertEquals(0L, lot()!!.syncedVersion)

        local.clearSyncErrors(uid)
        engine.syncOnce(uid)
        assertEquals(lot()!!.version, lot()!!.syncedVersion)
    }

    @Test
    fun `only the current account's rows upload`() = runBlocking {
        local.saveLot("someone-else", "lot", "Park", "Lot", "Region", "", now)
        engine.syncOnce(uid)
        assertTrue(remote.savedLots.isEmpty())
    }

    @Test
    fun `a report uploads once and is marked synced`() = runBlocking {
        local.addReport(report())
        engine.syncOnce(uid)
        assertEquals(ReportSyncState.SYNCED, local.dao.ownReport("r1")!!.syncState)
        assertEquals(uid, remote.reports["r1"]!!.first)
        engine.syncOnce(uid)
        assertEquals(1, remote.createCalls)
    }

    @Test
    fun `re-uploading a report the server already holds from us marks it synced`() = runBlocking {
        remote.reports["r1"] = "guest" to report(owner = "guest")
        local.addReport(report(previous = "guest"))
        engine.syncOnce(uid)
        assertEquals(ReportSyncState.SYNCED, local.dao.ownReport("r1")!!.syncState)
    }

    @Test
    fun `a report id held by someone else is rejected`() = runBlocking {
        remote.reports["r1"] = "stranger" to report(owner = "stranger")
        local.addReport(report())
        engine.syncOnce(uid)
        assertEquals(ReportSyncState.REJECTED, local.dao.ownReport("r1")!!.syncState)
    }

    @Test
    fun `a pending report past the window expires without uploading`() = runBlocking {
        local.addReport(report(observedAt = now - policy.validityWindowMs - step))
        engine.syncOnce(uid)
        assertEquals(ReportSyncState.EXPIRED, local.dao.ownReport("r1")!!.syncState)
        assertEquals(0, remote.createCalls)
    }

    @Test
    fun `invalid reports are rejected and quota errors retry`() = runBlocking {
        local.addReport(report("bad"))
        remote.reportError = RemoteErrorKind.INVALID
        engine.syncOnce(uid)
        assertEquals(ReportSyncState.REJECTED, local.dao.ownReport("bad")!!.syncState)

        local.addReport(report("quota"))
        remote.reportError = RemoteErrorKind.RETRYABLE
        assertEquals(SyncRunResult.RETRY, engine.syncOnce(uid))
        assertEquals(ReportSyncState.PENDING, local.dao.ownReport("quota")!!.syncState)
    }

    @Test
    fun `a synced tombstone is purged only after the retention period`() = runBlocking {
        save()
        engine.syncOnce(uid)
        local.unsaveLot(uid, "lot", now)
        engine.syncOnce(uid)
        assertTrue(lot()!!.deleted)

        now += SyncPolicy.tombstoneRetentionMs + step
        engine.syncOnce(uid)
        assertNull(lot())
    }

    @Test
    fun `an unsynced tombstone is never purged`() = runBlocking {
        save()
        engine.syncOnce(uid)
        local.unsaveLot(uid, "lot", now)
        remote.savedLotError = RemoteErrorKind.RETRYABLE
        now += SyncPolicy.tombstoneRetentionMs + step
        engine.syncOnce(uid)
        assertTrue(lot()!!.deleted)
    }
}
