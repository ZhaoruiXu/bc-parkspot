package ca.bcparkspot.app.sync

import ca.bcparkspot.app.data.local.entity.SyncedRecord
import ca.bcparkspot.app.data.local.entity.needsUpload

/** Result of re-owning one row into a target account that may already hold a row with the same key. */
enum class AdoptOutcome { MOVE, INCOMING_WINS, EXISTING_WINS }

/**
 * 4.5a: with no clash the row just moves. On a clash the newer `updatedAt` wins (tombstones included);
 * ties keep the row already in the target account, so adoption is stable.
 */
fun adoptOutcome(incoming: SyncedRecord, existing: SyncedRecord?): AdoptOutcome = when {
    existing == null -> AdoptOutcome.MOVE
    incoming.updatedAt > existing.updatedAt -> AdoptOutcome.INCOMING_WINS
    else -> AdoptOutcome.EXISTING_WINS
}

/** Version numbers for an adopted winner: it keeps the target's synced version and needs upload. */
fun adoptedVersions(existing: SyncedRecord?): Pair<Long, Long> {
    val synced = existing?.syncedVersion ?: 0L
    return (synced + 1) to synced
}

/** A cloud record as the listener saw it. */
data class CloudRecordMeta(val updatedAt: Long, val deleted: Boolean, val fromCache: Boolean)

enum class CloudApply { APPLY, IGNORE }

/**
 * 4.13: a cloud record replaces the local row only when it came from the server, the local row has
 * nothing pending, and the cloud copy is newer. A cloud tombstone with no local row is ignored, so a
 * purged local tombstone stays purged.
 */
fun cloudApply(local: SyncedRecord?, cloud: CloudRecordMeta): CloudApply = when {
    cloud.fromCache -> CloudApply.IGNORE
    local == null -> if (cloud.deleted) CloudApply.IGNORE else CloudApply.APPLY
    local.needsUpload -> CloudApply.IGNORE
    cloud.updatedAt > local.updatedAt -> CloudApply.APPLY
    else -> CloudApply.IGNORE
}

/** 4.9: during upload, the newer of the two records wins in both directions. Ties go to the device. */
enum class UploadWinner { LOCAL, CLOUD }

fun uploadWinner(localUpdatedAt: Long, cloudUpdatedAt: Long?): UploadWinner =
    if (cloudUpdatedAt != null && cloudUpdatedAt > localUpdatedAt) UploadWinner.CLOUD else UploadWinner.LOCAL

/** 4.3: a local tombstone is removable once it is synced and older than the retention period. */
fun tombstonePurgeBefore(nowMs: Long, retentionMs: Long): Long = nowMs - retentionMs
