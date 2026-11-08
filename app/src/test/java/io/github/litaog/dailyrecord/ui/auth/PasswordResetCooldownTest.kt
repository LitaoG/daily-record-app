package io.github.litaog.dailyrecord.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordResetCooldownTest {
    @Before
    fun setUp() {
        PasswordResetCooldown.resetForTesting()
    }

    @Test
    fun emailIsNotCoolingDownInitially() {
        // Inject the clock explicitly: the production default reads
        // SystemClock.elapsedRealtime, which is unavailable on the JVM.
        assertFalse(PasswordResetCooldown.isCoolingDown("user@example.com", nowMillis = 0L))
        assertEquals(0, PasswordResetCooldown.remainingCooldownSeconds("user@example.com", nowMillis = 0L))
    }

    @Test
    fun emailCoolsDownAfterSend() {
        val email = "user@example.com"
        val t0 = 1_000_000L
        PasswordResetCooldown.recordSent(email, nowMillis = t0)

        // Immediately after send
        assertTrue(PasswordResetCooldown.isCoolingDown(email, nowMillis = t0 + 1_000L))
        assertEquals(59, PasswordResetCooldown.remainingCooldownSeconds(email, nowMillis = t0 + 1_000L))

        // Halfway through cooldown
        assertTrue(PasswordResetCooldown.isCoolingDown(email, nowMillis = t0 + 30_000L))
        assertEquals(30, PasswordResetCooldown.remainingCooldownSeconds(email, nowMillis = t0 + 30_000L))

        // After cooldown expires
        assertFalse(PasswordResetCooldown.isCoolingDown(email, nowMillis = t0 + 60_000L))
        assertEquals(0, PasswordResetCooldown.remainingCooldownSeconds(email, nowMillis = t0 + 60_000L))
    }

    @Test
    fun emailCooldownIsNormalizedAndCaseInsensitive() {
        val t0 = 1_000_000L
        PasswordResetCooldown.recordSent("  User@Example.Com  ", nowMillis = t0)

        assertTrue(PasswordResetCooldown.isCoolingDown("user@example.com", nowMillis = t0 + 10_000L))
        assertTrue(PasswordResetCooldown.isCoolingDown("USER@EXAMPLE.COM", nowMillis = t0 + 10_000L))
    }

    @Test
    fun differentEmailIsNotCoolingDown() {
        val t0 = 1_000_000L
        PasswordResetCooldown.recordSent("user1@example.com", nowMillis = t0)

        assertFalse(PasswordResetCooldown.isCoolingDown("user2@example.com", nowMillis = t0 + 1_000L))
    }
}
