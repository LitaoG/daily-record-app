package io.github.litaog.dailyrecord.core.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Real launcher alias states on a device (ADR-024). */
class LauncherDisguiseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager: PackageManager = context.packageManager

    @Before
    fun startFromTheDefaultIdentity() {
        resetToManifestState()
    }

    @After
    fun restoreTheDefaultIdentity() {
        // Component states and preferences outlive this test class: leave the
        // default launcher entry for later tests and for the developer.
        resetToManifestState()
    }

    private fun resetToManifestState() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        LauncherIdentity.entries.forEach { identity ->
            packageManager.setComponentEnabledSetting(
                component(identity),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private fun component(identity: LauncherIdentity) =
        ComponentName(context.packageName, identity.aliasClassName)

    private fun isEnabled(identity: LauncherIdentity): Boolean =
        when (packageManager.getComponentEnabledSetting(component(identity))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> identity.enabledInManifest
            else -> false
        }

    @Suppress("DEPRECATION") // The ResolveInfoFlags overload needs API 33; minSdk is 26.
    private fun launcherEntries(): List<String> =
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.packageName),
            0,
        ).map { it.activityInfo.name }

    @Test
    fun startsWithOnlyTheDefaultEntry() {
        val disguise = LauncherDisguise(context)
        disguise.reconcile()

        assertEquals(LauncherIdentity.Default, disguise.identity)
        assertTrue(isEnabled(LauncherIdentity.Default))
        assertFalse(isEnabled(LauncherIdentity.Journal))
    }

    @Test
    fun turningTheDisguiseOnLeavesOnlyTheJournalEntryAndIsRemembered() {
        val disguise = LauncherDisguise(context)
        disguise.setIdentity(LauncherIdentity.Journal)

        assertFalse(isEnabled(LauncherIdentity.Default))
        assertTrue(isEnabled(LauncherIdentity.Journal))
        assertEquals(LauncherIdentity.Journal, disguise.identityState.value)
        assertEquals(LauncherIdentity.Journal, LauncherDisguise(context).identity)
    }

    @Test
    fun turningTheDisguiseOffRestoresTheDefaultEntry() {
        val disguise = LauncherDisguise(context)
        disguise.setIdentity(LauncherIdentity.Journal)
        disguise.setIdentity(LauncherIdentity.Default)

        assertTrue(isEnabled(LauncherIdentity.Default))
        assertFalse(isEnabled(LauncherIdentity.Journal))
        assertEquals(LauncherIdentity.Default, LauncherDisguise(context).identity)
    }

    @Test
    fun startRepairsASwitchThatStoppedHalfway() {
        // The choice was saved and the new entry enabled, but the process
        // died before the old entry was disabled: two icons.
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("identity", LauncherIdentity.Journal.id)
            .commit()
        packageManager.setComponentEnabledSetting(
            component(LauncherIdentity.Journal),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        LauncherDisguise(context).reconcile()

        assertFalse(isEnabled(LauncherIdentity.Default))
        assertTrue(isEnabled(LauncherIdentity.Journal))
    }

    @Test
    fun theLauncherListsExactlyOneEntryForEachIdentity() {
        val disguise = LauncherDisguise(context)
        for (identity in LauncherIdentity.entries) {
            disguise.setIdentity(identity)

            assertEquals(listOf(identity.aliasClassName), launcherEntries())
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_launcher"
    }
}
