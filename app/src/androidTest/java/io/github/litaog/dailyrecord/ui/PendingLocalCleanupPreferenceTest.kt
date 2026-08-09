package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingLocalCleanupPreferenceTest {
    @Test
    fun pendingOwnerSurvivesNewPreferenceInstanceAndCanBeRemoved() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preference = PendingLocalCleanupPreference(context)
        preference.remove("owner-1")
        preference.remove("owner-2")

        preference.add("owner-1")
        preference.add("owner-2")
        assertTrue(PendingLocalCleanupPreference(context).ownerIds.contains("owner-1"))
        assertTrue(PendingLocalCleanupPreference(context).ownerIds.contains("owner-2"))

        preference.remove("owner-1")
        val remaining = PendingLocalCleanupPreference(context).ownerIds
        assertFalse(remaining.contains("owner-1"))
        assertTrue(remaining.contains("owner-2"))
    }
}
