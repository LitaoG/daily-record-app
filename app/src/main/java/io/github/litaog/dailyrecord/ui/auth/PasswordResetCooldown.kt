package io.github.litaog.dailyrecord.ui.auth

import android.os.SystemClock

/**
 * In-memory client-side cooldown tracker for password reset email requests.
 * Prevents rapid double-clicks, multiple dialog submissions, and accidental email storms.
 *
 * Timestamps use the monotonic [SystemClock.elapsedRealtime] clock: the wall
 * clock can be rolled back (extending the cooldown) or forward (bypassing it)
 * by the user or a network time correction. Tests inject [nowMillis] instead
 * of relying on the default.
 */
internal object PasswordResetCooldown {
    internal const val COOLDOWN_MILLIS = 60_000L
    private val lastSentTimestamps = mutableMapOf<String, Long>()

    @Synchronized
    fun isCoolingDown(email: String, nowMillis: Long = SystemClock.elapsedRealtime()): Boolean {
        val normalized = email.trim().lowercase()
        val lastSent = lastSentTimestamps[normalized] ?: return false
        return nowMillis - lastSent < COOLDOWN_MILLIS
    }

    @Synchronized
    fun remainingCooldownSeconds(email: String, nowMillis: Long = SystemClock.elapsedRealtime()): Int {
        val normalized = email.trim().lowercase()
        val lastSent = lastSentTimestamps[normalized] ?: return 0
        val remaining = COOLDOWN_MILLIS - (nowMillis - lastSent)
        return if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
    }

    @Synchronized
    fun recordSent(email: String, nowMillis: Long = SystemClock.elapsedRealtime()) {
        val normalized = email.trim().lowercase()
        lastSentTimestamps[normalized] = nowMillis
    }

    @Synchronized
    fun resetForTesting() {
        lastSentTimestamps.clear()
    }
}
