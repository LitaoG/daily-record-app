package io.github.litaog.dailyrecord

import android.app.Application
import io.github.litaog.dailyrecord.core.cloud.FirebaseServices
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase

class DailyRecordApplication : Application() {
    internal val database: DailyRecordDatabase by lazy {
        DailyRecordDatabase.create(applicationContext)
    }

    internal val firebaseServices: FirebaseServices by lazy {
        FirebaseServices.create(applicationContext)
    }
}
