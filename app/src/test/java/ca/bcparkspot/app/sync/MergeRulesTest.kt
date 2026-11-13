package ca.bcparkspot.app.sync

import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeRulesTest {
    private val earlier = 1_000L
    private val later = 2_000L

    private fun row(updatedAt: Long, deleted: Boolean = false, version: Long = 1, synced: Long = 0) =
        SavedLotEntity("owner", "lot", "Park", "Lot", "Region", "", updatedAt, updatedAt, deleted, version, synced)

    @Test
    fun `adopt with no clash moves the row`() {
        assertEquals(AdoptOutcome.MOVE, adoptOutcome(row(earlier), null))
    }

    @Test
    fun `adopt clash keeps the newer row, tombstones included`() {
        assertEquals(AdoptOutcome.INCOMING_WINS, adoptOutcome(row(later, deleted = true), row(earlier)))
        assertEquals(AdoptOutcome.EXISTING_WINS, adoptOutcome(row(earlier), row(later, deleted = true)))
    }

    @Test
    fun `adopt tie keeps the target row`() {
        assertEquals(AdoptOutcome.EXISTING_WINS, adoptOutcome(row(earlier), row(earlier)))
    }

    @Test
    fun `adopted winner needs upload on top of the target's synced version`() {
        assertEquals(1L to 0L, adoptedVersions(null))
        val synced = 4L
        assertEquals((synced + 1) to synced, adoptedVersions(row(earlier, version = synced, synced = synced)))
    }

    @Test
    fun `cache snapshots are never applied`() {
        assertEquals(CloudApply.IGNORE, cloudApply(null, CloudRecordMeta(later, deleted = false, fromCache = true)))
        assertEquals(CloudApply.IGNORE, cloudApply(row(earlier, version = 1, synced = 1), CloudRecordMeta(later, false, fromCache = true)))
    }

    @Test
    fun `a cloud tombstone with no local row is not inserted`() {
        assertEquals(CloudApply.IGNORE, cloudApply(null, CloudRecordMeta(later, deleted = true, fromCache = false)))
    }

    @Test
    fun `a new cloud record with no local row is inserted`() {
        assertEquals(CloudApply.APPLY, cloudApply(null, CloudRecordMeta(later, deleted = false, fromCache = false)))
    }

    @Test
    fun `a local row with a pending change is never overwritten`() {
        assertEquals(CloudApply.IGNORE, cloudApply(row(earlier, version = 2, synced = 1), CloudRecordMeta(later, false, false)))
    }

    @Test
    fun `a settled local row takes a newer cloud record, not an older or equal one`() {
        val settled = row(earlier, version = 1, synced = 1)
        assertEquals(CloudApply.APPLY, cloudApply(settled, CloudRecordMeta(later, true, false)))
        assertEquals(CloudApply.IGNORE, cloudApply(settled, CloudRecordMeta(earlier, false, false)))
        assertEquals(CloudApply.IGNORE, cloudApply(row(later, version = 1, synced = 1), CloudRecordMeta(earlier, false, false)))
    }

    @Test
    fun `upload keeps the newer record in both directions, ties go to the device`() {
        assertEquals(UploadWinner.LOCAL, uploadWinner(later, null))
        assertEquals(UploadWinner.LOCAL, uploadWinner(later, earlier))
        assertEquals(UploadWinner.LOCAL, uploadWinner(later, later))
        assertEquals(UploadWinner.CLOUD, uploadWinner(earlier, later))
    }

    @Test
    fun `tombstone purge cutoff is the retention period before now`() {
        val now = SyncPolicy.tombstoneRetentionMs * 2
        assertEquals(SyncPolicy.tombstoneRetentionMs, tombstonePurgeBefore(now, SyncPolicy.tombstoneRetentionMs))
    }
}
