package io.github.litaog.dailyrecord.core.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional home screen disguise (ADR-024). Stores the chosen launcher identity
 * on this device and switches the manifest aliases to match. Only the aliases
 * are enabled or disabled; `ui.MainActivity` itself never is, so the screen
 * the user is on keeps running while the launcher updates.
 */
internal class LauncherDisguise(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _identity = MutableStateFlow(identity)

    val identityState: StateFlow<LauncherIdentity> = _identity.asStateFlow()

    /** The default identity until the user turns the disguise on. */
    val identity: LauncherIdentity
        get() = LauncherIdentity.fromId(preferences.getString(KEY_IDENTITY, null))

    fun setIdentity(value: LauncherIdentity) {
        if (value != identity) {
            // Written synchronously before the aliases change: if the process
            // dies mid-switch, the next start repairs them from this value.
            preferences.edit(commit = true) { putString(KEY_IDENTITY, value.id) }
            _identity.value = value
        }
        reconcile()
    }

    /**
     * Makes the enabled aliases match the stored identity. This also repairs
     * an interrupted switch, or aliases left behind by "clear storage".
     */
    fun reconcile() {
        val packageManager = appContext.packageManager
        // A launcher entry that cannot be read or changed must not crash the
        // app at start; the next start tries again.
        runCatching {
            val enabledNow = LauncherIdentity.entries.filter { isEnabled(packageManager, it) }.toSet()
            for (change in launcherAliasChanges(identity, enabledNow)) {
                packageManager.setComponentEnabledSetting(
                    componentFor(change.identity),
                    if (change.enabled) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    private fun isEnabled(packageManager: PackageManager, identity: LauncherIdentity): Boolean =
        when (packageManager.getComponentEnabledSetting(componentFor(identity))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> identity.enabledInManifest
            else -> false
        }

    private fun componentFor(identity: LauncherIdentity): ComponentName =
        ComponentName(appContext.packageName, identity.aliasClassName)

    private companion object {
        const val PREFERENCES_NAME = "daily_record_launcher"
        const val KEY_IDENTITY = "identity"
    }
}
