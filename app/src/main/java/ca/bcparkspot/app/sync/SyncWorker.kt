package ca.bcparkspot.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.bcparkspot.app.AppGraph
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Lets account flows stop uploads and wait for them, without knowing about WorkManager. */
interface SyncControl {
    fun request()
    suspend fun cancelAndAwait()
    /** Cancels the worker and runs one pass for [uid] directly, giving up after [timeoutMs]. */
    suspend fun uploadNow(uid: String, timeoutMs: Long)
}

/** Every engine pass runs under this lock, so cancel-and-await can be sure no pass is still writing. */
object SyncLock {
    val mutex = Mutex()
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = AppGraph.from(applicationContext)
        val uid = graph.accounts.activeUid.value ?: return Result.success()
        val engine = graph.syncEngine
        val outcome = SyncLock.mutex.withLock { engine.syncOnce(uid) }
        return if (outcome == SyncRunResult.RETRY) Result.retry() else Result.success()
    }
}

class WorkManagerSyncControl(private val context: Context, private val engine: () -> SyncEngine?) : SyncControl {
    private val workManager get() = WorkManager.getInstance(context)

    private fun request(policy: ExistingWorkPolicy) {
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, SyncPolicy.BACKOFF_INITIAL_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, policy, work)
    }

    override fun request() = request(ExistingWorkPolicy.APPEND_OR_REPLACE)

    override suspend fun cancelAndAwait() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        SyncLock.mutex.withLock { }
    }

    override suspend fun uploadNow(uid: String, timeoutMs: Long) {
        cancelAndAwait()
        val pass = engine() ?: return
        withTimeoutOrNull(timeoutMs) { SyncLock.mutex.withLock { pass.syncOnce(uid) } }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "bcparkspot-sync"
    }
}
