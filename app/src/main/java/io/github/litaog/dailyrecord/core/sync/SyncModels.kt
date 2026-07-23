package io.github.litaog.dailyrecord.core.sync

import java.time.Instant
import java.time.LocalDate

internal data class RemoteHandBrewRecord(
    val id: String,
    val localDate: LocalDate,
    val brewCount: Int,
    val createdAt: Instant,
    val clientUpdatedAt: Instant,
    val deleted: Boolean,
    val revision: Long,
)

internal data class RemoteSnapshot(
    val records: List<RemoteHandBrewRecord>,
    val fromCache: Boolean,
)

internal data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val pending: Int,
)

sealed interface SyncStatus {
    data object NotConfigured : SyncStatus
    data object Offline : SyncStatus
    data object Syncing : SyncStatus
    data object UpToDate : SyncStatus
    data class Pending(val count: Int) : SyncStatus
    data class Failed(
        val message: String,
        val networkRelated: Boolean = false,
    ) : SyncStatus
}
