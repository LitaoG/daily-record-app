package io.github.litaog.dailyrecord.core.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorIntegrationTest {
    @Test
    fun passwordAccountRestoresRecordAndRulesBlockOtherAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        try {
            val suffix = UUID.randomUUID().toString().take(10)
            val firstEmail = "first-$suffix@example.com"
            val secondEmail = "second-$suffix@example.com"
            val password = "test-password-2026"
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
            services.authRepository.signOut()
            val restoredAccount = services.authRepository.signIn(firstEmail, password)
            assertEquals(first.uid, restoredAccount.uid)
            val restored = services.remoteDataSource.fetch(first.uid).records.single()
            assertEquals(3, restored.brewCount)

            val newer = local.copy(
                brewCount = 5,
                updatedAt = Instant.parse("2026-07-16T08:00:03Z"),
            )
            val stale = local.copy(
                brewCount = 2,
                updatedAt = Instant.parse("2026-07-16T08:00:02Z"),
            )
            assertEquals(5, services.remoteDataSource.commit(first.uid, newer).brewCount)
            val rejectedStaleCommit = services.remoteDataSource.commit(first.uid, stale)
            assertEquals(5, rejectedStaleCommit.brewCount)
            assertEquals(2L, rejectedStaleCommit.revision)
            assertEquals(5, services.remoteDataSource.fetch(first.uid).records.single().brewCount)

            services.authRepository.signOut()
            services.authRepository.register(secondEmail, password)
            val crossAccountRead = runCatching { services.remoteDataSource.fetch(first.uid) }
            assertTrue("A different account must not read the first account", crossAccountRead.isFailure)
        } finally {
            services.authRepository.signOut()
        }
    }
}
