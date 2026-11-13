package ca.bcparkspot.app.ui.account

import android.content.res.Resources
import ca.bcparkspot.app.R
import ca.bcparkspot.app.auth.AccountBusy
import ca.bcparkspot.app.auth.AccountOutcome
import ca.bcparkspot.app.auth.GoogleTokenResult
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.CommunityReport
import ca.bcparkspot.app.sync.FeedStatus
import ca.bcparkspot.app.ui.text.formatAge

data class AccountUiState(
    val isGuest: Boolean = true,
    val email: String? = null,
    val pendingChanges: Int = 0,
    val notSyncedItems: Int = 0,
    val rejectedReports: Int = 0,
    val lastSyncAt: Long? = null,
    val online: Boolean = false,
    val busy: AccountBusy? = null,
    val deletionPending: Boolean = false
)

/** A question an account flow is waiting on; the sheet shows it as a dialog. */
sealed interface AccountPrompt {
    data class ConfirmMerge(val items: Int) : AccountPrompt
    data class ConfirmSignOut(val pendingChanges: Int) : AccountPrompt
}

fun outcomeMessage(res: Resources, outcome: AccountOutcome): String? = when (outcome) {
    AccountOutcome.Linked -> res.getString(R.string.account_linked)
    is AccountOutcome.Merged -> res.getQuantityString(R.plurals.account_merged, outcome.items, outcome.items)
    AccountOutcome.Cancelled, AccountOutcome.NotAllowed -> null
    AccountOutcome.ServerUnreachable -> res.getString(R.string.account_server_unreachable)
    AccountOutcome.MoveFailed -> res.getString(R.string.account_move_failed)
    AccountOutcome.SignInFailedItemsKept -> res.getString(R.string.account_sign_in_failed_kept)
    is AccountOutcome.Failed -> res.getString(R.string.account_failed)
    is AccountOutcome.SignedOut ->
        if (outcome.keptChanges > 0) res.getQuantityString(R.plurals.account_signed_out_kept, outcome.keptChanges, outcome.keptChanges)
        else res.getString(R.string.account_signed_out)
    AccountOutcome.Deleted -> res.getString(R.string.account_deleted)
    AccountOutcome.DeleteIncomplete -> res.getString(R.string.account_delete_incomplete)
    AccountOutcome.AccountRemovalFailed -> res.getString(R.string.account_removal_failed)
}

fun tokenMessage(res: Resources, result: GoogleTokenResult): String? = when (result) {
    is GoogleTokenResult.Token, GoogleTokenResult.Cancelled -> null
    GoogleTokenResult.NoAccount -> res.getString(R.string.google_no_account)
    GoogleTokenResult.NotConfigured -> res.getString(R.string.google_not_configured)
    is GoogleTokenResult.Failed -> res.getString(R.string.account_failed)
}

fun busyMessage(res: Resources, busy: AccountBusy): String = res.getString(
    when (busy) {
        AccountBusy.SIGNING_IN -> R.string.account_busy_signing_in
        AccountBusy.SIGNING_OUT -> R.string.account_busy_signing_out
        AccountBusy.DELETING -> R.string.account_busy_deleting
    }
)

fun feedMessage(res: Resources, status: FeedStatus, nowMs: Long): String = when (status) {
    FeedStatus.Live -> res.getString(R.string.feed_live)
    FeedStatus.Loading -> res.getString(R.string.feed_loading)
    FeedStatus.PausedForToday -> res.getString(R.string.feed_paused)
    is FeedStatus.Stale -> res.getString(R.string.feed_stale, formatAge(res, nowMs - status.lastServerSnapshotAt))
    FeedStatus.NeverLoaded -> res.getString(R.string.feed_never)
}

fun lastSyncText(res: Resources, lastSyncAt: Long?, nowMs: Long): String =
    if (lastSyncAt == null) res.getString(R.string.account_never_synced)
    else res.getString(R.string.account_last_sync, formatAge(res, nowMs - lastSyncAt))

/** A device-only report shown on its lot while it waits to sync; null if its stored status is unknown. */
fun OwnReportEntity.toCommunityReport(): CommunityReport? {
    val parsed = AvailabilityStatus.entries.firstOrNull { it.name == status } ?: return null
    return CommunityReport(
        id = id,
        userId = ownerUid,
        lotId = lotId,
        reporterName = reporterName,
        statusReported = parsed,
        comment = comment,
        spotsEstimate = spotsEstimate,
        timestampMs = observedAt
    )
}
