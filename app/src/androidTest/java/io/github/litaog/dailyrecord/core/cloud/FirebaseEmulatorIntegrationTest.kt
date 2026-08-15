package io.github.litaog.dailyrecord.core.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import io.github.litaog.dailyrecord.core.common.awaitResult
import io.github.litaog.dailyrecord.core.di.FIREBASE_EMULATOR_APP_NAME
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import io.github.litaog.dailyrecord.core.auth.AuthDeletionResult
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import io.github.litaog.dailyrecord.core.sync.RemoteHandBrewRecord
import io.github.litaog.dailyrecord.core.sync.RemoteSexRecord

@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorIntegrationTest {
    @Test
    fun passwordAccountRestoresRecordAndRulesBlockOtherAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        assertTrue("Emulator tests must never inherit production Firebase identity", !services.productionConfigured)
        services.authRepository.signOut()
        try {
            val suffix = UUID.randomUUID().toString().take(10)
            val firstEmail = "first-$suffix@example.com"
            val secondEmail = "second-$suffix@example.com"
            val password = "test-password-2026"
            val resetPassword = "reset-password-2026"
            services.authRepository.sendPasswordResetEmail("missing-$suffix@example.com")
            val first = services.authRepository.register(firstEmail, password)
            val date = LocalDate.of(2026, 7, 16)
            val local = HandBrewRecordEntity(
                id = "firebase-$suffix",
                localDate = date,
                ownerId = first.uid,
                brewCount = 3,
                createdAt = Instant.parse("2026-07-16T08:00:00Z"),
                updatedAt = Instant.parse("2026-07-16T08:00:01Z"),
                isDeleted = false,
                syncState = SYNC_PENDING,
                remoteRevision = 0,
            )

            val committed = services.remoteDataSource.commit(first.uid, local)
            assertEquals(1L, committed.revision)
            services.sexRemoteDataSource.commit(
                first.uid,
                SexRecordEntity(
                    id = "firebase-sex-$suffix",
                    localDate = date,
                    ownerId = first.uid,
                    sexCount = 1,
                    createdAt = Instant.parse("2026-07-16T08:00:00Z"),
                    updatedAt = Instant.parse("2026-07-16T08:00:01Z"),
                    isDeleted = false,
                    syncState = SYNC_PENDING,
                    remoteRevision = 0,
                ),
            )
            services.authRepository.signOut()
            services.authRepository.sendPasswordResetEmail(firstEmail)
            val oobCode = passwordResetCodeFor(firstEmail)
            confirmPasswordReset(oobCode, resetPassword)
            val restoredAccount = services.authRepository.signIn(firstEmail, resetPassword)
            assertEquals(first.uid, restoredAccount.uid)
            val restored = services.remoteDataSource.fetch(first.uid).records
                .filterIsInstance<RemoteHandBrewRecord>().single()
            assertEquals(3, restored.brewCount)
            assertEquals(
                1,
                services.sexRemoteDataSource.fetch(first.uid).records.filterIsInstance<RemoteSexRecord>().single().sexCount,
            )

            val newer = local.copy(
                brewCount = 5,
                updatedAt = Instant.parse("2026-07-16T08:00:03Z"),
                remoteRevision = 1,
            )
            val stale = local.copy(
                brewCount = 2,
                updatedAt = Instant.parse("2026-07-16T09:00:00Z"),
                remoteRevision = 1,
            )
            assertEquals(5, services.remoteDataSource.commit(first.uid, newer).brewCount)
            val rejectedStaleCommit = services.remoteDataSource.commit(first.uid, stale)
            assertEquals(5, rejectedStaleCommit.brewCount)
            assertEquals(2L, rejectedStaleCommit.revision)
            assertEquals(5, services.remoteDataSource.fetch(first.uid).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)

            services.authRepository.signOut()
            services.authRepository.register(secondEmail, password)
            val crossAccountRead = runCatching { services.remoteDataSource.fetch(first.uid) }
            assertTrue("A different account must not read the first account", crossAccountRead.isFailure)
            assertTrue(
                "A different account must not read the first account's sex records",
                runCatching { services.sexRemoteDataSource.fetch(first.uid) }.isFailure,
            )
        } finally {
            services.authRepository.signOut()
        }
    }

    @Test
    fun accountDeletionRemovesCloudRecordsBeforeDeletingAuthenticationAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        val suffix = UUID.randomUUID().toString().take(10)
        val email = "delete-$suffix@example.com"
        val password = "test-password-2026"
        try {
            val account = services.authRepository.register(email, password)
            services.remoteDataSource.commit(
                account.uid,
                HandBrewRecordEntity(
                    id = "delete-$suffix",
                    localDate = LocalDate.of(2026, 7, 18),
                    ownerId = account.uid,
                    brewCount = 2,
                    createdAt = Instant.parse("2026-07-18T08:00:00Z"),
                    updatedAt = Instant.parse("2026-07-18T08:00:01Z"),
                    isDeleted = false,
                    syncState = SYNC_PENDING,
                    remoteRevision = 0,
                ),
            )
            services.sexRemoteDataSource.commit(
                account.uid,
                SexRecordEntity(
                    id = "delete-sex-$suffix",
                    localDate = LocalDate.of(2026, 7, 18),
                    ownerId = account.uid,
                    sexCount = 1,
                    createdAt = Instant.parse("2026-07-18T08:00:00Z"),
                    updatedAt = Instant.parse("2026-07-18T08:00:01Z"),
                    isDeleted = false,
                    syncState = SYNC_PENDING,
                    remoteRevision = 0,
                ),
            )

            services.authRepository.reauthenticate(password)
            services.accountDataDeletionStore.deleteAll(account.uid)
            assertTrue(services.remoteDataSource.fetch(account.uid).records.isEmpty())
            assertTrue(services.sexRemoteDataSource.fetch(account.uid).records.filterIsInstance<RemoteSexRecord>().isEmpty())
            assertTrue(services.authRepository.deleteCurrentAccount() is AuthDeletionResult.Completed)

            assertTrue(
                "Deleted Firebase account must not accept the old credentials",
                runCatching { services.authRepository.signIn(email, password) }.isFailure,
            )
        } finally {
            services.authRepository.signOut()
        }
    }

    @Test
    fun trustedWriteCallableRejectsMalformedDetailBeforeWriting() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        val suffix = UUID.randomUUID().toString().take(10)
        try {
            val account = services.authRepository.register(
                "callable-validation-$suffix@example.com",
                "test-password-2026",
            )
            val functions = FirebaseFunctions.getInstance(
                FirebaseApp.getInstance(FIREBASE_EMULATOR_APP_NAME),
            )
            val result = runCatching {
                functions.getHttpsCallable("writeDailyCountRecord")
                    .call(
                        mapOf(
                            "collection" to "handBrewRecords",
                            "localDate" to "2026-07-21",
                            "id" to "callable-validation-$suffix",
                            "count" to 1L,
                            "createdAtMillis" to Instant.parse("2026-07-21T08:00:00Z").toEpochMilli(),
                            "clientUpdatedAtMillis" to Instant.parse("2026-07-21T08:00:01Z").toEpochMilli(),
                            "deleted" to false,
                            "remoteRevision" to 0L,
                            "details" to listOf(
                                mapOf(
                                    "id" to "detail-$suffix",
                                    "occurrenceIndex" to 1L,
                                    "startTime" to "09:00",
                                    "endTime" to "08:00",
                                    "feeling" to "",
                                ),
                            ),
                        ),
                    )
                    .awaitResult()
            }
            val error = result.exceptionOrNull()
            assertTrue(error is FirebaseFunctionsException)
            assertEquals(
                FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                (error as FirebaseFunctionsException).code,
            )
            assertTrue(services.remoteDataSource.fetch(account.uid).records.isEmpty())
        } finally {
            services.authRepository.signOut()
        }
    }

    @Test
    fun missingDocumentsRecreateBothModulesWithANewCloudGeneration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        val suffix = UUID.randomUUID().toString().take(10)
        val email = "recreate-$suffix@example.com"
        val password = "test-password-2026"
        try {
            val account = services.authRepository.register(email, password)
            val date = LocalDate.of(2026, 7, 19)
            val handLocal = HandBrewRecordEntity(
                id = "recreate-hand-$suffix",
                localDate = date,
                ownerId = account.uid,
                brewCount = 3,
                createdAt = Instant.parse("2026-07-19T08:00:00Z"),
                updatedAt = Instant.parse("2026-07-19T08:00:01Z"),
                isDeleted = false,
                syncState = SYNC_PENDING,
                remoteRevision = 0,
            )
            val sexLocal = SexRecordEntity(
                id = "recreate-sex-$suffix",
                localDate = date,
                ownerId = account.uid,
                sexCount = 2,
                createdAt = Instant.parse("2026-07-19T08:00:00Z"),
                updatedAt = Instant.parse("2026-07-19T08:00:01Z"),
                isDeleted = false,
                syncState = SYNC_PENDING,
                remoteRevision = 0,
            )
            val initialHand = services.remoteDataSource.commit(account.uid, handLocal)
            val initialSex = services.sexRemoteDataSource.commit(account.uid, sexLocal)

            services.accountDataDeletionStore.deleteAll(account.uid)

            val recreatedHand = services.remoteDataSource.commit(
                account.uid,
                handLocal.copy(
                    id = initialHand.id,
                    brewCount = 4,
                    updatedAt = Instant.parse("2026-07-19T08:00:02Z"),
                    remoteRevision = initialHand.revision,
                ),
            )
            val recreatedSex = services.sexRemoteDataSource.commit(
                account.uid,
                sexLocal.copy(
                    id = initialSex.id,
                    sexCount = 3,
                    updatedAt = Instant.parse("2026-07-19T08:00:02Z"),
                    remoteRevision = initialSex.revision,
                ),
            )

            assertNotEquals(initialHand.id, recreatedHand.id)
            assertNotEquals(initialSex.id, recreatedSex.id)
            assertEquals(1L, recreatedHand.revision)
            assertEquals(1L, recreatedSex.revision)
            assertEquals(4, services.remoteDataSource.fetch(account.uid).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
            assertEquals(3, services.sexRemoteDataSource.fetch(account.uid).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
        } finally {
            services.authRepository.signOut()
        }
    }

    private fun assertAuthEmulatorReachable() {
        val connection = URL(
            "http://10.0.2.2:9099/emulator/v1/projects/demo-daily-record-app/config",
        ).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }

    private fun passwordResetCodeFor(email: String): String {
        val connection = URL(
            "http://10.0.2.2:9099/emulator/v1/projects/demo-daily-record-app/oobCodes",
        ).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            assertEquals(200, connection.responseCode)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val codes = JSONObject(body).getJSONArray("oobCodes")
            (0 until codes.length())
                .asSequence()
                .map(codes::getJSONObject)
                .first { it.optString("email") == email && it.optString("requestType") == "PASSWORD_RESET" }
                .getString("oobCode")
        } finally {
            connection.disconnect()
        }
    }

    private fun confirmPasswordReset(oobCode: String, newPassword: String) {
        val connection = URL(
            "http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:resetPassword" +
                "?key=daily-record-emulator-key",
        ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(JSONObject().put("oobCode", oobCode).put("newPassword", newPassword).toString())
            }
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }
}
