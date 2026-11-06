package io.github.litaog.dailyrecord.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearStoredLock() {
        clearLockPrefs()
    }

    @After
    fun resetStoredLock() {
        // SharedPreferences survive across test classes in one instrumentation
        // process: a stored "on" here would put the lock screen in front of
        // every later MainActivity test.
        clearLockPrefs()
    }

    private fun clearLockPrefs() {
        context.getSharedPreferences("daily_record_app_lock", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsToOffAndImmediately() {
        val preference = AppLockPreference(context)

        assertFalse(preference.enabled)
        assertEquals(AppLockDelay.Immediately, preference.delay)
        assertFalse(preference.enabledState.value)
        assertEquals(AppLockDelay.Immediately, preference.delayState.value)
    }

    @Test
    fun persistsTheChoiceAcrossInstances() {
        AppLockPreference(context).apply {
            setEnabled(true)
            setDelay(AppLockDelay.FiveMinutes)
        }

        val restored = AppLockPreference(context)
        assertTrue(restored.enabled)
        assertEquals(AppLockDelay.FiveMinutes, restored.delay)
    }

    @Test
    fun flowsReflectTheChoice() {
        val preference = AppLockPreference(context)
        preference.setEnabled(true)
        preference.setDelay(AppLockDelay.OneMinute)

        assertTrue(preference.enabledState.value)
        assertEquals(AppLockDelay.OneMinute, preference.delayState.value)
    }

    @Test
    fun unknownStoredDelayFallsBackToImmediately() {
        context.getSharedPreferences("daily_record_app_lock", Context.MODE_PRIVATE)
            .edit()
            .putString("relock_delay", "ten_minutes")
            .commit()

        assertEquals(AppLockDelay.Immediately, AppLockPreference(context).delay)
    }
}
