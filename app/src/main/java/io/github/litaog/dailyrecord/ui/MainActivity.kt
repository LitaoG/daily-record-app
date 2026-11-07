package io.github.litaog.dailyrecord.ui

import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import io.github.litaog.dailyrecord.DailyRecordApplication
import io.github.litaog.dailyrecord.R
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.security.AppLockAuthenticator
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.ui.lock.AppLockScreen
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.settings.LauncherDisguiseSettings
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState

class MainActivity : ComponentActivity() {
    private lateinit var appLockAuthenticator: AppLockAuthenticator

    /** Set right before this activity's own recreate(), which is not leaving the app. */
    private var recreatingForSettings = false

    /** Re-read in onStart: the user may add or remove the screen lock while away. */
    private var deviceSecure by mutableStateOf(false)

    private val dailyRecordApp: DailyRecordApplication
        get() = application as DailyRecordApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = dailyRecordApp
        // Resolve the persisted language and terminology style before the first composition
        // so both UI and non-UI copy consumers read the same strings set.
        val terminologyStyle = TerminologyStylePreference(this).current
        AppLanguageState.current = LanguagePreference(this).current.strings(terminologyStyle)
        // The color theme follows the same rule (ADR-022): every token getter
        // reads the selected theme, so it must be set before the first frame.
        DailyRecordThemeState.current = ColorThemePreference(this).current
        // The app lock (ADR-023) registers an activity result launcher, so the
        // authenticator must exist before the activity starts.
        appLockAuthenticator = AppLockAuthenticator(this, app.appLockSession)
        deviceSecure = appLockAuthenticator.isDeviceSecure
        applyRecentsPrivacy(app.appLockPreference.enabled)
        // Home screen disguise (ADR-024): repair the launcher aliases if a
        // switch was interrupted, and name the recent-apps entry to match.
        app.launcherDisguise.reconcile()
        applyTaskIdentity(app.launcherDisguise.identity)
        setContent {
            DailyRecordTheme {
                val locked by app.appLockSession.locked.collectAsStateWithLifecycle()
                val attemptFailed by app.appLockSession.attemptFailed.collectAsStateWithLifecycle()
                val lockEnabled by app.appLockPreference.enabledState.collectAsStateWithLifecycle()
                val lockDelay by app.appLockPreference.delayState.collectAsStateWithLifecycle()
                val appLockSettings = remember(lockEnabled, lockDelay, deviceSecure) {
                    AppLockSettings(
                        enabled = lockEnabled,
                        delay = lockDelay,
                        deviceSecure = deviceSecure,
                        blocksScreenshots = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
                        onEnabledChange = ::setAppLockEnabled,
                        onDelayChange = ::setAppLockDelay,
                    )
                }
                val launcherIdentity by app.launcherDisguise.identityState.collectAsStateWithLifecycle()
                val launcherDisguiseSettings = remember(launcherIdentity) {
                    LauncherDisguiseSettings(
                        identity = launcherIdentity,
                        onIdentityChange = ::setLauncherIdentity,
                    )
                }
                // The provider is remembered with the stable app instance, so
                // recomposition never recreates FirebaseServices or its auth
                // listener. The provider itself stays lazy: Firebase is only
                // initialized when the signed-in (or login) path actually
                // reads services.
                val servicesProvider = remember(app) { app::firebaseServices }
                // While locked the content stays composed, so saves, sync and
                // account deletion keep running, but TalkBack cannot reach it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (locked) Modifier.clearAndSetSemantics { } else Modifier),
                ) {
                    DailyRecordRoot(
                        database = app.database,
                        servicesProvider = servicesProvider,
                        onLanguageChanged = { language ->
                            AppLanguageState.current = language.strings()
                            recreateForSettings()
                        },
                        onColorThemeChanged = { theme ->
                            DailyRecordThemeState.current = theme
                            recreateForSettings()
                        },
                        appLockSettings = appLockSettings,
                        launcherDisguiseSettings = launcherDisguiseSettings,
                    )
                }
                if (locked) {
                    val focusManager = LocalFocusManager.current
                    LaunchedEffect(Unit) {
                        // Close the keyboard of a field that was being edited.
                        focusManager.clearFocus(force = true)
                        // Ask once per lock, as soon as the activity is visible;
                        // after that the unlock button asks again.
                        lifecycle.withResumed { appLockAuthenticator.authenticate() }
                    }
                    AppLockScreen(
                        attemptFailed = attemptFailed,
                        onUnlock = appLockAuthenticator::authenticate,
                        onLeave = { moveTaskToBack(true) },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = dailyRecordApp
        deviceSecure = appLockAuthenticator.isDeviceSecure
        // Without a secure screen lock nothing can unlock the app, so the lock
        // turns itself off instead of keeping the user out of their records.
        if (app.appLockPreference.enabled && !deviceSecure) {
            setAppLockEnabled(false)
            Toast.makeText(this, AppCopy.AppLock.turnedOffNoScreenLock, Toast.LENGTH_LONG).show()
        }
        app.appLockSession.onForeground(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        dailyRecordApp.appLockSession.onBackground(
            nowMillis = SystemClock.elapsedRealtime(),
            changingConfigurations = isChangingConfigurations || recreatingForSettings,
        )
        super.onStop()
    }

    override fun onDestroy() {
        if (::appLockAuthenticator.isInitialized) appLockAuthenticator.cancel()
        super.onDestroy()
    }

    private fun recreateForSettings() {
        recreatingForSettings = true
        recreate()
    }

    /** Settings and the automatic switch-off both change the lock through here. */
    private fun setAppLockEnabled(enabled: Boolean) {
        if (enabled) {
            // Check again at the moment of the tap: the switch may still show
            // a screen lock that was removed after the last onStart.
            deviceSecure = appLockAuthenticator.isDeviceSecure
            if (!deviceSecure) return
        }
        val app = dailyRecordApp
        app.appLockPreference.setEnabled(enabled)
        app.appLockSession.setEnabled(enabled)
        applyRecentsPrivacy(enabled)
    }

    private fun setAppLockDelay(delay: AppLockDelay) {
        val app = dailyRecordApp
        app.appLockPreference.setDelay(delay)
        app.appLockSession.setDelay(delay)
    }

    private fun setLauncherIdentity(identity: LauncherIdentity) {
        dailyRecordApp.launcherDisguise.setIdentity(identity)
        applyTaskIdentity(identity)
    }

    /**
     * Recent apps show the label and icon of the current launcher identity, so
     * switching the disguise while the app is open takes effect there too.
     */
    private fun applyTaskIdentity(identity: LauncherIdentity) {
        val (labelRes, iconRes) = when (identity) {
            LauncherIdentity.Default -> R.string.app_name to R.mipmap.ic_launcher
            LauncherIdentity.Journal -> R.string.app_name_journal to R.mipmap.ic_launcher_journal
        }
        val label = getString(labelRes)
        val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityManager.TaskDescription.Builder().setLabel(label).setIcon(iconRes).build()
        } else {
            legacyTaskDescription(label, iconRes)
        }
        setTaskDescription(description)
    }

    /** The constructors replaced by TaskDescription.Builder in Android 13. */
    @Suppress("DEPRECATION")
    private fun legacyTaskDescription(label: String, iconRes: Int): ActivityManager.TaskDescription =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager.TaskDescription(label, iconRes)
        } else {
            // Android 8.x only takes a bitmap icon; the label is enough there.
            ActivityManager.TaskDescription(label)
        }

    /**
     * Hides the recent-apps preview while the lock is on (ADR-023). Before
     * Android 13 the only way is FLAG_SECURE, which also blocks screenshots.
     */
    private fun applyRecentsPrivacy(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!enabled)
        } else if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
