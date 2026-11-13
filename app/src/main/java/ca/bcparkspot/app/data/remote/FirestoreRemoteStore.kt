package ca.bcparkspot.app.data.remote

import ca.bcparkspot.app.data.local.entity.CAR_PIN_DOCUMENT_ID
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.sync.FlushResult
import ca.bcparkspot.app.sync.RemoteErrorKind
import ca.bcparkspot.app.sync.RemoteStore
import ca.bcparkspot.app.sync.RemoteWriteException
import ca.bcparkspot.app.sync.UpsertResult
import ca.bcparkspot.app.sync.UploadWinner
import ca.bcparkspot.app.sync.uploadWinner
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Firestore's batch write limit. */
private const val MAX_WRITES_PER_BATCH = 500

fun remoteErrorKind(code: Code): RemoteErrorKind = when (code) {
    Code.PERMISSION_DENIED -> RemoteErrorKind.PERMISSION_DENIED
    Code.INVALID_ARGUMENT, Code.FAILED_PRECONDITION, Code.OUT_OF_RANGE, Code.ALREADY_EXISTS, Code.NOT_FOUND -> RemoteErrorKind.INVALID
    else -> RemoteErrorKind.RETRYABLE
}

private const val LOG_TAG = "BCParkSpotSync"

class FirestoreRemoteStore(private val firestore: FirebaseFirestore) : RemoteStore {

    private fun user(uid: String) = firestore.collection(CloudFields.USERS).document(uid)
    private fun savedLotDoc(uid: String, lotId: String) = user(uid).collection(CloudFields.SAVED_LOTS).document(lotId)
    private fun carDoc(uid: String) = user(uid).collection(CloudFields.PARKED_VEHICLES).document(CAR_PIN_DOCUMENT_ID)
    private fun reports() = firestore.collection(CloudFields.REPORTS)

    private suspend fun <T> mapped(block: suspend () -> T): T = try {
        block()
    } catch (e: FirebaseFirestoreException) {
        throw RemoteWriteException(remoteErrorKind(e.code), e.code.name)
    }

    private suspend fun <T> lastWriterWins(
        ref: DocumentReference,
        localUpdatedAt: Long,
        payload: Map<String, Any>,
        parse: (Map<String, Any?>) -> T?
    ): UpsertResult<T> = mapped {
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val cloud = snap.data?.let(parse)
            val cloudUpdatedAt = snap.getTimestamp(CloudFields.UPDATED_AT)?.toDate()?.time
            if (cloud != null && uploadWinner(localUpdatedAt, cloudUpdatedAt) == UploadWinner.CLOUD) {
                UpsertResult.CloudNewer(cloud)
            } else {
                tx.set(ref, payload)
                UpsertResult.Written
            }
        }.await()
    }

    override suspend fun upsertSavedLot(uid: String, row: SavedLotEntity): UpsertResult<SavedLotEntity> =
        lastWriterWins(savedLotDoc(uid, row.lotId), row.updatedAt, row.toCloud()) { savedLotFromCloud(uid, it) }

    override suspend fun upsertParkedCar(uid: String, row: ParkedCarEntity): UpsertResult<ParkedCarEntity> =
        lastWriterWins(carDoc(uid), row.updatedAt, row.toCloud()) { parkedCarFromCloud(uid, it) }

    override suspend fun createReport(uid: String, row: OwnReportEntity) = mapped {
        reports().document(row.id).set(row.toCloud(uid) + (CloudFields.CREATED_AT to FieldValue.serverTimestamp())).await()
        Unit
    }

    override suspend fun reportAuthor(reportId: String): String? = mapped {
        reports().document(reportId).get(Source.SERVER).await().getString(CloudFields.USER_ID)
    }

    override suspend fun deleteCloudDataOf(uid: String) = mapped {
        val refs = user(uid).collection(CloudFields.SAVED_LOTS).get(Source.SERVER).await().documents.map { it.reference } +
            user(uid).collection(CloudFields.PARKED_VEHICLES).get(Source.SERVER).await().documents.map { it.reference } +
            reports().whereEqualTo(CloudFields.USER_ID, uid).get(Source.SERVER).await().documents.map { it.reference }
        refs.chunked(MAX_WRITES_PER_BATCH).forEach { chunk ->
            firestore.batch().apply { chunk.forEach { delete(it) } }.commit().await()
        }
    }

    override suspend fun flush(timeoutMs: Long): FlushResult {
        Log.d(LOG_TAG, "flush: waiting for pending writes")
        // waitForPendingWrites completes with null, so the result must not come from await() itself.
        val result = withTimeoutOrNull(timeoutMs) {
            firestore.waitForPendingWrites().await()
            FlushResult.FLUSHED
        } ?: FlushResult.TIMED_OUT
        Log.d(LOG_TAG, "flush: $result")
        return result
    }
}
