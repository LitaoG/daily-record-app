package io.github.litaog.dailyrecord

import android.app.Application
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.security.AppLockPreference
import io.github.litaog.dailyrecord.core.security.AppLockSession
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier

class DailyRecordApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Bind the durable deletion journal before any worker or composable
        // can consult the barrier.
        DeletionBarrier.configure(applicationContext)
    }

    internal val database: DailyRecordDatabase by lazy {
        DailyRecordDatabase.create(applicationContext)
    }

    internal val firebaseServices: FirebaseServices by lazy {
        FirebaseServices.create(applicationContext, database = database)
    }

    internal val appLockPreference: AppLockPreference by lazy {
        AppLockPreference(applicationContext)
    }

    /**
     * Process-wide app lock state (ADR-023). It outlives the activity, so the
     * app's own recreate() keeps an unlocked session unlocked, while a new
     * process starts locked when the lock is on.
     */
    internal val appLockSession: AppLockSession by lazy {
        AppLockSession(enabled = appLockPreference.enabled, delay = appLockPreference.delay)
    }
}
