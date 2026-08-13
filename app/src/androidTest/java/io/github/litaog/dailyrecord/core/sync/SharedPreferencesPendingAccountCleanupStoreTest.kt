package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesDeletionStateStoreTest {
    @Test
    fun legacyCleanupOwnerMigratesIntoJournalAndIsCleared() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacy = context.getSharedPreferences(
            DeletionJournalPreferenceKeys.LEGACY_CLEANUP_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        legacy.edit()
            .putStringSet(
                DeletionJournalPreferenceKeys.LEGACY_CLEANUP_OWNER_IDS,
                setOf("owner-1"),
            )
            .commit()

        val journal = SharedPreferencesDeletionStateStore(context).readJournal()

        assertEquals(
            DeletionJournalPhase.AuthDeletedCleanupPending,
            journal["owner-1"]?.phase,
        )
        assertFalse(legacy.contains(DeletionJournalPreferenceKeys.LEGACY_CLEANUP_OWNER_IDS))
    }
}
