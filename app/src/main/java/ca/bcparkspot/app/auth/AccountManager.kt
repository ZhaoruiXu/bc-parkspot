package ca.bcparkspot.app.auth

import ca.bcparkspot.app.data.local.DevicePrefs
import ca.bcparkspot.app.data.local.LocalStore
import ca.bcparkspot.app.data.local.entity.LOCAL_DEVICE_OWNER
import ca.bcparkspot.app.domain.ReportPolicy
import ca.bcparkspot.app.sync.FlushResult
import ca.bcparkspot.app.sync.RemoteStore
import ca.bcparkspot.app.sync.SyncControl
import ca.bcparkspot.app.sync.SyncPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

enum class AccountBusy { SIGNING_IN, SIGNING_OUT, DELETING }

sealed interface AccountOutcome {
    data object Linked : AccountOutcome
    data class Merged(val items: Int) : AccountOutcome
    data object Cancelled : AccountOutcome
    data object NotAllowed : AccountOutcome
    data object ServerUnreachable : AccountOutcome
    data object MoveFailed : AccountOutcome
    data object SignInFailedItemsKept : AccountOutcome
    data class Failed(val reason: String?) : AccountOutcome
    data class SignedOut(val keptChanges: Int) : AccountOutcome
    data object Deleted : AccountOutcome
    data object DeleteIncomplete : AccountOutcome
    data object AccountRemovalFailed : AccountOutcome
}

object AnonymousSignInPolicy {
    const val FIRST_RETRY_SECONDS = 5L
    const val MAX_RETRY_SECONDS = 300L
    const val BACKOFF_FACTOR = 2
}

/**
 * Owns who the current account is and every flow that changes it (4.5, 4.5b, 4.5c, 4.15 to 4.19).
 * Account flows hold one lock; while it is held, adoption, the cloud listeners, anonymous sign-in and
 * the worker stay off, and they resume in that order when it is released.
 */
