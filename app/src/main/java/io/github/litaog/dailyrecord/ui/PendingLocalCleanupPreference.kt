package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.core.content.edit

/**
 * Persists account owner ids whose owner cache could not be cleared during
 * account deletion. A later startup retries the cleanup so records of a
 * deleted account are never silently kept (or misread by a future account).
 */
internal class PendingLocalCleanupPreference(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val ownerIds: Set<String>
        get() = preferences.getStringSet(KEY_OWNERS, emptySet()).orEmpty()

    fun add(ownerId: String) {
        preferences.edit { putStringSet(KEY_OWNERS, ownerIds + ownerId) }
    }

    fun remove(ownerId: String) {
        preferences.edit { putStringSet(KEY_OWNERS, ownerIds - ownerId) }
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_pending_local_cleanup"
        const val KEY_OWNERS = "owner_ids"
    }
}
