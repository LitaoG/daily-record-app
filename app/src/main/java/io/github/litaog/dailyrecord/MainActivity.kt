package io.github.litaog.dailyrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.ui.ColorThemePreference
import io.github.litaog.dailyrecord.ui.DailyRecordRoot
import io.github.litaog.dailyrecord.ui.LanguagePreference
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyRecordApplication
        // Resolve the persisted language before the first composition so both
        // UI and non-UI copy consumers read the same strings set.
        AppLanguageState.current = LanguagePreference(this).current.strings()
        // The color theme follows the same rule (ADR-022): every token getter
        // reads the selected theme, so it must be set before the first frame.
        DailyRecordThemeState.current = ColorThemePreference(this).current
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
                    onLanguageChanged = { language ->
                        AppLanguageState.current = language.strings()
                        recreate()
                    },
                    onColorThemeChanged = { theme ->
                        DailyRecordThemeState.current = theme
                        recreate()
                    },
                )
            }
        }
    }
}
