package io.github.litaog.dailyrecord.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Frozen historical value: the v1 hand-brew activity was identified by this
 * machine key in released data. Migration SQL must use the key rather than a
 * user-facing name, otherwise a future wording or localization change could
 * silently stop matching rows and lose v1 hand-brew records.
 */
private const val HAND_BREW_LEGACY_ICON_KEY = "flight"

@Database(
    entities = [
        HandBrewRecordEntity::class,
        HandBrewRecordDetailEntity::class,
        SexRecordEntity::class,
        SexRecordDetailEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
internal abstract class DailyRecordDatabase : RoomDatabase() {
    abstract fun handBrewRecordDao(): HandBrewRecordDao
    abstract fun handBrewRecordDetailDao(): HandBrewRecordDetailDao
    abstract fun sexRecordDao(): SexRecordDao
    abstract fun sexRecordDetailDao(): SexRecordDetailDao

    companion object {
        const val DATABASE_NAME = "daily-record.db"
        const val SCHEMA_VERSION = 5

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
                      AND a.`icon_key` = '$HAND_BREW_LEGACY_ICON_KEY'
                      -- v1 kept a row per interacted date and defaulted status
                      -- to 'UNSET'; never migrate an unset date into an
                      -- explicit zero-count record (it would change the
                      -- "recorded day" statistics for the whole history).
                      -- quantity must be present too: a non-UNSET row without
                      -- a quantity is not evidence of an explicit zero.
                      AND r.`status` != 'UNSET'
                      AND r.`quantity` IS NOT NULL
                    GROUP BY r.`local_date`
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hand_brew_records_local_date` " +
                        "ON `hand_brew_records` (`local_date`)",
                )

                // Keep the deprecated generic v1 tables as read-only recovery
                // evidence. The generic activity model was never productized,
                // but its released v1 hand-brew rows were migrated above; the
                // legacy tables are no longer referenced by product code and
                // can be removed in a later audited migration.
                db.execSQL("ALTER TABLE `daily_records` RENAME TO `legacy_daily_records_v1`")
                db.execSQL("ALTER TABLE `activities` RENAME TO `legacy_activities_v1`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hand_brew_records_v3` (
                        `id` TEXT NOT NULL,
                        `local_date` TEXT NOT NULL,
                        `owner_id` TEXT NOT NULL DEFAULT '__local__',
                        `brew_count` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0,
                        `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                        `remote_revision` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `hand_brew_records_v3` (
                        `id`, `local_date`, `owner_id`, `brew_count`, `created_at`,
                        `updated_at`, `is_deleted`, `sync_state`, `remote_revision`
                    )
                    SELECT `id`, `local_date`, '__local__', `brew_count`, `created_at`,
                           `updated_at`, 0, 'PENDING', 0
                    FROM `hand_brew_records`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `hand_brew_records`")
                db.execSQL("ALTER TABLE `hand_brew_records_v3` RENAME TO `hand_brew_records`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hand_brew_records_owner_id_local_date` " +
                        "ON `hand_brew_records` (`owner_id`, `local_date`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_hand_brew_records_owner_id_sync_state` " +
                        "ON `hand_brew_records` (`owner_id`, `sync_state`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sex_records` (
                        `id` TEXT NOT NULL,
                        `local_date` TEXT NOT NULL,
                        `owner_id` TEXT NOT NULL DEFAULT '__local__',
                        `sex_count` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0,
                        `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                        `remote_revision` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sex_records_owner_id_local_date` " +
                        "ON `sex_records` (`owner_id`, `local_date`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sex_records_owner_id_sync_state` " +
                        "ON `sex_records` (`owner_id`, `sync_state`)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hand_brew_record_details` (
                        `id` TEXT NOT NULL,
                        `local_date` TEXT NOT NULL,
                        `owner_id` TEXT NOT NULL DEFAULT '__local__',
                        `occurrence_index` INTEGER NOT NULL,
                        `start_time` TEXT,
                        `end_time` TEXT,
                        `feeling` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_hand_brew_record_details_owner_id_local_date_occurrence_index`
                    ON `hand_brew_record_details` (`owner_id`, `local_date`, `occurrence_index`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_hand_brew_record_details_owner_id_local_date`
                    ON `hand_brew_record_details` (`owner_id`, `local_date`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sex_record_details` (
                        `id` TEXT NOT NULL,
                        `local_date` TEXT NOT NULL,
                        `owner_id` TEXT NOT NULL DEFAULT '__local__',
                        `occurrence_index` INTEGER NOT NULL,
                        `start_time` TEXT,
                        `end_time` TEXT,
                        `feeling` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_sex_record_details_owner_id_local_date_occurrence_index`
                    ON `sex_record_details` (`owner_id`, `local_date`, `occurrence_index`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_sex_record_details_owner_id_local_date`
                    ON `sex_record_details` (`owner_id`, `local_date`)
                    """.trimIndent(),
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
        )

        fun create(context: Context): DailyRecordDatabase = Room.databaseBuilder(
            context.applicationContext,
            DailyRecordDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(*MIGRATIONS).build()
    }
}
