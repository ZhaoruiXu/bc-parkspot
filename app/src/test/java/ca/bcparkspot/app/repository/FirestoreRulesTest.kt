package ca.bcparkspot.app.repository

import ca.bcparkspot.app.base.FirestoreEmulatorTestBase
import ca.bcparkspot.app.data.local.entity.CAR_PIN_DOCUMENT_ID
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.data.remote.CloudFields
import ca.bcparkspot.app.data.remote.FirestoreRemoteStore
import ca.bcparkspot.app.data.remote.toCloud
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.sync.FlushResult
import ca.bcparkspot.app.sync.RemoteErrorKind
import ca.bcparkspot.app.sync.RemoteWriteException
import ca.bcparkspot.app.sync.SyncPolicy
import ca.bcparkspot.app.sync.UpsertResult
import ca.bcparkspot.app.sync.reportQueryLowerBound
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

/** The rules (4.21) and the real FirestoreRemoteStore, against the Firestore and Auth emulators. */
class FirestoreRulesTest : FirestoreEmulatorTestBase() {
    private val policy = ReportPolicies.DEFAULT
    private val step = TimeUnit.MINUTES.toMillis(1)
    private val remote get() = FirestoreRemoteStore(firestore)

    private fun report(uid: String, observedAt: Long = System.currentTimeMillis(), id: String = UUID.randomUUID().toString()) =
        OwnReportEntity(id, uid, "lot", "Park", "BC Hiker", "AVAILABLE", "plenty of room", 12, observedAt)

    private fun savedLot(uid: String, updatedAt: Long = System.currentTimeMillis(), deleted: Boolean = false) =
        SavedLotEntity(uid, "lot", "Park", "Lot", "Region", "", updatedAt, updatedAt, deleted)

    private suspend fun assertDenied(block: suspend () -> Unit) {
        try {
            block()
            fail("expected PERMISSION_DENIED")
        } catch (e: RemoteWriteException) {
            assertEquals(RemoteErrorKind.PERMISSION_DENIED, e.kind)
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun `flush reports FLUSHED once the server has every write, with or without writes pending`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        assertEquals(FlushResult.FLUSHED, remote.flush(SyncPolicy.flushTimeoutMs))
        remote.upsertSavedLot(me, savedLot(me))
        assertEquals(FlushResult.FLUSHED, remote.flush(SyncPolicy.flushTimeoutMs))
    }

    @Test
    fun `a report is create-only, and a re-send is refused as an update`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        val row = report(me)
        remote.createReport(me, row)
        assertEquals(me, remote.reportAuthor(row.id))
        assertDenied { remote.createReport(me, row) }
    }

    @Test
    fun `a report must carry its author's uid`() = runBlocking {
        signInTestUser("alice@example.com")
        assertDenied { remote.createReport("someone-else", report("someone-else")) }
    }

