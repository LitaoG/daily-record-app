package io.github.litaog.dailyrecord.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        ActivityEntity::class,
        DailyRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
internal abstract class DailyRecordDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    abstract fun dailyRecordDao(): DailyRecordDao

    companion object {
        const val DATABASE_NAME = "daily-record.db"

        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): DailyRecordDatabase = Room.databaseBuilder(
            context.applicationContext,
            DailyRecordDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(*MIGRATIONS).build()
    }
}
