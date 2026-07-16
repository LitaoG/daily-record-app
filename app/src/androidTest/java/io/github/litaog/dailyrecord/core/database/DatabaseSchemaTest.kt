package io.github.litaog.dailyrecord.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
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
    fun versionOneSchemaCanBeCreatedAndOpenedByRoom() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DailyRecordDatabase::class.java,
            TEST_DATABASE,
        ).addMigrations(*DailyRecordDatabase.MIGRATIONS).build()
        try {
            database.openHelper.writableDatabase
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
