package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesDeletionStateStoreTest {
    private lateinit var context: Context
    private lateinit var deletionPreferences: android.content.SharedPreferences
    private lateinit var legacyCleanupPreferences: android.content.SharedPreferences

    @Before
    fun clearPreferences() {
        context = ApplicationProvider.getApplicationContext()
        deletionPreferences = context.getSharedPreferences(
            DeletionJournalPreferenceKeys.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        legacyCleanupPreferences = context.getSharedPreferences(
            DeletionJournalPreferenceKeys.LEGACY_CLEANUP_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        deletionPreferences.edit().clear().commit()
        legacyCleanupPreferences.edit().clear().commit()
    }

    @After
    fun restoreCleanPreferences() {
        deletionPreferences.edit().clear().commit()
        legacyCleanupPreferences.edit().clear().commit()
    }

    @Test
    fun legacyCleanupOwnerMigratesIntoJournalAndIsCleared() {
        legacyCleanupPreferences.edit()
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_CLEANUP_OWNER_IDS,
                setOf("owner-cleanup"),
            )
            .commit()

        val journal = SharedPreferencesDeletionStateStore(context).readJournal()

        assertEquals(
            DeletionJournalPhase.AuthDeletedCleanupPending,
            journal["owner-cleanup"]?.phase,
        )
        assertFalse(
            legacyCleanupPreferences.contains(
                DeletionJournalPreferenceKeys.LEGACY_CLEANUP_OWNER_IDS,
            ),
        )
    }

    @Test
    fun legacyOwnerSetsMigrateToOneVersionedEntryPerOwner() {
        deletionPreferences.edit()
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_IN_PROGRESS,
                setOf("owner-progress"),
            )
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_CLOUD_DELETION_PENDING,
                setOf("owner-cloud"),
            )
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_AUTH_DELETION_STARTED,
                setOf("owner-auth"),
            )
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_AUTH_DELETION_COMPLETE,
                setOf("owner-auth-complete"),
            )
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_CLEANUP_PENDING,
                setOf("owner-cleanup"),
            )
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_LOCAL_RECOVERY_COPY_PENDING,
                setOf("owner-copy-pending"),
            )
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_LOCAL_RECOVERY_COPY_READY,
                setOf("owner-copy-ready"),
            )
            .commit()

        val journal = SharedPreferencesDeletionStateStore(context).readJournal()

        assertEquals(DeletionJournalPhase.InProgress, journal["owner-progress"]?.phase)
        assertEquals(DeletionJournalPhase.CloudDeleted, journal["owner-cloud"]?.phase)
        assertEquals(DeletionJournalPhase.AuthDeletionPending, journal["owner-auth"]?.phase)
        assertEquals(
            DeletionJournalPhase.AuthDeletedCleanupPending,
            journal["owner-auth-complete"]?.phase,
        )
        assertEquals(
            DeletionJournalPhase.AuthDeletedCleanupPending,
            journal["owner-cleanup"]?.phase,
        )
        assertEquals(RecoveryCopyState.Pending, journal["owner-copy-pending"]?.recoveryCopy)
        assertEquals(RecoveryCopyState.Ready, journal["owner-copy-ready"]?.recoveryCopy)
        assertEquals(DeletionJournalEntry.CURRENT_VERSION, journal.values.single {
            it.phase == DeletionJournalPhase.InProgress
        }.version)
        assertTrue(
            deletionPreferences.getStringSet(
                DeletionJournalPreferenceKeys.LEGACY_IN_PROGRESS,
                emptySet(),
            ).orEmpty().isEmpty(),
        )
    }
}
