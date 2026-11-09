package io.github.litaog.dailyrecord.core.sync

internal fun requireValidJournal(entries: Map<String, DeletionJournalEntry>) {
    entries.values.forEach { entry ->
        check(entry.version == DeletionJournalEntry.CURRENT_VERSION)
    }
}

internal fun requireValidTransition(
    current: DeletionJournalEntry,
    next: DeletionJournalEntry,
) {
    if (current == next) return
    val allowed = when (current.phase) {
        DeletionJournalPhase.InProgress ->
            next.phase == DeletionJournalPhase.InProgress ||
                next.phase == DeletionJournalPhase.CloudDeleted
        DeletionJournalPhase.CloudDeleted ->
            next.phase == DeletionJournalPhase.CloudDeleted ||
                next.phase == DeletionJournalPhase.AuthDeletionPending
        DeletionJournalPhase.AuthDeletionPending ->
            next.phase == DeletionJournalPhase.AuthDeletionPending ||
                next.phase == DeletionJournalPhase.AuthDeletedCleanupPending
        DeletionJournalPhase.AuthDeletedCleanupPending ->
            next.phase == DeletionJournalPhase.AuthDeletedCleanupPending
    }
    check(allowed) {
        "Invalid deletion journal transition: ${current.phase} -> ${next.phase}"
    }
}

internal val DeletionJournalEntry.isSafeSameProcessRetry: Boolean
    get() = phase == DeletionJournalPhase.InProgress &&
        recoveryCopy == RecoveryCopyState.None
