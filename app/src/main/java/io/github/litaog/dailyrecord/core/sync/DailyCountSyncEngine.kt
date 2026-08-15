package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.account.AccountRemoteDataStore

import kotlinx.coroutines.flow.Flow

/**
 * Storage abstraction shared by the daily-count modules. Domain entities and
 * cloud records remain strongly typed at the adapter boundary.
 */
internal interface DailyCountSyncStore<LocalRecord, RemoteRecord> {
    fun observePendingCount(ownerId: String): Flow<Int>
    suspend fun pending(ownerId: String): List<LocalRecord>
    suspend fun pendingCount(ownerId: String): Int
    suspend fun adoptLocalRecords(ownerId: String): Int
    suspend fun applyRemote(ownerId: String, records: List<RemoteRecord>): Int
    suspend fun alignUnbasedPendingRevisions(ownerId: String, records: List<RemoteRecord>): Int
    suspend fun applyCommitIfUnchanged(
        ownerId: String,
        local: LocalRecord,
        committed: RemoteRecord,
    ): Boolean

    /**
     * Rebases a still-pending row on the revision that the server just
     * confirmed for it. Called when the server returned a record matching the
     * submitted content (usually our own successful commit) but the local row
     * was edited while the request was in flight: the newer local edit keeps
     * its content and pending state, and the next commit writes it against the
     * confirmed revision instead of conflicting with our own earlier write.
     */
    suspend fun rebasePending(
        ownerId: String,
        local: LocalRecord,
        committed: RemoteRecord,
    ): Int
}

internal interface DailyCountRemoteDataSource<LocalRecord, RemoteRecord> :
    AccountRemoteDataStore {
    fun observe(ownerId: String): Flow<RemoteSnapshot>
    suspend fun fetch(ownerId: String): RemoteSnapshot
    fun recordsFrom(snapshot: RemoteSnapshot): List<RemoteRecord>
    suspend fun commit(ownerId: String, local: LocalRecord): RemoteRecord
    fun matches(remote: RemoteRecord, local: LocalRecord): Boolean
}

/**
 * Shared conflict/retry algorithm for daily-count modules. Runs commits,
 * applies remote snapshots and keeps pending rows consistent under the
 * revision protocol; both modules share this engine with their own typed
 * store and remote adapter.
 */
internal class DailyCountSyncEngine<LocalRecord, RemoteRecord>(
    private val store: DailyCountSyncStore<LocalRecord, RemoteRecord>,
    private val remote: DailyCountRemoteDataSource<LocalRecord, RemoteRecord>,
) {
    fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = remote.observe(ownerId)

    fun observePendingCount(ownerId: String): Flow<Int> = store.observePendingCount(ownerId)

    suspend fun pendingCount(ownerId: String): Int = store.pendingCount(ownerId)

    suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        store.applyRemote(ownerId, remote.recordsFrom(snapshot))

    suspend fun prepareLocalAccount(ownerId: String): Int {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        return store.adoptLocalRecords(ownerId)
    }

    suspend fun syncOnce(ownerId: String): SyncResult {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        store.adoptLocalRecords(ownerId)
        val initial = remote.fetch(ownerId)
        val initialRecords = remote.recordsFrom(initial)
        store.alignUnbasedPendingRevisions(ownerId, initialRecords)
        var downloaded = store.applyRemote(ownerId, initialRecords)
        var uploaded = 0
        var rejected = initial.rejectedRecordCount

        val pending = store.pending(ownerId)
        pending.forEach { local ->
            val committed = try {
                remote.commit(ownerId, local)
            } catch (_: MalformedRemoteRecordException) {
                rejected += 1
                return@forEach
            }
            if (store.applyCommitIfUnchanged(ownerId, local, committed)) {
                if (remote.matches(committed, local)) uploaded += 1 else downloaded += 1
            } else if (remote.matches(committed, local)) {
                // The server returned a record matching the submitted content
                // (usually our own successful commit), but the local row was
                // edited while the request was in flight. Keep the newer local
                // edit pending and rebase it on the server-confirmed revision
                // so the next attempt writes it instead of silently losing it
                // to a conflict with our own earlier write.
                store.rebasePending(ownerId, local, committed)
            }
            // A conflicting committed record (server had a different revision)
            // with an unchanged local row is applied by applyCommitIfUnchanged
            // as the authoritative server version; with a changed local row the
            // server version wins on the next attempt per ADR-010 and is never
            // rebased over another device's data.
        }

        // The initial server snapshot is already authoritative when there is
        // no local work to commit. Avoid a second full collection read in the
        // common clean-sync path; a second fetch is still required after a
        // pending upload to observe the server's committed/conflicting state.
        if (pending.isNotEmpty()) {
            val confirmed = remote.fetch(ownerId)
            downloaded += store.applyRemote(ownerId, remote.recordsFrom(confirmed))
            rejected = maxOf(rejected, confirmed.rejectedRecordCount)
        }
        return SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            pending = store.pendingCount(ownerId),
            rejectedRemoteRecords = rejected,
        )
    }
}
