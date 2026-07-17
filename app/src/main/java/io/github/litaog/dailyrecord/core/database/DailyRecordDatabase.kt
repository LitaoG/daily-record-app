package io.github.litaog.dailyrecord.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HandBrewRecordEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
internal abstract class DailyRecordDatabase : RoomDatabase() {
    abstract fun handBrewRecordDao(): HandBrewRecordDao

    companion object {
        const val DATABASE_NAME = "daily-record.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hand_brew_records` (
                        `id` TEXT NOT NULL,
                        `local_date` TEXT NOT NULL,
                        `brew_count` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `hand_brew_records` (
                        `id`, `local_date`, `brew_count`, `created_at`, `updated_at`
                    )
                    SELECT MIN(r.`id`),
                           r.`local_date`,
                           CAST(SUM(CASE WHEN r.`quantity` > 0 THEN r.`quantity` ELSE 0 END) AS INTEGER),
                           MIN(r.`created_at`),
                           MAX(r.`updated_at`)
                    FROM `daily_records` r
                    INNER JOIN `activities` a
                        ON a.`owner_id` = r.`owner_id` AND a.`id` = r.`activity_id`
                    WHERE r.`deleted_at` IS NULL
                      AND (a.`name` = '手冲' OR a.`icon_key` = 'flight')
                    GROUP BY r.`local_date`
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hand_brew_records_local_date` " +
                        "ON `hand_brew_records` (`local_date`)",
                )

                // Keep the unreleased generic v1 tables as read-only recovery evidence.
                // They are no longer referenced by product code and can be removed in a
                // later audited migration after the hand-brew extraction is verified.
                db.execSQL("ALTER TABLE `daily_records` RENAME TO `legacy_daily_records_v1`")
                db.execSQL("ALTER TABLE `activities` RENAME TO `legacy_activities_v1`")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun create(context: Context): DailyRecordDatabase = Room.databaseBuilder(
            context.applicationContext,
            DailyRecordDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(*MIGRATIONS).build()
    }
}
