package io.github.litaog.dailyrecord

import android.app.Application
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
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
}
