package io.github.litaog.dailyrecord.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DailyRecordDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun versionOneHandBrewDataMigratesToDedicatedVersionTwoTable() = runBlocking {
        migrationHelper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO activities (
                    id, owner_id, name, icon_key, color_argb, measurement_type,
                    unit, sort_order, is_archived, created_at, updated_at, revision
                ) VALUES ('hand-brew', 'local-owner', '手冲', 'flight', 1, 'COUNT',
                          '次', 0, 0, 1000, 1000, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO activities (
                    id, owner_id, name, icon_key, color_argb, measurement_type,
                    unit, sort_order, is_archived, created_at, updated_at, revision
                ) VALUES ('legacy-other', 'local-owner', '旧记录', 'legacy', 2, 'BOOLEAN',
                          NULL, 1, 0, 1000, 1000, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO daily_records (
                    id, owner_id, activity_id, local_date, status, quantity,
                    timezone_id, occurred_at, created_at, updated_at, revision
                ) VALUES ('brew-record', 'local-owner', 'hand-brew', '2026-07-16',
                          'DONE', 3, 'Asia/Shanghai', 1000, 1000, 1000, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO daily_records (
                    id, owner_id, activity_id, local_date, status, quantity,
                    timezone_id, occurred_at, created_at, updated_at, revision
                ) VALUES ('legacy-record', 'local-owner', 'legacy-other', '2026-07-16',
                          'DONE', NULL, 'Asia/Shanghai', 1000, 1000, 1000, 0)
                """.trimIndent(),
            )
            close()
        }

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DailyRecordDatabase::class.java,
            TEST_DATABASE,
        ).addMigrations(*DailyRecordDatabase.MIGRATIONS).build()

        try {
            val migrated = database.handBrewRecordDao()
                .getByDate(java.time.LocalDate.of(2026, 7, 16))
            assertNotNull(migrated)
            assertEquals(3, migrated?.brewCount)
            assertEquals(2, database.openHelper.readableDatabase.version)

            database.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'legacy_activities_v1'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
            database.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'legacy_daily_records_v1'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
