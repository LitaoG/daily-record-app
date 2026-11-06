package io.github.litaog.dailyrecord.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How long the app may stay in the background before it locks again (ADR-023). */
enum class AppLockDelay(val id: String, val millis: Long) {
    Immediately("immediately", 0L),
    OneMinute("one_minute", 60_000L),
    FiveMinutes("five_minutes", 5 * 60_000L),
    ;

    companion object {
        val Default: AppLockDelay = Immediately

        /** Persisted ids never change; an unknown id falls back to the default. */
        fun fromId(id: String?): AppLockDelay = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * In-memory app lock state (ADR-023).
 *
 * Pure Kotlin so the rules are covered by JVM tests: the activity feeds it
 * lifecycle events with a monotonic clock (`SystemClock.elapsedRealtime`) and
 * the lock screen observes [locked]. It lives in the Application, so the
 * app's own `recreate()` for a language or theme change keeps the state,
 * while a new process starts locked again.
 *
 * Rules:
 * - A cold start is locked when the lock is enabled.
 * - Leaving the foreground records the time; returning locks again once the
 *   chosen [delay] has passed.
 * - Configuration changes, the app's own `recreate()` and the authentication
 *   UI stopping the activity never count as leaving the app.
 * - Only a successful authentication unlocks.
 */
class AppLockSession(
    enabled: Boolean,
    delay: AppLockDelay = AppLockDelay.Default,
) {
    var enabled: Boolean = enabled
        private set

    var delay: AppLockDelay = delay
        private set

    private val _locked = MutableStateFlow(enabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _attemptFailed = MutableStateFlow(false)

    /** True after the latest attempt ended without success; the lock screen says so. */
    val attemptFailed: StateFlow<Boolean> = _attemptFailed.asStateFlow()

    private var foreground = false
    private var backgroundedAtMillis: Long? = null
    private var authInProgress = false
    private var authResultArrived = false
    private var authRequest = 0

    /** The activity left the foreground (`onStop`). */
    fun onBackground(nowMillis: Long, changingConfigurations: Boolean) {
        foreground = false
        if (changingConfigurations || authInProgress) return
        if (enabled && !_locked.value) backgroundedAtMillis = nowMillis
    }

    /** The activity is back in the foreground (`onStart`). */
    fun onForeground(nowMillis: Long) {
        foreground = true
        if (authInProgress) {
            // Coming back from the authentication UI is not a new visit.
            if (authResultArrived) clearAuthentication()
            return
        }
        val leftAt = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        val away = nowMillis - leftAt
        // A clock that appears to run backwards counts as long enough.
        if (enabled && (away < 0L || away >= delay.millis)) {
            _locked.value = true
            _attemptFailed.value = false
        }
    }

    /**
     * The lock screen asked the system to authenticate the user. Returns the
     * id of this request, or null when the app is not locked.
     */
    fun onAuthenticationStarted(): Int? {
        if (!_locked.value) return null
        authRequest += 1
        authInProgress = true
        authResultArrived = false
        _attemptFailed.value = false
        return authRequest
    }

    /**
     * The system answered. [request] identifies the answered request; null
     * means the latest one. Results without a started request, or for an
     * earlier request (a prompt cancelled by a rotation), are ignored.
     */
    fun onAuthenticationFinished(success: Boolean, request: Int? = null) {
        if (!authInProgress) return
        if (request != null && request != authRequest) return
        authResultArrived = true
        _attemptFailed.value = !success
        if (success) {
            _locked.value = false
            backgroundedAtMillis = null
        }
        // While the authentication UI still covers the activity, wait for
        // the next onForeground so its stop/start pair cannot relock.
        if (foreground) clearAuthentication()
    }

    /**
     * Turning the lock on never locks the person who is using the app; turning
     * it off (including the automatic switch-off when the device no longer has
     * a secure lock screen) unlocks at once.
     */
    fun setEnabled(value: Boolean) {
        enabled = value
        backgroundedAtMillis = null
        if (!value) {
            _locked.value = false
            _attemptFailed.value = false
            clearAuthentication()
        }
    }

    fun setDelay(value: AppLockDelay) {
        delay = value
    }

    private fun clearAuthentication() {
        authInProgress = false
        authResultArrived = false
    }
}
