package io.github.litaog.dailyrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.ui.HandBrewApp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme

class MainActivity : ComponentActivity() {
    private val database by lazy { DailyRecordDatabase.create(applicationContext) }
    private val repository by lazy { RoomHandBrewRecordRepository(database) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyRecordTheme {
                HandBrewApp(repository = repository)
            }
        }
    }
}
