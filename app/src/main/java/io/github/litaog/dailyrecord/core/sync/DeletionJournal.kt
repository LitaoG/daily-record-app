package io.github.litaog.dailyrecord.core.sync

import android.content.Context

/**
 * The durable phase of one account-deletion attempt.
 *
 * A journal entry deliberately stores one phase instead of representing a
 * phase as the intersection/difference of several owner sets. This keeps the
 * ordering rules in one place and makes a persisted record explainable after
 * a process restart.
 */
internal enum class DeletionJournalPhase {
    InProgress,
    CloudDeleted,
    AuthDeletionPending,
    AuthDeletedCleanupPending,
}

internal enum class RecoveryCopyState {
    None,
    Pending,
    Ready,
}

internal data class DeletionJournalEntry(
    val version: Int = CURRENT_VERSION,
    val phase: DeletionJournalPhase,
    val recoveryCopy: RecoveryCopyState = RecoveryCopyState.None,
) {
    init {
        require(version == CURRENT_VERSION) {
            "Unsupported deletion journal version: $version"
        }
        require(phase != DeletionJournalPhase.InProgress ||
            recoveryCopy == RecoveryCopyState.None) {
            "An in-progress deletion cannot have a recovery copy phase"
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** Atomic, version-aware storage for all owner deletion journal entries. */
internal interface DeletionStateStore {
    fun readJournal(): Map<String, DeletionJournalEntry>

    fun writeJournal(entries: Map<String, DeletionJournalEntry>)
}

/** Legacy keys retained only as a one-time migration source. */
internal object DeletionJournalPreferenceKeys {
    const val PREFERENCES_NAME = "daily_record_deletion_state"
    const val LEGACY_CLEANUP_PREFERENCES_NAME = "daily_record_pending_local_cleanup"
    const val LEGACY_CLEANUP_OWNER_IDS = "owner_ids"
    const val LEGACY_IN_PROGRESS = "in_progress_owner_ids"
    const val LEGACY_CLEANUP_PENDING = "cleanup_pending_owner_ids"
    const val LEGACY_CLOUD_DELETION_PENDING = "cloud_deletion_pending_owner_ids"
    const val LEGACY_AUTH_DELETION_STARTED = "auth_deletion_started_owner_ids"
    const val LEGACY_AUTH_DELETION_COMPLETE = "auth_deletion_complete_owner_ids"
    const val LEGACY_LOCAL_RECOVERY_COPY_PENDING = "local_recovery_copy_pending_owner_ids"
    const val LEGACY_LOCAL_RECOVERY_COPY_READY = "local_recovery_copy_ready_owner_ids"
}

internal class SharedPreferencesDeletionStateStore(context: Context) : DeletionStateStore {
    private val legacyCleanupPreferences = context.applicationContext.getSharedPreferences(
        DeletionJournalPreferenceKeys.LEGACY_CLEANUP_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val lock = Any()

    /**
     * Parsed journal cache. [DeletionBarrier.isDeletionBlocked] runs on the
     * caller thread (including the main thread on every local save) and used
     * to decode every owner entry on each call; the journal only changes
     * through this store, so a volatile cache keeps repeated barrier checks
     * allocation- and decode-free. Writes update the cache under [lock].
     */
    @Volatile
    private var cachedEntries: Map<String, DeletionJournalEntry>? = null

    override fun readJournal(): Map<String, DeletionJournalEntry> {
        cachedEntries?.let { return it }
        return synchronized(lock) {
            cachedEntries ?: loadJournalLocked().also { cachedEntries = it }
        }
    }

    override fun writeJournal(entries: Map<String, DeletionJournalEntry>) {
        synchronized(lock) {
            writeJournalLocked(entries)
            cachedEntries = entries
        }
    }

    private fun loadJournalLocked(): Map<String, DeletionJournalEntry> = when (
        preferences.getInt(KEY_SCHEMA_VERSION, 0)
    ) {
        0 -> migrateLegacyJournal()
        DeletionJournalEntry.CURRENT_VERSION -> migrateLegacyCleanupPreference(readCurrentJournal())
        else -> error("Unsupported deletion journal schema")
    }

    private fun readCurrentJournal(): Map<String, DeletionJournalEntry> {
        val owners = preferences.getStringSet(KEY_OWNER_IDS, emptySet()).orEmpty()
        return owners.associateWith { ownerId ->
            val encoded = requireNotNull(preferences.getString(entryKey(ownerId), null)) {
                "Missing deletion journal entry for owner $ownerId"
            }
            decodeEntry(encoded)
        }
    }

    private fun migrateLegacyJournal(): Map<String, DeletionJournalEntry> {
        val inProgress = readLegacy(DeletionJournalPreferenceKeys.LEGACY_IN_PROGRESS)
        val cleanupPending = readLegacy(DeletionJournalPreferenceKeys.LEGACY_CLEANUP_PENDING)
        val cloudDeletionPending = readLegacy(
            DeletionJournalPreferenceKeys.LEGACY_CLOUD_DELETION_PENDING,
        )
        val authDeletionStarted = readLegacy(
            DeletionJournalPreferenceKeys.LEGACY_AUTH_DELETION_STARTED,
        )
        val authDeletionComplete = readLegacy(
            DeletionJournalPreferenceKeys.LEGACY_AUTH_DELETION_COMPLETE,
        )
        val localRecoveryCopyPending = readLegacy(
            DeletionJournalPreferenceKeys.LEGACY_LOCAL_RECOVERY_COPY_PENDING,
        )
        val localRecoveryCopyReady = readLegacy(
            DeletionJournalPreferenceKeys.LEGACY_LOCAL_RECOVERY_COPY_READY,
        )
        val legacyCleanupOwners = readLegacyCleanupOwners()
        val owners = inProgress + cleanupPending + cloudDeletionPending +
            authDeletionStarted + authDeletionComplete + localRecoveryCopyPending +
            localRecoveryCopyReady + legacyCleanupOwners
        val migrated = owners.associateWith { ownerId ->
            val recoveryCopy = when {
                ownerId in localRecoveryCopyReady -> RecoveryCopyState.Ready
                ownerId in localRecoveryCopyPending -> RecoveryCopyState.Pending
                else -> RecoveryCopyState.None
            }
            val phase = when {
                ownerId in authDeletionComplete || ownerId in cleanupPending ||
                    ownerId in legacyCleanupOwners ->
                    DeletionJournalPhase.AuthDeletedCleanupPending
                ownerId in authDeletionStarted -> DeletionJournalPhase.AuthDeletionPending
                ownerId in cloudDeletionPending || recoveryCopy != RecoveryCopyState.None ->
                    DeletionJournalPhase.CloudDeleted
                else -> DeletionJournalPhase.InProgress
            }
            DeletionJournalEntry(phase = phase, recoveryCopy = recoveryCopy)
        }
        writeJournalLocked(
            migrated,
            clearLegacyKeys = true,
            clearLegacyCleanupPreference = true,
        )
        return migrated
    }

    private fun migrateLegacyCleanupPreference(
        current: Map<String, DeletionJournalEntry>,
    ): Map<String, DeletionJournalEntry> {
        val legacyOwners = readLegacyCleanupOwners()
        if (legacyOwners.isEmpty()) return current
        val migrated = current.toMutableMap()
        legacyOwners.forEach { ownerId ->
            migrated.putIfAbsent(
                ownerId,
                DeletionJournalEntry(phase = DeletionJournalPhase.AuthDeletedCleanupPending),
            )
        }
        writeJournalLocked(migrated, clearLegacyCleanupPreference = true)
        return migrated
    }

    private fun readLegacy(key: String): Set<String> =
        preferences.getStringSet(key, emptySet()).orEmpty().toSet()

    private fun readLegacyCleanupOwners(): Set<String> =
        legacyCleanupPreferences.getStringSet(
            DeletionJournalPreferenceKeys.LEGACY_CLEANUP_OWNER_IDS,
            emptySet(),
        ).orEmpty().toSet()

    @Suppress("UseKtx")
    private fun writeJournalLocked(
        entries: Map<String, DeletionJournalEntry>,
        clearLegacyKeys: Boolean = false,
        clearLegacyCleanupPreference: Boolean = false,
    ) {
        val previousOwners = preferences.getStringSet(KEY_OWNER_IDS, emptySet()).orEmpty()
        val editor = preferences.edit()
        (previousOwners - entries.keys).forEach { ownerId ->
            editor.remove(entryKey(ownerId))
        }
        entries.forEach { (ownerId, entry) ->
            editor.putString(entryKey(ownerId), encodeEntry(entry))
        }
        if (clearLegacyKeys) {
            legacyKeys.forEach(editor::remove)
        }
        check(
            editor
                .putStringSet(KEY_OWNER_IDS, entries.keys)
                .putInt(KEY_SCHEMA_VERSION, DeletionJournalEntry.CURRENT_VERSION)
                .commit(),
        ) {
            "Unable to persist account deletion journal"
        }
        if (clearLegacyCleanupPreference) {
            check(
                legacyCleanupPreferences.edit()
                    .remove(DeletionJournalPreferenceKeys.LEGACY_CLEANUP_OWNER_IDS)
                    .commit(),
            ) {
                "Unable to clear legacy pending account cleanup"
            }
        }
    }

    private fun encodeEntry(entry: DeletionJournalEntry): String =
        listOf(entry.version, entry.phase.name, entry.recoveryCopy.name).joinToString(DELIMITER)

    private fun decodeEntry(encoded: String): DeletionJournalEntry {
        val parts = encoded.split(DELIMITER)
        require(parts.size == 3) { "Malformed deletion journal entry" }
        return DeletionJournalEntry(
            version = parts[0].toIntOrNull() ?: error("Malformed deletion journal version"),
            phase = runCatching { DeletionJournalPhase.valueOf(parts[1]) }
                .getOrElse { error("Malformed deletion journal phase") },
            recoveryCopy = runCatching { RecoveryCopyState.valueOf(parts[2]) }
                .getOrElse { error("Malformed deletion journal recovery state") },
        )
    }

    private fun entryKey(ownerId: String): String = "$KEY_ENTRY_PREFIX$ownerId"

    private companion object {
        const val PREFERENCES_NAME = DeletionJournalPreferenceKeys.PREFERENCES_NAME
        const val KEY_SCHEMA_VERSION = "deletion_journal_schema_version"
        const val KEY_OWNER_IDS = "deletion_journal_owner_ids"
        const val KEY_ENTRY_PREFIX = "deletion_journal_entry_"
        const val DELIMITER = "\u0001"

        val legacyKeys = listOf(
            DeletionJournalPreferenceKeys.LEGACY_IN_PROGRESS,
            DeletionJournalPreferenceKeys.LEGACY_CLEANUP_PENDING,
            DeletionJournalPreferenceKeys.LEGACY_CLOUD_DELETION_PENDING,
            DeletionJournalPreferenceKeys.LEGACY_AUTH_DELETION_STARTED,
            DeletionJournalPreferenceKeys.LEGACY_AUTH_DELETION_COMPLETE,
            DeletionJournalPreferenceKeys.LEGACY_LOCAL_RECOVERY_COPY_PENDING,
            DeletionJournalPreferenceKeys.LEGACY_LOCAL_RECOVERY_COPY_READY,
        )
    }
}
