package io.github.litaog.dailyrecord.core.sync

/**
 * A sync attempt that still has pending rows should be retried only when the
 * rows failed for transient reasons. Rows whose cloud documents were rejected
 * as malformed can never sync: retrying them would burn WorkManager backoff
 * and Firestore quota until the ceiling without making progress.
 */
internal fun SyncResult.workerShouldRetry(): Boolean =
    pending > 0 && rejectedRemoteRecords == 0
