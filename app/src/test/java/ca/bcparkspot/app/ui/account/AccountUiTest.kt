package ca.bcparkspot.app.ui.account

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.R
import ca.bcparkspot.app.auth.AccountBusy
import ca.bcparkspot.app.auth.AccountOutcome
import ca.bcparkspot.app.auth.GoogleTokenResult
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.sync.FeedStatus
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountUiTest {
    private val res: Resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val now = 1_700_000_000_000L
    private val items = 3

    @Test
    fun `every outcome the user should hear about has a message, cancels stay silent`() {
        assertNull(outcomeMessage(res, AccountOutcome.Cancelled))
        assertNull(outcomeMessage(res, AccountOutcome.NotAllowed))
        listOf(
            AccountOutcome.Linked, AccountOutcome.ServerUnreachable, AccountOutcome.MoveFailed,
            AccountOutcome.SignInFailedItemsKept, AccountOutcome.Failed("x"), AccountOutcome.SignedOut(0),
            AccountOutcome.Deleted, AccountOutcome.DeleteIncomplete, AccountOutcome.AccountRemovalFailed
        ).forEach { assertNotNull(it.toString(), outcomeMessage(res, it)) }
    }

    @Test
    fun `merge and sign-out counts are spelled out`() {
        assertEquals(res.getQuantityString(R.plurals.account_merged, items, items), outcomeMessage(res, AccountOutcome.Merged(items)))
        assertTrue(outcomeMessage(res, AccountOutcome.SignedOut(items))!!.contains(items.toString()))
        assertEquals(res.getString(R.string.account_signed_out), outcomeMessage(res, AccountOutcome.SignedOut(0)))
    }

    @Test
    fun `token results map to a message or silence`() {
        assertNull(tokenMessage(res, GoogleTokenResult.Token("t")))
        assertNull(tokenMessage(res, GoogleTokenResult.Cancelled))
        assertEquals(res.getString(R.string.google_no_account), tokenMessage(res, GoogleTokenResult.NoAccount))
        assertEquals(res.getString(R.string.google_not_configured), tokenMessage(res, GoogleTokenResult.NotConfigured))
        assertEquals(res.getString(R.string.account_failed), tokenMessage(res, GoogleTokenResult.Failed(null)))
    }

    @Test
    fun `each busy state has its own progress text`() {
        val texts = AccountBusy.entries.map { busyMessage(res, it) }
        assertEquals(texts.size, texts.toSet().size)
    }

    @Test
    fun `feed messages, including how old a stale feed is`() {
        val age = TimeUnit.MINUTES.toMillis(items.toLong())
        assertEquals(res.getString(R.string.feed_live), feedMessage(res, FeedStatus.Live, now))
        assertEquals(res.getString(R.string.feed_paused), feedMessage(res, FeedStatus.PausedForToday, now))
        assertEquals(res.getString(R.string.feed_never), feedMessage(res, FeedStatus.NeverLoaded, now))
        assertEquals(res.getString(R.string.feed_loading), feedMessage(res, FeedStatus.Loading, now))
        assertTrue(feedMessage(res, FeedStatus.Stale(now - age), now).contains(res.getQuantityString(R.plurals.age_minutes, items, items)))
    }

    @Test
    fun `last sync text says never until a sync finishes`() {
        assertEquals(res.getString(R.string.account_never_synced), lastSyncText(res, null, now))
        assertEquals(res.getString(R.string.account_last_sync, res.getString(R.string.age_just_now)), lastSyncText(res, now, now))
    }

    @Test
    fun `a waiting report shows on its lot with its own time, an unknown status is dropped`() {
        val row = OwnReportEntity("id", "me", "lot", "Park", "BC Hiker", "FILLING_FAST", "busy", 4, now)
        val shown = row.toCommunityReport()!!
        assertEquals(AvailabilityStatus.FILLING_FAST, shown.statusReported)
        assertEquals(now, shown.timestampMs)
        assertEquals(4, shown.spotsEstimate)
        assertNull(row.copy(status = "MAYBE").toCommunityReport())
    }
}
