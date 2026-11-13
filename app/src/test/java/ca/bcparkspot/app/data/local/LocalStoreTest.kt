package ca.bcparkspot.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import ca.bcparkspot.app.data.local.entity.LOCAL_DEVICE_OWNER
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.data.local.entity.needsUpload
import ca.bcparkspot.app.testutil.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalStoreTest {
    private val me = "me"
    private val step = 1_000L
    private val now = 1_700_000_000_000L

    private lateinit var db: AppDatabase
    private lateinit var local: LocalStore

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        local = LocalStore(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun save(owner: String, lotId: String = "lot", at: Long = now) =
        local.saveLot(owner, lotId, "Park", "Lot", "Region", "", at)

    @Test
    fun `unsaving a lot the cloud never saw removes it, a synced one leaves a tombstone`() = runBlocking {
        save(me, "never-synced")
        local.unsaveLot(me, "never-synced", now)
        assertNull(local.dao.savedLot(me, "never-synced"))

        save(me, "synced")
        local.dao.markSavedLotSynced(me, "synced", 1)
        local.unsaveLot(me, "synced", now + step)
        val tomb = local.dao.savedLot(me, "synced")!!
        assertTrue(tomb.deleted && tomb.needsUpload)
        assertTrue(local.savedLots(me).first().isEmpty())
    }

    @Test
    fun `re-saving bumps the version so it uploads again`() = runBlocking {
        save(me)
        local.dao.markSavedLotSynced(me, "lot", 1)
        save(me, at = now + step)
        val row = local.dao.savedLot(me, "lot")!!
        assertEquals(2L, row.version)
        assertTrue(row.needsUpload)
    }

    @Test
    fun `clearing the car follows the same tombstone rule`() = runBlocking {
        val car = ParkedCarEntity(me, "Park", "Lot", 1.0, 1.0, timestamp = now, updatedAt = now)
        local.pinCar(me, car)
        local.clearCar(me, now)
        assertNull(local.dao.parkedCarRow(me))

        local.pinCar(me, car)
        local.dao.markParkedCarSynced(me, 1)
        local.clearCar(me, now + step)
        assertTrue(local.dao.parkedCarRow(me)!!.deleted)
        assertNull(local.parkedCar(me).first())
    }

    @Test
    fun `adoption moves device rows into the account`() = runBlocking {
        save(LOCAL_DEVICE_OWNER)
        local.addReport(OwnReportEntity("r", LOCAL_DEVICE_OWNER, "lot", "Park", "BC Hiker", "FULL", "packed", null, now))
        local.adoptDeviceRows(me)
        assertTrue(local.dao.savedLot(me, "lot")!!.needsUpload)
        assertEquals(me, local.dao.ownReport("r")!!.ownerUid)
        assertTrue(local.dao.savedLotRows(LOCAL_DEVICE_OWNER).isEmpty())
    }

    @Test
    fun `adoption clash with a hidden same-key row keeps the newer one and raises no key error`() = runBlocking {
        save(me, at = now)
        local.dao.markSavedLotSynced(me, "lot", 1)
        local.unsaveLot(me, "lot", now + step)
        save(LOCAL_DEVICE_OWNER, at = now + step * 2)

        local.adoptDeviceRows(me)
        val row = local.dao.savedLot(me, "lot")!!
        assertFalse(row.deleted)
        assertTrue(row.needsUpload)
        assertEquals(1, local.dao.savedLotRows(me).size)
    }

    @Test
    fun `adoption clash where the account's row is newer keeps it`() = runBlocking {
        save(LOCAL_DEVICE_OWNER, at = now)
        save(me, at = now + step)
        local.dao.markSavedLotSynced(me, "lot", 1)
        local.unsaveLot(me, "lot", now + step * 2)
        local.adoptDeviceRows(me)
        assertTrue(local.dao.savedLot(me, "lot")!!.deleted)
    }

    @Test
    fun `listener records apply only when newer, from the server, and nothing is pending`() = runBlocking {
        val cloud = SavedLotEntity(me, "lot", "Park", "Cloud", "Region", "", now, now + step)
        local.applyCloudSavedLot(me, cloud, fromCache = true)
        assertNull(local.dao.savedLot(me, "lot"))

        local.applyCloudSavedLot(me, cloud, fromCache = false)
        assertEquals("Cloud", local.dao.savedLot(me, "lot")!!.lotName)
        assertFalse(local.dao.savedLot(me, "lot")!!.needsUpload)

        save(me, at = now + step * 2)
        local.applyCloudSavedLot(me, cloud.copy(lotName = "Newer", updatedAt = now + step * 3), fromCache = false)
        assertEquals("Lot", local.dao.savedLot(me, "lot")!!.lotName)
    }

    @Test
    fun `sign-out keeps only rows that still need upload`() = runBlocking {
        save(me, "synced")
        local.dao.markSavedLotSynced(me, "synced", 1)
        save(me, "pending")
        local.addReport(OwnReportEntity("sent", me, "lot", "Park", "BC Hiker", "FULL", "packed", null, now))
        local.dao.setReportState("sent", ReportSyncState.SYNCED, null)
        local.addReport(OwnReportEntity("queued", me, "lot", "Park", "BC Hiker", "FULL", "packed", null, now))

        local.dropSettledRowsOf(me)
        assertEquals(listOf("pending"), local.dao.savedLotRows(me).map { it.lotId })
        assertEquals(listOf("queued"), local.dao.ownReportRows(me).map { it.id })
    }

    @Test
    fun `pending count covers lots, car and reports but not rows the server refused`() = runBlocking {
        save(me, "a")
        save(me, "b")
        local.pinCar(me, ParkedCarEntity(me, "Park", "Lot", 1.0, 1.0, timestamp = now, updatedAt = now))
        local.addReport(OwnReportEntity("r", me, "lot", "Park", "BC Hiker", "FULL", "packed", null, now))
        assertEquals(4, local.pendingChangeCount(me).first())
        local.dao.setSavedLotError(me, "b", "refused")
        assertEquals(3, local.pendingChangeCount(me).first())
        assertEquals(1, local.syncErrorCount(me).first())
    }

    @Test
    fun `delete everything removes only that account's rows`() = runBlocking {
        save(me)
        save("other")
        local.deleteEverythingOf(me)
        assertTrue(local.dao.savedLotRows(me).isEmpty())
        assertEquals(1, local.dao.savedLotRows("other").size)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test"
    private val savedAt = 1_700_000_000_000L
    private val schemaVersionBefore = 2
    private val schemaVersionAfter = 3

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun `version 2 rows move to the device owner, needing upload`() {
        helper.createDatabase(dbName, schemaVersionBefore).use { db ->
            db.execSQL(
                "INSERT INTO saved_lots (lotId, parkName, lotName, region, savedAtTimestamp, userNotes) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf("lot-1", "Park", "Lot", "Region", savedAt, "notes")
            )
            db.execSQL(
                "INSERT INTO parked_car (id, parkName, lotName, latitude, longitude, stallNumber, notes, timestamp, isExactGpsLocation) " +
                    "VALUES (1, 'Park', 'Lot', 49.1, -123.2, 'B4', '', ?, 1)",
                arrayOf(savedAt)
            )
        }
        helper.runMigrationsAndValidate(dbName, schemaVersionAfter, true, AppDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT ownerUid, lotId, updatedAt, deleted, version, syncedVersion FROM saved_lots").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(LOCAL_DEVICE_OWNER, c.getString(0))
                assertEquals("lot-1", c.getString(1))
                assertEquals(savedAt, c.getLong(2))
                assertEquals(0, c.getInt(3))
                assertEquals(1L, c.getLong(4))
                assertEquals(0L, c.getLong(5))
            }
            db.query("SELECT ownerUid, isExactGpsLocation, version, syncedVersion FROM parked_car").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(LOCAL_DEVICE_OWNER, c.getString(0))
                assertEquals(1, c.getInt(1))
                assertEquals(1L, c.getLong(2))
                assertEquals(0L, c.getLong(3))
            }
        }
    }
}
