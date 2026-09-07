package io.github.litaog.dailyrecord.core.sync

/**
 * A sync attempt that still has pending rows should be retried only when the
 * rows failed for transient reasons. Locally quarantined rows (the server
 * refused them with a data error) can never sync until the user edits the
 * date again, so they stop WorkManager retries; malformed cloud documents
 * elsewhere must not starve the healthy pending rows of either module.
 */
internal fun SyncResult.workerShouldRetry(): Boolean =
    pending > 0 && quarantinedLocalRecords == 0
