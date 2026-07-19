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
    fun versionOneHandBrewDataMigratesThroughCurrentSchema() = runBlocking {
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
                .getByDate(LOCAL_OWNER_ID, java.time.LocalDate.of(2026, 7, 16))
            assertNotNull(migrated)
            assertEquals(3, migrated?.brewCount)
            assertEquals(LOCAL_OWNER_ID, migrated?.ownerId)
            assertEquals(SYNC_PENDING, migrated?.syncState)
            assertEquals(3, database.openHelper.readableDatabase.version)

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

    @Test
    @Throws(IOException::class)
    fun versionTwoRecordsGainPendingSyncMetadataWithoutDataLoss() = runBlocking {
        migrationHelper.createDatabase(V2_TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO hand_brew_records (
                    id, local_date, brew_count, created_at, updated_at
                ) VALUES ('old-local', '2024-02-29', 4, 1000, 2000)
                """.trimIndent(),
            )
            close()
        }

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DailyRecordDatabase::class.java,
            V2_TEST_DATABASE,
        ).addMigrations(*DailyRecordDatabase.MIGRATIONS).build()

        try {
            val migrated = database.handBrewRecordDao().getByDate(
                LOCAL_OWNER_ID,
                java.time.LocalDate.of(2024, 2, 29),
            )
            assertNotNull(migrated)
            assertEquals(4, migrated?.brewCount)
            assertEquals(false, migrated?.isDeleted)
            assertEquals(SYNC_PENDING, migrated?.syncState)
            assertEquals(0L, migrated?.remoteRevision)
            assertEquals(3, database.openHelper.readableDatabase.version)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
        const val V2_TEST_DATABASE = "migration-v2-test.db"
    }
}
