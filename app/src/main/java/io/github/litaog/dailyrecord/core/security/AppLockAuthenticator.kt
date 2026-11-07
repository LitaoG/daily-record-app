package io.github.litaog.dailyrecord.core.security

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import io.github.litaog.dailyrecord.core.common.AppCopy

/**
 * Asks the system to confirm the user for the app lock (ADR-023) without a new
 * dependency: Android 10+ uses the platform BiometricPrompt with the screen
 * lock as fallback, Android 8-9 use the system screen-lock confirmation.
 *
 * Create it in `onCreate`: the screen-lock confirmation registers an activity
 * result launcher, which must happen before the activity starts. Results go to
 * the application-scoped [AppLockSession], so a rotation while the system UI
 * is open cannot lose them.
 */
internal class AppLockAuthenticator(
    private val activity: ComponentActivity,
    private val session: AppLockSession,
) {
    private val keyguardManager: KeyguardManager? =
        activity.getSystemService(KeyguardManager::class.java)
    private var cancellation: CancellationSignal? = null

    /** One system prompt at a time per activity; repeated taps are ignored. */
    private var inFlight = false

    // The check guards against FragmentActivity before androidx.fragment
    // 1.3.0. Fragment 1.1.0 only arrives transitively (Firebase Auth ->
    // play-services-auth 20.7.0); MainActivity is a plain ComponentActivity,
    // whose own ActivityResultRegistry handles this launcher (ADR-023).
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val confirmScreenLock = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        inFlight = false
        session.onAuthenticationFinished(success = result.resultCode == Activity.RESULT_OK)
    }

    /** True when the device has a PIN, pattern or password screen lock. */
    val isDeviceSecure: Boolean
        get() = keyguardManager?.isDeviceSecure == true

    fun authenticate() {
        if (inFlight || !session.locked.value) return
        val request = session.onAuthenticationStarted() ?: return
        inFlight = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBiometricPrompt(request)
        } else {
            showScreenLockConfirmation()
        }
    }

    /** Closes a prompt that is still open, for example when the activity is destroyed. */
    fun cancel() {
        cancellation?.cancel()
        cancellation = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBiometricPrompt(request: Int) {
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(AppCopy.AppLock.promptTitle)
            .setSubtitle(AppCopy.AppLock.promptSubtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
        } else {
            // Android 10: biometrics with the screen lock as fallback. A negative
            // button must not be set together with this option.
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        val signal = CancellationSignal()
        cancellation = signal
        val started = runCatching {
            builder.build().authenticate(
                signal,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        authResult: BiometricPrompt.AuthenticationResult,
                    ) {
                        finish(request, success = true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Cancelled, locked out or no screen lock: stay locked;
                        // the lock screen offers the unlock button again.
                        finish(request, success = false)
                    }

                    // onAuthenticationFailed is one rejected attempt while the
                    // prompt stays open, so it is not a final answer.
                },
            )
        }
        if (started.isFailure) finish(request, success = false)
    }

    private fun finish(request: Int, success: Boolean) {
        inFlight = false
        cancellation = null
        session.onAuthenticationFinished(success = success, request = request)
    }

    @Suppress("DEPRECATION")
    private fun showScreenLockConfirmation() {
        // Deprecated from Android 10, which uses BiometricPrompt instead.
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
            AppCopy.AppLock.promptTitle,
            AppCopy.AppLock.promptSubtitle,
        )
        if (intent == null) {
            // No secure screen lock: nothing can confirm the user.
            inFlight = false
            session.onAuthenticationFinished(success = false)
            return
        }
        runCatching { confirmScreenLock.launch(intent) }.onFailure {
            inFlight = false
            session.onAuthenticationFinished(success = false)
        }
    }
}
