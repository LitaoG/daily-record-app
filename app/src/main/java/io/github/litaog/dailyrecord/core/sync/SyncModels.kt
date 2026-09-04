package io.github.litaog.dailyrecord.core.sync

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

internal data class RemoteHandBrewDetail(
    val id: String,
    val occurrenceIndex: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val feeling: String,
)

internal data class RemoteSexDetail(
    val id: String,
    val occurrenceIndex: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val feeling: String,
)

/**
 * One module's remote record at the sync boundary. Modules keep their own
 * count field and detail types; the sealed shape lets snapshots stay
 * module-agnostic so adding a module never changes [RemoteSnapshot].
 */
internal sealed interface RemoteDailyCountRecord {
    val id: String
    val localDate: LocalDate
    val createdAt: Instant
    val clientUpdatedAt: Instant
    val deleted: Boolean
    val revision: Long
}

internal data class RemoteHandBrewRecord(
    override val id: String,
    override val localDate: LocalDate,
    val brewCount: Int,
    override val createdAt: Instant,
    override val clientUpdatedAt: Instant,
    override val deleted: Boolean,
    override val revision: Long,
    val details: List<RemoteHandBrewDetail> = emptyList(),
) : RemoteDailyCountRecord

internal data class RemoteSexRecord(
    override val id: String,
    override val localDate: LocalDate,
    val sexCount: Int,
    override val createdAt: Instant,
    override val clientUpdatedAt: Instant,
    override val deleted: Boolean,
    override val revision: Long,
    val details: List<RemoteSexDetail> = emptyList(),
) : RemoteDailyCountRecord

internal data class RemoteSnapshot(
    val records: List<RemoteDailyCountRecord> = emptyList(),
    val fromCache: Boolean,
    val rejectedRecordCount: Int = 0,
)

internal data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val pending: Int,
    val rejectedRemoteRecords: Int = 0,
)

/** Identity prefix for the local recovery copy of an account's records. */
internal fun localCopyId(id: String): String = "__local__-copy-$id"

/** Dedicated Room owner namespace for a pending account-deletion recovery copy. */
internal fun recoveryOwnerId(ownerId: String): String = "__recovery__:$ownerId"

internal fun localCopySourceId(id: String): String =
    id.removePrefix("__local__-copy-")

enum class SyncFailureKind {
    Network,
    Authentication,
    Permission,
    Quota,
    Service,
    Data,
    Unknown,
}

/**
 * A cloud failure already classified at the boundary that produced it. The
 * classifier in AccountSyncManager honors this kind before inspecting the
 * cause chain, so boundaries can translate SDK-specific errors (such as
 * Cloud Functions status codes, whose types touch Android-only static state)
 * without leaking those types into the shared classification path.
 */
internal class ClassifiedSyncException(
    val kind: SyncFailureKind,
    cause: Throwable,
) : RuntimeException(cause)

sealed interface SyncStatus {
    data object NotConfigured : SyncStatus
    data object Offline : SyncStatus
    data object Syncing : SyncStatus
    data object UpToDate : SyncStatus
    data class Pending(val count: Int) : SyncStatus
    data class Failed(
        val message: String,
        val kind: SyncFailureKind = SyncFailureKind.Unknown,
    ) : SyncStatus {
        val networkRelated: Boolean
            get() = kind == SyncFailureKind.Network
    }
}
