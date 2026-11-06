package io.github.litaog.dailyrecord.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockSessionTest {
    @Test
    fun coldStartIsLockedOnlyWhenTheLockIsEnabled() {
        assertTrue(AppLockSession(enabled = true).locked.value)
        assertFalse(AppLockSession(enabled = false).locked.value)
    }

    @Test
    fun disabledLockNeverLocks() {
        val session = AppLockSession(enabled = false)
        session.onForeground(0L)
        session.onBackground(10L, changingConfigurations = false)
        session.onForeground(10_000_000L)

        assertFalse(session.locked.value)
    }

    @Test
    fun immediateDelayLocksOnEveryReturn() {
        val session = unlockedSession(AppLockDelay.Immediately)
        session.onBackground(1_000L, changingConfigurations = false)
        session.onForeground(1_000L)

        assertTrue(session.locked.value)
    }

    @Test
    fun timedDelayLocksOnlyAfterItHasPassed() {
        val session = unlockedSession(AppLockDelay.OneMinute)
        session.onBackground(1_000L, changingConfigurations = false)
        session.onForeground(1_000L + 59_999L)
        assertFalse(session.locked.value)

        session.onBackground(100_000L, changingConfigurations = false)
        session.onForeground(100_000L + 60_000L)
        assertTrue(session.locked.value)
    }

    @Test
    fun configurationChangesAndOwnRecreateDoNotLock() {
        val session = unlockedSession(AppLockDelay.Immediately)
        session.onBackground(1_000L, changingConfigurations = true)
        session.onForeground(500_000L)

        assertFalse(session.locked.value)
    }

    @Test
    fun authenticationScreenStoppingTheActivityDoesNotLockAgain() {
        // Android 8-9 path: the system credential screen is its own activity.
        val session = AppLockSession(enabled = true, delay = AppLockDelay.Immediately)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onBackground(10L, changingConfigurations = false)
        session.onForeground(20L)
        session.onAuthenticationFinished(success = true)

        assertFalse(session.locked.value)

        // The next real visit elsewhere locks again.
        session.onBackground(30L, changingConfigurations = false)
        session.onForeground(31L)
        assertTrue(session.locked.value)
    }

    @Test
    fun successArrivingWhileStillInTheBackgroundDoesNotLockAgain() {
        val session = AppLockSession(enabled = true, delay = AppLockDelay.Immediately)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onBackground(10L, changingConfigurations = false)
        session.onAuthenticationFinished(success = true)
        session.onForeground(20L)

        assertFalse(session.locked.value)

        session.onBackground(30L, changingConfigurations = false)
        session.onForeground(31L)
        assertTrue(session.locked.value)
    }

    @Test
    fun failedOrCancelledAuthenticationStaysLockedAndCanRetry() {
        val session = AppLockSession(enabled = true)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onAuthenticationFinished(success = false)
        assertTrue(session.locked.value)
        assertTrue(session.attemptFailed.value)

        session.onAuthenticationStarted()
        assertFalse(session.attemptFailed.value)
        session.onAuthenticationFinished(success = true)
        assertFalse(session.locked.value)
        assertFalse(session.attemptFailed.value)
    }

    @Test
    fun aStaleResultFromAnEarlierRequestIsIgnored() {
        assertNull(AppLockSession(enabled = false).onAuthenticationStarted())

        val session = AppLockSession(enabled = true)
        session.onForeground(0L)
        val first = session.onAuthenticationStarted()
        val second = session.onAuthenticationStarted()
        // The first prompt was cancelled by a rotation; its late error must not
        // swallow the success of the prompt that replaced it.
        session.onAuthenticationFinished(success = false, request = first)
        session.onAuthenticationFinished(success = true, request = second)

        assertFalse(session.locked.value)
    }

    @Test
    fun resultsWithoutAStartedRequestAreIgnored() {
        val session = AppLockSession(enabled = true)
        session.onForeground(0L)
        session.onAuthenticationFinished(success = true)

        assertTrue(session.locked.value)
    }

    @Test
    fun enablingNeverLocksThePersonUsingTheAppAndDisablingUnlocks() {
        val session = AppLockSession(enabled = false)
        session.onForeground(0L)
        session.setEnabled(true)
        assertFalse(session.locked.value)

        session.onBackground(10L, changingConfigurations = false)
        session.onForeground(11L)
        assertTrue(session.locked.value)

        // For example, the device no longer has a secure lock screen.
        session.setEnabled(false)
        assertFalse(session.locked.value)
    }

    @Test
    fun aClockRunningBackwardsCountsAsLongEnough() {
        val session = unlockedSession(AppLockDelay.FiveMinutes)
        session.onBackground(50_000L, changingConfigurations = false)
        session.onForeground(40_000L)

        assertTrue(session.locked.value)
    }

    @Test
    fun delayIdsRoundTripAndUnknownIdsFallBackToImmediately() {
        AppLockDelay.entries.forEach { delay ->
            assertEquals(delay, AppLockDelay.fromId(delay.id))
        }
        assertEquals(AppLockDelay.entries.size, AppLockDelay.entries.map { it.id }.distinct().size)
        assertEquals(AppLockDelay.Immediately, AppLockDelay.fromId(null))
        assertEquals(AppLockDelay.Immediately, AppLockDelay.fromId("ten_minutes"))
    }

    /** An enabled session the user has already unlocked after a cold start. */
    private fun unlockedSession(delay: AppLockDelay): AppLockSession {
        val session = AppLockSession(enabled = true, delay = delay)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onAuthenticationFinished(success = true)
        check(!session.locked.value)
        return session
    }
}