    @Test
    fun `observedAt must be inside the validity window and not in the future`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        val now = System.currentTimeMillis()
        assertDenied { remote.createReport(me, report(me, observedAt = now - policy.validityWindowMs - step)) }
        assertDenied { remote.createReport(me, report(me, observedAt = now + policy.clockSkewAllowanceMs + step)) }
        remote.createReport(me, report(me, observedAt = now - policy.validityWindowMs + step))
    }

    @Test
    fun `a report id must be a UUID`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        assertDenied { remote.createReport(me, report(me, id = "not-a-uuid")) }
    }

    @Test
    fun `saved lots are owner-only, last-writer-wins, and tombstones upload`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        val now = System.currentTimeMillis()
        assertEquals(UpsertResult.Written, remote.upsertSavedLot(me, savedLot(me, now)))
        assertEquals(UpsertResult.Written, remote.upsertSavedLot(me, savedLot(me, now + step, deleted = true)))
        assertTrue(remote.upsertSavedLot(me, savedLot(me, now)) is UpsertResult.CloudNewer)
        assertDenied { remote.upsertSavedLot("bob", savedLot("bob")) }
    }

    @Test
    fun `an offline change from long ago still uploads, a future one does not`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        val longAgo = System.currentTimeMillis() - SyncPolicy.tombstoneRetentionMs
        assertEquals(UpsertResult.Written, remote.upsertSavedLot(me, savedLot(me, longAgo)))
        val future = System.currentTimeMillis() + policy.clockSkewAllowanceMs + step
        assertDenied { remote.upsertSavedLot(me, savedLot(me, future)) }
    }

    @Test
    fun `the car pin lives only at the current document`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        val now = System.currentTimeMillis()
        val car = ParkedCarEntity(me, "Park", "Lot", 49.1, -123.2, timestamp = now, updatedAt = now)
        assertEquals(UpsertResult.Written, remote.upsertParkedCar(me, car))
        assertDenied {
            firestore.collection(CloudFields.USERS).document(me).collection(CloudFields.PARKED_VEHICLES)
                .document("second").set(car.toCloud()).await()
        }
        assertEquals(CAR_PIN_DOCUMENT_ID, firestore.collection(CloudFields.USERS).document(me)
            .collection(CloudFields.PARKED_VEHICLES).get(Source.SERVER).await().documents.single().id)
    }

    @Test
    fun `another user's old report cannot be read, a recent one can`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        val old = UUID.randomUUID().toString()
        val recent = UUID.randomUUID().toString()
        val oldAt = Instant.now().minusMillis(policy.validityWindowMs * 2)
        seedReport(old, "bob", oldAt.toString())
        seedReport(recent, "bob", Instant.now().toString())

        assertEquals("bob", remote.reportAuthor(recent))
        assertDenied { remote.reportAuthor(old) }
        assertDenied { firestore.collection(CloudFields.REPORTS).get(Source.SERVER).await() }
        assertTrue(me.isNotEmpty())
    }

    @Test
    fun `the bucketed feed query is allowed and leaves out old reports`() = runBlocking {
        signInTestUser("alice@example.com")
        seedReport(UUID.randomUUID().toString(), "bob", Instant.now().toString())
        seedReport(UUID.randomUUID().toString(), "bob", Instant.now().minusMillis(policy.validityWindowMs * 2).toString())
        val lowerBound = reportQueryLowerBound(System.currentTimeMillis(), policy)
        val snap = firestore.collection(CloudFields.REPORTS)
            .whereGreaterThanOrEqualTo(CloudFields.OBSERVED_AT, Timestamp(Date(lowerBound)))
            .get(Source.SERVER).await()
        assertEquals(1, snap.size())
    }

    @Test
    fun `my own reports of any age can be listed and deleted`() = runBlocking {
        val me = signInTestUser("alice@example.com")
        seedReport(UUID.randomUUID().toString(), me, Instant.now().minusMillis(policy.validityWindowMs * 2).toString())
        remote.createReport(me, report(me))
        remote.upsertSavedLot(me, savedLot(me))

        val mine = firestore.collection(CloudFields.REPORTS).whereEqualTo(CloudFields.USER_ID, me).get(Source.SERVER).await()
        assertEquals(2, mine.size())

        remote.deleteCloudDataOf(me)
        assertEquals(0, firestore.collection(CloudFields.REPORTS).whereEqualTo(CloudFields.USER_ID, me).get(Source.SERVER).await().size())
        assertEquals(0, firestore.collection(CloudFields.USERS).document(me).collection(CloudFields.SAVED_LOTS).get(Source.SERVER).await().size())
    }

    @Test
    fun `signed-out clients read nothing`() = runBlocking {
        seedReport(UUID.randomUUID().toString(), "bob", Instant.now().toString())
        auth.signOut()
        assertDenied {
            firestore.collection(CloudFields.REPORTS)
                .whereGreaterThanOrEqualTo(CloudFields.OBSERVED_AT, Timestamp(Date(reportQueryLowerBound(System.currentTimeMillis(), policy))))
                .get(Source.SERVER).await()
        }
    }
}