class AccountManager(
    private val auth: AuthGateway,
    private val local: LocalStore,
    private val remote: RemoteStore,
    private val sync: SyncControl,
    private val prefs: DevicePrefs,
    private val online: StateFlow<Boolean>,
    private val policy: ReportPolicy,
    private val clock: () -> Long,
    private val scope: CoroutineScope
) {
    private val lock = Mutex()
    private val _user = MutableStateFlow(auth.currentUser())
    private val _busy = MutableStateFlow<AccountBusy?>(null)
    private val _deletionPending = MutableStateFlow(prefs.deletionPendingUid)

    val user: StateFlow<AuthUser?> = _user.asStateFlow()
    val busy: StateFlow<AccountBusy?> = _busy.asStateFlow()
    val deletionPending: StateFlow<String?> = _deletionPending.asStateFlow()

    /** The account whose rows the UI shows. */
    val ownerUid: StateFlow<String> = _user.map { it?.uid ?: LOCAL_DEVICE_OWNER }
        .stateIn(scope, SharingStarted.Eagerly, auth.currentUser()?.uid ?: LOCAL_DEVICE_OWNER)

    /** The account the listeners and the worker may act for: none while a flow runs or a deletion is unfinished. */
    val activeUid: StateFlow<String?> = combine(_user, _busy, _deletionPending) { user, busy, deleting ->
        if (busy != null || deleting != null) null else user?.uid
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun start() {
        scope.launch {
            auth.users().collect { user ->
                _user.value = user
                if (_busy.value == null) settle()
            }
        }
        scope.launch {
            online.collect { if (it && _busy.value == null) settle() }
        }
        if (prefs.deletionPendingUid != null) scope.launch { retryDeletion(reauthToken = null) }
    }

    private var anonymousRetry: kotlinx.coroutines.Job? = null

    /** Adoption for a present user, or a silent guest account for an absent one (4.5, 4.5b). */
    private suspend fun settle() {
        if (_deletionPending.value != null) return
        val current = auth.currentUser()
        _user.value = current
        if (current != null) {
            local.adoptDeviceRows(current.uid)
            sync.request()
        } else {
            ensureAnonymous()
        }
    }

    private fun ensureAnonymous() {
        if (anonymousRetry?.isActive == true) return
        anonymousRetry = scope.launch {
            var waitSeconds = AnonymousSignInPolicy.FIRST_RETRY_SECONDS
            while (auth.currentUser() == null && _busy.value == null && _deletionPending.value == null && online.value) {
                try {
                    auth.signInAnonymously()
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    delay(TimeUnit.SECONDS.toMillis(waitSeconds))
                    waitSeconds = minOf(waitSeconds * AnonymousSignInPolicy.BACKOFF_FACTOR, AnonymousSignInPolicy.MAX_RETRY_SECONDS)
                }
            }
        }
    }

    private suspend fun <T> withAccountLock(kind: AccountBusy, block: suspend () -> T): T = lock.withLock {
        anonymousRetry?.cancel()
        _busy.value = kind
        try {
            block()
        } finally {
            val current = auth.currentUser()
            _user.value = current
            if (current != null && _deletionPending.value == null) local.adoptDeviceRows(current.uid)
            _busy.value = null
            if (current != null) sync.request() else if (_deletionPending.value == null) ensureAnonymous()
        }
    }

    /** 4.15 and 4.16. [confirmMerge] asks the user before guest items join an existing Google account. */
    suspend fun signInWithGoogle(idToken: String, confirmMerge: suspend (Int) -> Boolean): AccountOutcome =
        withAccountLock(AccountBusy.SIGNING_IN) {
            val guest = auth.currentUser()
            if (guest != null && !guest.isAnonymous) return@withAccountLock AccountOutcome.NotAllowed
            try {
                if (guest == null) {
                    auth.signInGoogle(idToken)
                    return@withAccountLock AccountOutcome.Linked
                }
                if (auth.linkGoogle(idToken) == LinkOutcome.LINKED) return@withAccountLock AccountOutcome.Linked
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@withAccountLock AccountOutcome.Failed(e.message)
            }
            moveGuestInto(guest.uid, idToken, confirmMerge)
        }

    private suspend fun moveGuestInto(guest: String, idToken: String, confirmMerge: suspend (Int) -> Boolean): AccountOutcome {
        val now = clock()
        val items = local.movableItemCount(guest, now, policy.validityWindowMs)
        if (items > 0) {
            if (!confirmMerge(items)) return AccountOutcome.Cancelled
            sync.cancelAndAwait()
            if (remote.flush(SyncPolicy.flushTimeoutMs) == FlushResult.TIMED_OUT) return AccountOutcome.ServerUnreachable
            local.parkGuestRowsForMove(guest, now, policy.validityWindowMs)
            try {
                remote.deleteCloudDataOf(guest)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                local.returnParkedRowsTo(guest)
                return AccountOutcome.MoveFailed
            }
        }
        return try {
            auth.signInGoogle(idToken)
            if (items > 0) AccountOutcome.Merged(items) else AccountOutcome.Linked
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (items > 0) local.returnParkedRowsTo(guest)
            AccountOutcome.SignInFailedItemsKept
        }
    }

    /** 4.18. [confirmKeep] is asked only when changes have not synced yet. */
    suspend fun signOut(confirmKeep: suspend (Int) -> Boolean): AccountOutcome =
        withAccountLock(AccountBusy.SIGNING_OUT) {
            val current = auth.currentUser()
            if (current == null || current.isAnonymous) return@withAccountLock AccountOutcome.NotAllowed
            sync.uploadNow(current.uid, SyncPolicy.signOutSyncTimeoutMs)
            sync.cancelAndAwait()
            remote.flush(SyncPolicy.flushTimeoutMs)
            val pending = local.pendingChangeCount(current.uid).first()
            if (pending > 0 && !confirmKeep(pending)) return@withAccountLock AccountOutcome.Cancelled
            auth.signOut()
            local.dropSettledRowsOf(current.uid)
            AccountOutcome.SignedOut(pending)
        }

    /** 4.19. A Google account must pass a fresh [reauthToken]; a guest needs none. */
    suspend fun deleteMyData(reauthToken: String?): AccountOutcome =
        withAccountLock(AccountBusy.DELETING) {
            val current = auth.currentUser() ?: return@withAccountLock AccountOutcome.NotAllowed
            if (!current.isAnonymous) {
                if (reauthToken == null) return@withAccountLock AccountOutcome.Cancelled
                try {
                    auth.reauthenticateGoogle(reauthToken)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return@withAccountLock AccountOutcome.Failed(e.message)
                }
            }
            sync.cancelAndAwait()
            if (remote.flush(SyncPolicy.flushTimeoutMs) == FlushResult.TIMED_OUT) return@withAccountLock AccountOutcome.ServerUnreachable
            prefs.deletionPendingUid = current.uid
            _deletionPending.value = current.uid
            finishDeletion(current.uid)
        }

    /** Resumes an unfinished deletion (after a failure or a restart), re-authenticating first when a token is given. */
    suspend fun retryDeletion(reauthToken: String?): AccountOutcome =
        withAccountLock(AccountBusy.DELETING) {
            val uid = prefs.deletionPendingUid ?: return@withAccountLock AccountOutcome.NotAllowed
            if (reauthToken != null) {
                try {
                    auth.reauthenticateGoogle(reauthToken)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return@withAccountLock AccountOutcome.AccountRemovalFailed
                }
            }
            finishDeletion(uid)
        }

    private suspend fun finishDeletion(uid: String): AccountOutcome {
        local.deleteEverythingOf(uid)
        try {
            remote.deleteCloudDataOf(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return AccountOutcome.DeleteIncomplete
        }
        if (auth.currentUser()?.uid == uid) {
            try {
                auth.deleteCurrentUser()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return AccountOutcome.AccountRemovalFailed
            }
        }
        prefs.deletionPendingUid = null
        _deletionPending.value = null
        return AccountOutcome.Deleted
    }
}
