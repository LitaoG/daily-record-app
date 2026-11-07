package io.github.litaog.dailyrecord.ui.auth

/**
 * In-memory client-side cooldown tracker for password reset email requests.
 * Prevents rapid double-clicks, multiple dialog submissions, and accidental email storms.
 */
internal object PasswordResetCooldown {
    internal const val COOLDOWN_MILLIS = 60_000L
    private val lastSentTimestamps = mutableMapOf<String, Long>()

    @Synchronized
    fun isCoolingDown(email: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val normalized = email.trim().lowercase()
        val lastSent = lastSentTimestamps[normalized] ?: return false
        return nowMillis - lastSent < COOLDOWN_MILLIS
    }

    @Synchronized
    fun remainingCooldownSeconds(email: String, nowMillis: Long = System.currentTimeMillis()): Int {
        val normalized = email.trim().lowercase()
        val lastSent = lastSentTimestamps[normalized] ?: return 0
        val remaining = COOLDOWN_MILLIS - (nowMillis - lastSent)
        return if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
    }

    @Synchronized
    fun recordSent(email: String, nowMillis: Long = System.currentTimeMillis()) {
        val normalized = email.trim().lowercase()
        lastSentTimestamps[normalized] = nowMillis
    }

    @Synchronized
    fun resetForTesting() {
        lastSentTimestamps.clear()
    }
}
