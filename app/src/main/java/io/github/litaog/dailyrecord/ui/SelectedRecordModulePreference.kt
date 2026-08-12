package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.core.content.edit

/** Persists the user's last selected record module across launches. */
internal class SelectedRecordModulePreference(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val selectedModule: RecordModule
        get() = preferences.getString(KEY_SELECTED_MODULE, null)
            ?.let { stored -> RecordModule.entries.firstOrNull { it.name == stored } }
            ?: RecordModule.HandBrew

    fun setSelectedModule(module: RecordModule) {
        preferences.edit { putString(KEY_SELECTED_MODULE, module.name) }
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_module"
        const val KEY_SELECTED_MODULE = "selected_module"
    }
}
