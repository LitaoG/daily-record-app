package io.github.litaog.dailyrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.ui.DailyRecordRoot
import io.github.litaog.dailyrecord.ui.LanguagePreference
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyRecordApplication
        // Resolve the persisted language before the first composition so both
        // UI and non-UI copy consumers read the same strings set.
        AppLanguageState.current = LanguagePreference(this).current.strings()
        setContent {
            DailyRecordTheme {
                // The provider is remembered with the stable app instance, so
                // recomposition never recreates FirebaseServices or its auth
                // listener. The provider itself stays lazy: Firebase is only
                // initialized when the signed-in (or login) path actually
                // reads services.
                val servicesProvider = remember(app) { app::firebaseServices }
                DailyRecordRoot(
                    database = app.database,
                    servicesProvider = servicesProvider,
                )
            }
        }
    }
}
