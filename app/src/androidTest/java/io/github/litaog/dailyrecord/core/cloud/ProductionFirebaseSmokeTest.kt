package io.github.litaog.dailyrecord.core.cloud

import io.github.litaog.dailyrecord.core.common.awaitResult
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import io.github.litaog.dailyrecord.core.sync.RemoteSexRecord

/**
 * Explicit, non-default production smoke test.
 *
 * It creates and deletes a random account, verifies owner reads and cross-owner denial,
 * and never writes a private record document. Normal connected test runs skip this class.
 */
@RunWith(AndroidJUnit4::class)
class ProductionFirebaseSmokeTest {
    @Test
    fun productionEmailPasswordAndOwnerRulesSmoke() {
        runBlocking {
            assumeTrue(
                "Pass -e runProductionFirebaseSmoke true to run against production",
                InstrumentationRegistry.getArguments()
                    .getString("runProductionFirebaseSmoke")
                    .toBoolean(),
            )
            val context = ApplicationProvider.getApplicationContext<Context>()
            val services = FirebaseServices.create(context)
            assertTrue("google-services.json must point at production", services.productionConfigured)
            val suffix = UUID.randomUUID().toString().replace("-", "").take(16)
            val email = "codex-smoke-$suffix@example.com"
            val password = "Brew-$suffix-2026"
            val auth = FirebaseAuth.getInstance()

            try {
                val account = services.authRepository.register(email, password)
                assertTrue(services.remoteDataSource.fetch(account.uid).records.isEmpty())
                assertTrue(services.sexRemoteDataSource.fetch(account.uid).records.filterIsInstance<RemoteSexRecord>().isEmpty())
                assertTrue(
                    "A signed-in user must not list another owner's records",
                    runCatching { services.remoteDataSource.fetch("not-${account.uid}") }.isFailure,
                )
                assertTrue(
                    "A signed-in user must not list another owner's sex records",
                    runCatching { services.sexRemoteDataSource.fetch("not-${account.uid}") }.isFailure,
                )
                services.authRepository.signOut()
                services.authRepository.signIn(email, password)
            } finally {
                auth.currentUser?.delete()?.awaitResult()
                services.authRepository.signOut()
            }
        }
    }
}
