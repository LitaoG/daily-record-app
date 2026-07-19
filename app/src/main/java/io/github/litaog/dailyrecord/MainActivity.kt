package io.github.litaog.dailyrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.litaog.dailyrecord.ui.DailyRecordRoot
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyRecordApplication
        setContent {
            DailyRecordTheme {
                DailyRecordRoot(database = app.database, services = app.firebaseServices)
            }
        }
    }
}
