package io.github.litaog.dailyrecord.core.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import io.github.litaog.dailyrecord.core.common.awaitResult
import io.github.litaog.dailyrecord.core.di.FIREBASE_EMULATOR_APP_NAME
import io.github.litaog.dailyrecord.core.di.FIREBASE_FUNCTIONS_REGION
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

internal class FirebaseEmulatorTrustedWriteTest {

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
                FIREBASE_FUNCTIONS_REGION,
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
    fun trustedWriteCallableRejectsOversizedDetailListBeforeWriting() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        val suffix = UUID.randomUUID().toString().take(10)
        try {
            val account = services.authRepository.register(
                "callable-size-$suffix@example.com",
                "test-password-2026",
            )
            val functions = FirebaseFunctions.getInstance(
                FirebaseApp.getInstance(FIREBASE_EMULATOR_APP_NAME),
                FIREBASE_FUNCTIONS_REGION,
            )
            val oversizedDetails = (1..1001).map { occurrenceIndex ->
                mapOf(
                    "id" to "detail-$suffix-$occurrenceIndex",
                    "occurrenceIndex" to occurrenceIndex.toLong(),
                    "startTime" to null,
                    "endTime" to null,
                    "feeling" to "",
                )
            }
            val result = runCatching {
                functions.getHttpsCallable("writeDailyCountRecord")
                    .call(
                        mapOf(
                            "collection" to "handBrewRecords",
                            "localDate" to "2026-07-22",
                            "id" to "callable-size-$suffix",
                            "count" to 1001L,
                            "createdAtMillis" to Instant.parse("2026-07-22T08:00:00Z").toEpochMilli(),
                            "clientUpdatedAtMillis" to Instant.parse("2026-07-22T08:00:01Z").toEpochMilli(),
                            "deleted" to false,
                            "remoteRevision" to 0L,
                            "details" to oversizedDetails,
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
    fun trustedWriteCallableRejectsPrototypePollutedCollection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        val suffix = UUID.randomUUID().toString().take(10)
        try {
            val account = services.authRepository.register(
                "callable-proto-$suffix@example.com",
                "test-password-2026",
            )
            val functions = FirebaseFunctions.getInstance(
                FirebaseApp.getInstance(FIREBASE_EMULATOR_APP_NAME),
                FIREBASE_FUNCTIONS_REGION,
            )
            // Object.prototype keys ("constructor", "toString", "__proto__") must not
            // pass the callable collection allow-list: MODULES inherits them, so a
            // truthiness lookup would let Admin SDK writes escape to same-UID
            // collections that triggers and account deletion never clean up.
            listOf("constructor", "toString", "__proto__").forEach { collection ->
                val result = runCatching {
                    functions.getHttpsCallable("writeDailyCountRecord")
                        .call(
                            mapOf(
                                "collection" to collection,
                                "localDate" to "2026-07-23",
                                "id" to "callable-proto-$suffix",
                                "count" to 1L,
                                "createdAtMillis" to Instant.parse("2026-07-23T08:00:00Z").toEpochMilli(),
                                "clientUpdatedAtMillis" to Instant.parse("2026-07-23T08:00:01Z").toEpochMilli(),
                                "deleted" to false,
                                "remoteRevision" to 0L,
                                "details" to emptyList<Map<String, Any?>>(),
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
            }
            assertTrue(services.remoteDataSource.fetch(account.uid).records.isEmpty())
            assertTrue(services.sexRemoteDataSource.fetch(account.uid).records.isEmpty())
        } finally {
            services.authRepository.signOut()
        }
    }

}
